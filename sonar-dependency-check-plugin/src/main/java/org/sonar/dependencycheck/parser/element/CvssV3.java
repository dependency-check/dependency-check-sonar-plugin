/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2019 SonarSecurityCommunity
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

public class CvssV3 implements Cvss{
    private final Float baseScore;
    private final String baseSeverity;

    /**
     * @param baseScore
     * @param baseSeverity
     */
    public CvssV3(Float baseScore, String baseSeverity) {
        this.baseScore = baseScore;
        this.baseSeverity = baseSeverity;
    }
    /**
     * @return the baseScore
     */
    public Float getBaseScore() {
        return baseScore;
    }
    /**
     * @return the baseSeverity
     */
    public String getBaseSeverity() {
        return baseSeverity;
    }
    @Override
    public Float getScore() {
        return getBaseScore();
    }
    @Override
    public String getSeverity() {
        return getBaseSeverity();
    }
}
