/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2025 dependency-check
 * philipp.dallig@gmail.com
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
package org.sonar.dependencycheck.report.store;

import java.nio.file.Paths;
import java.util.Optional;

import org.sonar.api.config.Configuration;
import org.sonar.dependencycheck.base.DependencyCheckConstants;

/**
 * Creates the configured {@link ReportStore}. Stores are created per request and closed
 * afterwards; caching one would mean invalidating it whenever the configuration changes, which is
 * not worth it for an upload per analysis and the occasional page view.
 */
public final class ReportStores {

    private ReportStores() {
    }

    public static Optional<ReportStore> create(Configuration config) throws ReportStoreException {
        // A present but blank value is not a store name. Passing it on would fail fromKey(""),
        // and that exception surfaces as a 500 on every upload and every page view - a blank
        // property is how an administrator switches the store off again, so it means "none",
        // exactly as an absent one does.
        String configured = config.get(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(ReportStoreType.NONE.key());
        ReportStoreType type = ReportStoreType.fromKey(configured)
                .orElseThrow(() -> new ReportStoreException("Unknown report store '" + configured
                        + "'. Please check property " + DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY));
        switch (type) {
            case NONE:
                return Optional.empty();
            case FILESYSTEM:
                return Optional.of(createFilesystemStore(config));
            case S3:
                return Optional.of(createS3Store(config));
            default:
                throw new ReportStoreException("Unsupported report store " + type);
        }
    }

    private static ReportStore createFilesystemStore(Configuration config) throws ReportStoreException {
        // A present but blank value is not a configured directory: Paths.get("") is the SonarQube
        // working directory, which would root the store in the installation itself - and a later
        // cleanup with dryRun=false would then walk it.
        String path = config.get(DependencyCheckConstants.HTML_REPORT_FILESYSTEM_PATH_PROPERTY)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new ReportStoreException("Report store 'filesystem' needs a directory. Please check property "
                        + DependencyCheckConstants.HTML_REPORT_FILESYSTEM_PATH_PROPERTY));
        return new FilesystemReportStore(Paths.get(path));
    }

    private static ReportStore createS3Store(Configuration config) throws ReportStoreException {
        return S3ReportStore.from(config);
    }
}
