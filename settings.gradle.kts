import org.gradle.api.internal.FeaturePreviews

pluginManagement {
    repositories {
        maven {
            url = uri("https://repo.gradle.org/gradle/enterprise-libs-release-candidates")
            content {
                val rcAndMilestonesPattern = "\\d{1,2}?\\.\\d{1,2}?(\\.\\d{1,2}?)?-((rc-\\d{1,2}?)|(milestone-\\d{1,2}?))"
                // GE plugin marker artifact
                includeVersionByRegex("com.gradle.enterprise", "com.gradle.enterprise.gradle.plugin", rcAndMilestonesPattern)
                // GE plugin jar
                includeVersionByRegex("com.gradle", "gradle-enterprise-gradle-plugin", rcAndMilestonesPattern)
            }
        }
        maven {
            name = "Gradle public repository"
            url = uri("https://repo.gradle.org/gradle/public")
            content {
                includeModule("org.openmbee.junit", "junit-xml-parser")
            }
        }
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.enterprise").version("3.15") // Sync with `build-logic/build-platform/build.gradle.kts`
    id("io.github.gradle.gradle-enterprise-conventions-plugin").version("0.7.6")
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0")
}

includeBuild("build-logic-commons")
includeBuild("build-logic")

apply(from = "gradle/shared-with-buildSrc/mirrors.settings.gradle.kts")

// If you include a new subproject here, you will need to execute the
// ./gradlew generateSubprojectsInfo
// task to update metadata about the build for CI

unassigned {
    subproject("distributions-dependencies") // platform for dependency versions
    subproject("core-platform")              // platform for Gradle distribution core
}

// Gradle Distributions - for testing and for publishing a full distribution
unassigned {
    subproject("distributions-core")
    subproject("distributions-basics")
    subproject("distributions-publishing")
    subproject("distributions-native")
    subproject("distributions-full")
}

// Gradle implementation projects
unassigned {
    subproject("api-metadata")
    subproject("base-services-groovy")
    subproject("jvm-services")
    subproject("core")
    subproject("dependency-management")
    subproject("resources")
    subproject("resources-http")
    subproject("resources-gcs")
    subproject("resources-s3")
    subproject("resources-sftp")
    subproject("plugins")
    subproject("maven")
    subproject("build-events")
    subproject("signing")
    subproject("reporting")
    subproject("diagnostics")
    subproject("publish")
    subproject("ivy")
    subproject("platform-base")
    subproject("platform-native")
    subproject("platform-jvm")
    subproject("language-native")
    subproject("tooling-native")
    subproject("plugin-use")
    subproject("plugin-development")
    subproject("model-core")
    subproject("model-groovy")
    subproject("testing-native")
    subproject("test-kit")
    subproject("installation-beacon")
    subproject("composite-builds")
    subproject("core-api")
    subproject("version-control")
    subproject("file-collections")
    subproject("build-profile")
    subproject("security")
    subproject("normalization-java")
    subproject("build-operations")
    subproject("instrumentation-agent")
    subproject("instrumentation-declarations")
}

// Core Runtime Platform
platform("core-runtime") {
    subproject("base-annotations")
    subproject("base-services")
    subproject("bootstrap")
    subproject("build-option")
    subproject("cli")
    subproject("file-temp")
    subproject("files")
    subproject("functional")
    subproject("launcher")
    subproject("logging")
    subproject("logging-api")
    subproject("messaging")
    subproject("native")
    subproject("process-services")
    subproject("worker-services")
    subproject("wrapper")
    subproject("wrapper-shared")
}

// Core Configuration Platform
platform("core-configuration") {
    subproject("configuration-cache")
    subproject("kotlin-dsl")
    subproject("kotlin-dsl-provider-plugins")
    subproject("kotlin-dsl-tooling-builders")
    subproject("kotlin-dsl-tooling-models")
    subproject("kotlin-dsl-plugins")
    subproject("kotlin-dsl-integ-tests")
}

// Core Execution Platform
platform("core-execution") {
    subproject("build-cache")
    subproject("build-cache-base")
    subproject("build-cache-http")
    subproject("build-cache-packaging")
    subproject("file-watching")
    subproject("execution")
    subproject("hashing")
    subproject("persistent-cache")
    subproject("snapshots")
    subproject("worker-processes")
    subproject("workers")
}

// IDE Platform
platform("ide") {
    subproject("base-ide-plugins")
    subproject("ide")
    subproject("ide-native")
    subproject("ide-plugins")
    subproject("problems")
    subproject("tooling-api")
    subproject("tooling-api-builders")
}

// Software Platform
platform("software") {
    subproject("antlr")
    subproject("build-init")
    subproject("testing-base")
}

// JVM Platform
platform("jvm") {
    subproject("code-quality")
    subproject("distributions-jvm")
    subproject("ear")
    subproject("jacoco")
    subproject("language-groovy")
    subproject("language-java")
    subproject("language-jvm")
    subproject("toolchains-jvm")
    subproject("java-compiler-plugin")
    subproject("java-platform")
    subproject("plugins-groovy")
    subproject("plugins-java")
    subproject("plugins-jvm-test-suite")
    subproject("plugins-jvm-test-suite-base")
    subproject("scala")
    subproject("testing-jvm")
    subproject("testing-jvm-infrastructure")
    subproject("testing-junit-platform")
    subproject("war")
}

// Gradle Enterprise Platform
platform("enterprise") {
    subproject("enterprise")
    subproject("enterprise-logging")
    subproject("enterprise-operations")
    subproject("enterprise-plugin-performance")
    subproject("enterprise-workers")
}

// Internal utility and verification projects
unassigned {
    subproject("internal-instrumentation-api")
    subproject("internal-instrumentation-processor")
    subproject("docs")
    subproject("docs-asciidoctor-extensions-base")
    subproject("docs-asciidoctor-extensions")
    subproject("samples")
    subproject("architecture-test")
    subproject("internal-testing")
    subproject("internal-integ-testing")
    subproject("internal-performance-testing")
    subproject("internal-architecture-testing")
    subproject("internal-build-reports")
    subproject("integ-test")
    subproject("distributions-integ-tests")
    subproject("soak")
    subproject("smoke-test")
    subproject("performance")
    subproject("precondition-tester")
}

rootProject.name = "gradle"

FeaturePreviews.Feature.values().forEach { feature ->
    if (feature.isActive) {
        enableFeaturePreview(feature.name)
    }
}

fun remoteBuildCacheEnabled(settings: Settings) = settings.buildCache.remote?.isEnabled == true

fun getBuildJavaHome() = System.getProperty("java.home")

gradle.settingsEvaluated {
    if ("true" == System.getProperty("org.gradle.ignoreBuildJavaVersionCheck")) {
        return@settingsEvaluated
    }

    if (!JavaVersion.current().isJava11) {
        throw GradleException("This build requires JDK 11. It's currently ${getBuildJavaHome()}. You can ignore this check by passing '-Dorg.gradle.ignoreBuildJavaVersionCheck=true'.")
    }
}

// region platform include DSL

fun platform(platformName: String, platformConfiguration: PlatformScope.() -> Unit) =
    PlatformScope("platforms/$platformName").platformConfiguration()

fun unassigned(platformConfiguration: PlatformScope.() -> Unit) =
    PlatformScope("subprojects").platformConfiguration()

class PlatformScope(
    private val basePath: String
) {
    fun subproject(projectName: String) {
        include(projectName)
        project(":$projectName").projectDir = file("$basePath/$projectName")
    }
}

// endregion
