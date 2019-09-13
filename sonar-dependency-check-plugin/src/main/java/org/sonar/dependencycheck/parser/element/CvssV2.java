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

public class CvssV2 implements Cvss {
    private final Float score;
    private final String severity;
    
    public CvssV2(@NonNull Float score, @NonNull String severity) {
        this.score = score;
        this.severity = severity;
    }

    /**
     * @return the score
     */
    @Override
    public Float getScore() {
        return score;
    }

    /**
     * @return the severity
     */
    @Override
    public String getSeverity() {
        return severity;
    }
}
