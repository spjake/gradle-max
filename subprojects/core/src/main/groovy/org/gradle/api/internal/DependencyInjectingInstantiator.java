/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal;

import org.gradle.api.Action;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.reflect.JavaReflectionUtil;
import org.gradle.internal.reflect.ObjectInstantiationException;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.util.DeprecationLogger;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * An {@link Instantiator} that applies JSR-330 style dependency injection.
 */
public class DependencyInjectingInstantiator implements Instantiator {
    private final ServiceRegistry services;
    private final Action<String> onDeprecationWarning;

    public DependencyInjectingInstantiator(ServiceRegistry services) {
        this.services = services;
        onDeprecationWarning = new Action<String>() {
            public void execute(String message) {
                DeprecationLogger.nagUserWith(message);
            }
        };
    }

    DependencyInjectingInstantiator(ServiceRegistry services, Action<String> onDeprecationWarning) {
        this.services = services;
        this.onDeprecationWarning = onDeprecationWarning;
    }

    public <T> T newInstance(Class<T> type, Object... parameters) {
        try {
            validateType(type);
            Constructor<?> constructor = selectConstructor(type, parameters);
            constructor.setAccessible(true);
            Object[] resolvedParameters = convertParameters(type, constructor, parameters);
            try {
                return type.cast(constructor.newInstance(resolvedParameters));
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        } catch (Throwable e) {
            throw new ObjectInstantiationException(type, e);
        }
    }

    private <T> Object[] convertParameters(Class<T> type, Constructor<?> match, Object[] parameters) {
        Class<?>[] parameterTypes = match.getParameterTypes();
        if (parameterTypes.length < parameters.length) {
            throw new IllegalArgumentException(String.format("Too many parameters provided for constructor for class %s. Expected %s, received %s.", type.getName(), parameterTypes.length, parameters.length));
        }
        Object[] resolvedParameters = new Object[parameterTypes.length];
        int pos = 0;
        for (int i = 0; i < resolvedParameters.length; i++) {
            Class<?> targetType = parameterTypes[i];
            if (targetType.isPrimitive()) {
                targetType = JavaReflectionUtil.getWrapperTypeForPrimitiveType(targetType);
            }
            if (pos < parameters.length && targetType.isInstance(parameters[pos])) {
                resolvedParameters[i] = parameters[pos];
                pos++;
            } else {
                resolvedParameters[i] = services.get(match.getGenericParameterTypes()[i]);
            }
        }
        if (pos != parameters.length) {
            throw new IllegalArgumentException(String.format("Unexpected parameter provided for constructor for class %s.", type.getName()));
        }
        return resolvedParameters;
    }

    private <T> Constructor<?> selectConstructor(Class<T> type, Object... parameters) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();

        // If the class has only 1 constructor, then use it.
        // Note: this does not quite match the JSR-330 spec. We should only do this for default (no-args) constructors.
        if (constructors.length == 1) {
            return constructors[0];
        }

        // For backwards compatibility, first we look for a public constructor that accepts the provided parameters
        // Then we find a candidate constructor as per JSR-330
        // If we find an old style match, then warn if this is not the same as the JSR-330 match. Use the old style match
        // If we find a JSR-330 match and no old style match, use the JSR-330 match
        // Otherwise, fail

        Constructor<?> injectConstructor = null;
        for (Constructor<?> constructor : constructors) {
            if (constructor.getAnnotation(Inject.class) != null) {
                if (injectConstructor != null) {
                    throw new IllegalArgumentException(String.format("Class %s has multiple constructors with @Inject annotation.", type.getName()));
                }
                injectConstructor = constructor;
            }
        }

        Constructor<?> parameterMatchConstructor = null;
        for (Constructor<?> constructor : type.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == parameters.length) {
                boolean match = true;
                for (int i = 0; match && i < parameters.length; i++) {
                    Class<?> targetType = parameterTypes[i];
                    if (targetType.isPrimitive()) {
                        targetType = JavaReflectionUtil.getWrapperTypeForPrimitiveType(targetType);
                    }
                    if (!targetType.isInstance(parameters[i])) {
                        match = false;
                    }
                }
                if (match) {
                    if (parameterMatchConstructor != null) {
                        throw new IllegalArgumentException(String.format("Class %s has multiple constructors that accept parameters %s.", type.getName(), Arrays.toString(parameters)));
                    }
                    parameterMatchConstructor = constructor;
                }
            }
        }

        if (parameterMatchConstructor == null && injectConstructor == null) {
            throw new IllegalArgumentException(String.format("Class %s has no constructor that accepts parameters %s or that is annotated with @Inject.", type.getName(), Arrays.toString(parameters)));
        }
        if (parameterMatchConstructor != null) {
            if (injectConstructor == null) {
                onDeprecationWarning.execute(String.format("Class %s has multiple constructors and none of them are annotated with @Inject.", type.getName()));
            } else if (!parameterMatchConstructor.equals(injectConstructor)) {
                onDeprecationWarning.execute(String.format("Class %s has @Inject annotation on the incorrect constructor.", type.getName()));
            }
            return parameterMatchConstructor;
        }
        return injectConstructor;
    }

    private <T> void validateType(Class<T> type) {
        if (type.isInterface() || type.isAnnotation() || type.isEnum()) {
            throw new IllegalArgumentException(String.format("Type %s is not a class.", type.getName()));
        }
        if (type.getEnclosingClass() != null && !Modifier.isStatic(type.getModifiers())) {
            throw new IllegalArgumentException(String.format("Class %s is a non-static inner class.", type.getName()));
        }
        if (Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalArgumentException(String.format("Class %s is an abstract class.", type.getName()));
        }
    }
}
