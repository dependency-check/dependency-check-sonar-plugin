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
package org.sonar.dependencycheck;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinition.ConfigScope;
import org.sonar.dependencycheck.base.DependencyCheckConstants;
import org.sonar.dependencycheck.report.store.ReportStoreType;

public class DependencyCheckConfiguration {

    private DependencyCheckConfiguration() {
        // do nothing
    }
    public static List<PropertyDefinition> getPropertyDefinitions() {
        return Arrays.asList(
                PropertyDefinition.builder(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY)
                        .onConfigScopes(ConfigScope.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_PATHS)
                        .name("Dependency-Check JSON report path")
                        .description("path to the 'dependency-check-report.json' file")
                        .defaultValue(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_PATH_PROPERTY)
                        .onConfigScopes(ConfigScope.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_PATHS)
                        .name("Dependency-Check HTML report path")
                        .description("path to the 'dependency-check-report.html' file. The report is only published when a report store is configured.")
                        .defaultValue(DependencyCheckConstants.HTML_REPORT_PATH_DEFAULT)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SEVERITY_HIGH)
                        .deprecatedKey("sonar.dependencyCheck.severity.critical")
                        .onConfigScopes(ConfigScope.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_SEVERITIES)
                        .name("High")
                        .description("Minimum score for high issues or -1 to deactivate high issues.")
                        .defaultValue(Float.toString(DependencyCheckConstants.SEVERITY_HIGH_DEFAULT))
                        .type(PropertyType.FLOAT)
                        .index(1)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SEVERITY_MEDIUM)
                        .deprecatedKey("sonar.dependencyCheck.severity.major")
                        .onConfigScopes(ConfigScope.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_SEVERITIES)
                        .name("Medium")
                        .description("Minimum score for medium issues or -1 to deactivate medium issues.")
                        .defaultValue(Float.toString(DependencyCheckConstants.SEVERITY_MEDIUM_DEFAULT))
                        .type(PropertyType.FLOAT)
                        .index(2)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SEVERITY_LOW)
                        .deprecatedKey("sonar.dependencyCheck.severity.minor")
                        .onConfigScopes(ConfigScope.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_SEVERITIES)
                        .name("Low")
                        .description("Minimum score for low issues or -1 to deactivate low issues.")
                        .defaultValue(Float.toString(DependencyCheckConstants.SEVERITY_LOW_DEFAULT))
                        .type(PropertyType.FLOAT)
                        .index(3)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SUMMARIZE_PROPERTY)
                        .onConfigScopes(ConfigScope.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_GENERAL)
                        .name("Summarize")
                        .description("When enabled we summarize all vulnerabilities per dependency.")
                        .defaultValue(Boolean.toString(DependencyCheckConstants.SUMMARIZE_PROPERTY_DEFAULT))
                        .type(PropertyType.BOOLEAN)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SKIP_PROPERTY)
                        .onConfigScopes(ConfigScope.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_GENERAL)
                        .name("Skip")
                        .description("When enabled we skip this plugin.")
                        .defaultValue(Boolean.toString(DependencyCheckConstants.SKIP_PROPERTY_DEFAULT))
                        .type(PropertyType.BOOLEAN)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SECURITY_HOTSPOT)
                        .onConfigScopes(ConfigScope.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_GENERAL)
                        .name("Security-Hotspot")
                        .description("When enabled all SonarQube issues are flagged as Security-Hotspot.")
                        .defaultValue(Boolean.toString(DependencyCheckConstants.SECURITY_HOTSPOT_DEFAULT))
                        .type(PropertyType.BOOLEAN)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.USE_FILEPATH)
                        .onConfigScopes(ConfigScope.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_GENERAL)
                        .name("Use Filepath")
                        .description("When enabled Filepath is used instead of Filename.")
                        .defaultValue(Boolean.toString(DependencyCheckConstants.USE_FILEPATH_DEFAULT))
                        .type(PropertyType.BOOLEAN)
                        .build(),
                /*
                 * The report store is read and written only by the SonarQube server: the scanner
                 * sends the report to the server, and the server writes it into the store and reads
                 * it back for the report page. It is still configured as a global property, not a
                 * project one, because the server serves every project from the same store.
                 */
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_HTML_REPORT)
                        .name("HTML report store")
                        .description("Where published Dependency-Check HTML reports are kept. With 'none' no report is published and the Dependency-Check page stays empty.")
                        .defaultValue(ReportStoreType.NONE.key())
                        .type(PropertyType.SINGLE_SELECT_LIST)
                        .options(ReportStoreType.NONE.key(), ReportStoreType.FILESYSTEM.key(), ReportStoreType.S3.key())
                        .index(1)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_FILESYSTEM_PATH_PROPERTY)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_HTML_REPORT)
                        .name("Directory")
                        .description("Store 'filesystem': directory the reports are written to. Has to be readable and writable by the SonarQube server, a local disk or a mounted volume for example. On a cluster it has to be a volume shared by every server node - on a node local disk a report written by one node is a 404 on the others.")
                        .index(2)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_S3_BUCKET_PROPERTY)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_HTML_REPORT)
                        .name("S3 bucket")
                        .description("Store 's3': name of the bucket the reports are written to.")
                        .index(3)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_S3_PREFIX_PROPERTY)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_HTML_REPORT)
                        .name("S3 key prefix")
                        .description("Store 's3': prefix all report keys are written below.")
                        .defaultValue(DependencyCheckConstants.HTML_REPORT_S3_PREFIX_DEFAULT)
                        .index(4)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_S3_REGION_PROPERTY)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_HTML_REPORT)
                        .name("S3 region")
                        .description("Store 's3': region of the bucket, also used to sign the requests.")
                        .defaultValue(DependencyCheckConstants.HTML_REPORT_S3_REGION_DEFAULT)
                        .index(5)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_S3_ENDPOINT_PROPERTY)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_HTML_REPORT)
                        .name("S3 endpoint")
                        .description("Store 's3': endpoint of an S3 compatible server, for example https://minio.example.com. Defaults to Amazon S3 in the configured region.")
                        .index(6)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_S3_PATH_STYLE_ACCESS_PROPERTY)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_HTML_REPORT)
                        .name("S3 path style access")
                        .description("Store 's3': address the bucket as a path instead of a sub domain. Needed by most S3 compatible servers, MinIO among them.")
                        .defaultValue(Boolean.toString(DependencyCheckConstants.HTML_REPORT_S3_PATH_STYLE_ACCESS_DEFAULT))
                        .type(PropertyType.BOOLEAN)
                        .index(7)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_S3_ACCESS_KEY_ID_PROPERTY)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_HTML_REPORT)
                        .name("S3 access key id")
                        .description("Store 's3': access key id. Leave empty to use the default AWS credential chain of the SonarQube server, for example an IAM role.")
                        .index(8)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_S3_SECRET_ACCESS_KEY_PROPERTY)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_HTML_REPORT)
                        .name("S3 secret access key")
                        .description("Store 's3': secret access key. Leave empty to use the default AWS credential chain of the SonarQube server, for example an IAM role.")
                        .type(PropertyType.PASSWORD)
                        .index(9)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_S3_SESSION_TOKEN_PROPERTY)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_HTML_REPORT)
                        .name("S3 session token")
                        .description("Store 's3': session token for temporary credentials. Leave empty to use the default AWS credential chain of the SonarQube server.")
                        .type(PropertyType.PASSWORD)
                        .index(10)
                        .build()
        );
    }
}
