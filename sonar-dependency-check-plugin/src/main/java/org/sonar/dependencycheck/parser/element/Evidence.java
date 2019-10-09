/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2019 dependency-check
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
package org.sonar.dependencycheck.parser.element;

import edu.umd.cs.findbugs.annotations.NonNull;

public class Evidence {

    private final String source;
    private final String name;
    private final String value;
    private final String type;
    private final Confidence confidence;

    public Evidence(@NonNull String source, @NonNull String name, @NonNull String value, String type, Confidence confidence) {
        this.source = source;
        this.name = name;
        this.value = value;
        this.type = type;
        this.confidence = confidence;
    }

    public String getSource() {
        return source;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public Confidence getConfidence() {
        return confidence;
    }

}
