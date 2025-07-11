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
package org.sonar.dependencycheck.base;

public final class DependencyCheckConstants {

    public static final String JSON_REPORT_PATH_PROPERTY = "sonar.dependencyCheck.jsonReportPath";
    public static final String SEVERITY_HIGH = "sonar.dependencyCheck.severity.high";
    public static final String SEVERITY_MEDIUM = "sonar.dependencyCheck.severity.medium";
    public static final String SEVERITY_LOW = "sonar.dependencyCheck.severity.low";
    public static final String SUMMARIZE_PROPERTY = "sonar.dependencyCheck.summarize";
    public static final String SKIP_PROPERTY = "sonar.dependencyCheck.skip";
    public static final String SECURITY_HOTSPOT = "sonar.dependencyCheck.securityHotspot";
    public static final String USE_FILEPATH = "sonar.dependencyCheck.useFilePath";

    public static final Float SEVERITY_HIGH_DEFAULT = 7.0f;
    public static final Float SEVERITY_MEDIUM_DEFAULT = 4.0f;
    public static final Float SEVERITY_LOW_DEFAULT = 0.0f;

    public static final Float CVSS_CRITICAL_SCORE = 9.0f;
    public static final Float CVSS_HIGH_SCORE = 7.0f;
    public static final Float CVSS_MEDIUM_SCORE = 4.0f;
    public static final Float CVSS_LOW_SCORE = 0.1f;

    public static final String JSON_REPORT_PATH_DEFAULT = "${WORKSPACE}/dependency-check-report.json";
    public static final Boolean SUMMARIZE_PROPERTY_DEFAULT = Boolean.FALSE;
    public static final Boolean SKIP_PROPERTY_DEFAULT = Boolean.FALSE;
    public static final Boolean SECURITY_HOTSPOT_DEFAULT = Boolean.FALSE;
    public static final Boolean USE_FILEPATH_DEFAULT = Boolean.FALSE;

    public static final String REPOSITORY_KEY = "OWASP";
    public static final String LANGUAGE_KEY = "neutral";
    public static final String RULE_KEY = "UsingComponentWithKnownVulnerability";
    public static final String RULE_KEY_WITH_SECURITY_HOTSPOT = "UsingComponentWithKnownVulnerabilitySecurityHotspot";
    public static final String SUB_CATEGORY_SEVERITIES = "Severities";
    public static final String SUB_CATEGORY_PATHS = "Paths";
    public static final String SUB_CATEGORY_GENERAL = "General";

    private DependencyCheckConstants() {
    }

}
