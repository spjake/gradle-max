/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.api.tasks.bundling;

import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.archive.TarCopySpecVisitor;
import org.gradle.api.internal.file.archive.compression.ArchiveOutputStreamFactory;
import org.gradle.api.internal.file.archive.compression.Bzip2Archiver;
import org.gradle.api.internal.file.archive.compression.GzipArchiver;
import org.gradle.api.internal.file.archive.compression.SimpleCompressor;
import org.gradle.api.internal.file.copy.ArchiveCopyAction;
import org.gradle.api.internal.file.copy.CopyActionImpl;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Assembles a TAR archive.
 *
 * @author Hans Dockter
 */
public class Tar extends AbstractArchiveTask {
    private CopyActionImpl action;
    private Compression compression = Compression.NONE;

    public Tar() {
        action = new TarCopyActionImpl(getServices().get(FileResolver.class));
        getConventionMapping().map("extension", new Callable<Object>(){
            public Object call() throws Exception {
                return getCompression().getDefaultExtension();
            }
        });
    }

    @Override
    protected void postCopyCleanup() {
        action = null;
    }

    protected CopyActionImpl getCopyAction() {
        return action;
    }

    /**
     * Returns the compression that is used for this archive.
     *
     * @return The compression. Never returns null.
     */
    public Compression getCompression() {
        return compression;
    }

    /**
     * Configures the compressor based on passed in compression.
     *
     * @param compression The compression. Should not be null.
     */
    public void setCompression(Compression compression) {
        this.compression = compression;
    }

    private class TarCopyActionImpl extends CopyActionImpl implements ArchiveCopyAction  {
        public TarCopyActionImpl(FileResolver fileResolver) {
            super(fileResolver, new TarCopySpecVisitor());
        }

        public File getArchivePath() {
            return Tar.this.getArchivePath();
        }

        public ArchiveOutputStreamFactory getCompressor() {
            switch(compression) {
                case BZIP2: return Bzip2Archiver.getCompressor();
                case GZIP:  return GzipArchiver.getCompressor();
                default:    return new SimpleCompressor();
            }
        }
    }
}