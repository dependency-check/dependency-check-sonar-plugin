/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2017 Steve Springett
 * steve.springett@owasp.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.dependencycheck.parser;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Configuration;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;

public class XmlReportFile {
    private static final Logger LOGGER = Loggers.get(XmlReportFile.class);

    private final Configuration config;
    private final FileSystem fileSystem;
    private final PathResolver pathResolver;

    private File report;

    public XmlReportFile(Configuration config, FileSystem fileSystem, PathResolver pathResolver) {
        this.config = config;
        this.fileSystem = fileSystem;
        this.pathResolver = pathResolver;
    }

    /**
     * Report file, null if the property is not set.
     *
     * @throws org.sonar.api.utils.MessageException if the property relates to a directory or a non-existing file.
     */
    @CheckForNull
    private File getReportFromProperty(String property) {
        Optional<String> path = config.get(property);
        if (!path.isPresent()) {
            return null;
        }

        this.report = pathResolver.relativeFile(fileSystem.baseDir(), path.get());

        if (report != null && !report.isFile()) {
            LOGGER.warn("Dependency-Check report does not exist. SKIPPING. Please check property {}:{}", property, path.get());
            return null;
        }
        return report;
    }

    public File getFile(String property) {
        if (report == null) {
            report = getReportFromProperty(property);
        }
        return report;
    }

    public InputStream getInputStream(String property) throws IOException {
        File reportFile = getFile(property);
        if (reportFile == null) {
            throw new FileNotFoundException("Dependency-Check report does not exist.");
        }
        return Files.newInputStream(reportFile.toPath());
    }

    public boolean exist(String property) {
        File reportFile = getReportFromProperty(property);
        return reportFile != null;
    }
}
