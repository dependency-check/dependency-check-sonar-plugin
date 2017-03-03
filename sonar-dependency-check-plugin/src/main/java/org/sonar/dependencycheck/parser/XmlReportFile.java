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
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.dependencycheck.base.DependencyCheckConstants;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class XmlReportFile {
    private static final Logger LOGGER = Loggers.get(XmlReportFile.class);

    private final Settings settings;
    private final FileSystem fileSystem;
    private final PathResolver pathResolver;

    private File report;

    public XmlReportFile(Settings settings, FileSystem fileSystem, PathResolver pathResolver) {
        this.settings = settings;
        this.fileSystem = fileSystem;
        this.pathResolver = pathResolver;
    }

    /**
     * Report file, null if the property is not set.
     *
     * @throws org.sonar.api.utils.MessageException if the property relates to a directory or a non-existing file.
     */
    @CheckForNull
    private File getReportFromProperty() {
        String path = settings.getString(DependencyCheckConstants.REPORT_PATH_PROPERTY);
        if (path == null) {
            return null;
        }

        this.report = pathResolver.relativeFile(fileSystem.baseDir(), path);

        if (report != null && !report.isFile()) {
            LOGGER.warn("Dependency-Check report does not exist. SKIPPING. Please check property " +
                    DependencyCheckConstants.REPORT_PATH_PROPERTY + ": " + path);
            return null;
        }
        return report;
    }

    public File getFile() {
        if (report == null) {
            report = getReportFromProperty();
        }
        return report;
    }

    public InputStream getInputStream() throws FileNotFoundException {
        File reportFile = getFile();
        if (reportFile == null) {
            throw new FileNotFoundException("Dependency-Check report does not exist.");
        }
        return new FileInputStream(reportFile);
    }

    public boolean exist() {
        File reportFile = getReportFromProperty();
        return reportFile != null;
    }
}
