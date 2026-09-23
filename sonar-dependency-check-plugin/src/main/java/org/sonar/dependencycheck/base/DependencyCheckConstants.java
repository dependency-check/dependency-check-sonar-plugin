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
    public static final String HTML_REPORT_PATH_PROPERTY = "sonar.dependencyCheck.htmlReportPath";
    public static final String SEVERITY_HIGH = "sonar.dependencyCheck.severity.high";
    public static final String SEVERITY_MEDIUM = "sonar.dependencyCheck.severity.medium";
    public static final String SEVERITY_LOW = "sonar.dependencyCheck.severity.low";
    public static final String SUMMARIZE_PROPERTY = "sonar.dependencyCheck.summarize";
    public static final String SKIP_PROPERTY = "sonar.dependencyCheck.skip";
    public static final String SECURITY_HOTSPOT = "sonar.dependencyCheck.securityHotspot";
    public static final String USE_FILEPATH = "sonar.dependencyCheck.useFilePath";

    /*
     * Where the HTML report is published to. These properties are read on the server side only:
     * the server writes the uploaded report into the store and reads it back for the report page.
     * They are therefore global properties - the scanner just posts the report to the server and
     * never touches the store, which is what keeps the store credentials off the CI runners. The
     * scanner only looks at the store type, and only to skip an upload that would be discarded.
     */
    public static final String HTML_REPORT_STORE_PROPERTY = "sonar.dependencyCheck.htmlReport.store";
    public static final String HTML_REPORT_FILESYSTEM_PATH_PROPERTY = "sonar.dependencyCheck.htmlReport.filesystem.path";
    public static final String HTML_REPORT_S3_BUCKET_PROPERTY = "sonar.dependencyCheck.htmlReport.s3.bucket";
    public static final String HTML_REPORT_S3_PREFIX_PROPERTY = "sonar.dependencyCheck.htmlReport.s3.prefix";
    public static final String HTML_REPORT_S3_REGION_PROPERTY = "sonar.dependencyCheck.htmlReport.s3.region";
    public static final String HTML_REPORT_S3_ENDPOINT_PROPERTY = "sonar.dependencyCheck.htmlReport.s3.endpoint";
    public static final String HTML_REPORT_S3_PATH_STYLE_ACCESS_PROPERTY = "sonar.dependencyCheck.htmlReport.s3.pathStyleAccess";
    public static final String HTML_REPORT_S3_ACCESS_KEY_ID_PROPERTY = "sonar.dependencyCheck.htmlReport.s3.accessKeyId";
    public static final String HTML_REPORT_S3_SECRET_ACCESS_KEY_PROPERTY = "sonar.dependencyCheck.htmlReport.s3.secretAccessKey.secured";
    public static final String HTML_REPORT_S3_SESSION_TOKEN_PROPERTY = "sonar.dependencyCheck.htmlReport.s3.sessionToken.secured";

    public static final Float SEVERITY_HIGH_DEFAULT = 7.0f;
    public static final Float SEVERITY_MEDIUM_DEFAULT = 4.0f;
    public static final Float SEVERITY_LOW_DEFAULT = 0.0f;

    public static final Float CVSS_CRITICAL_SCORE = 9.0f;
    public static final Float CVSS_HIGH_SCORE = 7.0f;
    public static final Float CVSS_MEDIUM_SCORE = 4.0f;
    public static final Float CVSS_LOW_SCORE = 0.1f;

    public static final String JSON_REPORT_PATH_DEFAULT = "${WORKSPACE}/dependency-check-report.json";
    public static final String HTML_REPORT_PATH_DEFAULT = "${WORKSPACE}/dependency-check-report.html";
    public static final Boolean SUMMARIZE_PROPERTY_DEFAULT = Boolean.FALSE;
    public static final Boolean SKIP_PROPERTY_DEFAULT = Boolean.FALSE;
    public static final Boolean SECURITY_HOTSPOT_DEFAULT = Boolean.FALSE;
    public static final Boolean USE_FILEPATH_DEFAULT = Boolean.FALSE;

    public static final String HTML_REPORT_S3_PREFIX_DEFAULT = "dependency-check";
    public static final String HTML_REPORT_S3_REGION_DEFAULT = "us-east-1";
    public static final Boolean HTML_REPORT_S3_PATH_STYLE_ACCESS_DEFAULT = Boolean.FALSE;

    public static final String REPOSITORY_KEY = "OWASP";
    public static final String LANGUAGE_KEY = "neutral";
    public static final String RULE_KEY = "UsingComponentWithKnownVulnerability";
    public static final String RULE_KEY_WITH_SECURITY_HOTSPOT = "UsingComponentWithKnownVulnerabilitySecurityHotspot";
    public static final String SUB_CATEGORY_SEVERITIES = "Severities";
    public static final String SUB_CATEGORY_PATHS = "Paths";
    public static final String SUB_CATEGORY_GENERAL = "General";
    public static final String SUB_CATEGORY_HTML_REPORT = "HTML Report";

    private DependencyCheckConstants() {
    }

}
