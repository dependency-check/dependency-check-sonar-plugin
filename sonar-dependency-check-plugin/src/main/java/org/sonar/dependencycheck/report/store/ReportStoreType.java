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

import java.util.Optional;

public enum ReportStoreType {

    NONE("none"),
    FILESYSTEM("filesystem"),
    S3("s3");

    private final String key;

    ReportStoreType(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Optional<ReportStoreType> fromKey(String key) {
        for (ReportStoreType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
