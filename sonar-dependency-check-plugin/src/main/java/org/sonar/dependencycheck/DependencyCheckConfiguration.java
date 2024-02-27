/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2024 dependency-check
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
import org.sonar.api.resources.Qualifiers;
import org.sonar.dependencycheck.base.DependencyCheckConstants;

public class DependencyCheckConfiguration {

    private DependencyCheckConfiguration() {
        // do nothing
    }
    public static List<PropertyDefinition> getPropertyDefinitions() {
        return Arrays.asList(
                PropertyDefinition.builder(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY)
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_PATHS)
                        .name("Dependency-Check JSON report path")
                        .description("path to the 'dependency-check-report.json' file")
                        .defaultValue(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.HTML_REPORT_PATH_PROPERTY)
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_PATHS)
                        .name("Dependency-Check HTML report path")
                        .description("path to the 'dependency-check-report.html' file")
                        .defaultValue(DependencyCheckConstants.HTML_REPORT_PATH_DEFAULT)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SEVERITY_HIGH)
                        .deprecatedKey("sonar.dependencyCheck.severity.critical")
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_SEVERITIES)
                        .name("High")
                        .description("Minimum score for high issues or -1 to deactivate high issues.")
                        .defaultValue(Float.toString(DependencyCheckConstants.SEVERITY_HIGH_DEFAULT))
                        .type(PropertyType.FLOAT)
                        .index(1)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SEVERITY_MEDIUM)
                        .deprecatedKey("sonar.dependencyCheck.severity.major")
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_SEVERITIES)
                        .name("Medium")
                        .description("Minimum score for medium issues or -1 to deactivate medium issues.")
                        .defaultValue(Float.toString(DependencyCheckConstants.SEVERITY_MEDIUM_DEFAULT))
                        .type(PropertyType.FLOAT)
                        .index(2)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SEVERITY_LOW)
                        .deprecatedKey("sonar.dependencyCheck.severity.minor")
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_SEVERITIES)
                        .name("Low")
                        .description("Minimum score for low issues or -1 to deactivate low issues.")
                        .defaultValue(Float.toString(DependencyCheckConstants.SEVERITY_LOW_DEFAULT))
                        .type(PropertyType.FLOAT)
                        .index(3)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SUMMARIZE_PROPERTY)
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_GENERAL)
                        .name("Summarize")
                        .description("When enabled we summarize all vulnerabilities per dependency.")
                        .defaultValue(Boolean.toString(DependencyCheckConstants.SUMMARIZE_PROPERTY_DEFAULT))
                        .type(PropertyType.BOOLEAN)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SKIP_PROPERTY)
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_GENERAL)
                        .name("Skip")
                        .description("When enabled we skip this plugin.")
                        .defaultValue(Boolean.toString(DependencyCheckConstants.SKIP_PROPERTY_DEFAULT))
                        .type(PropertyType.BOOLEAN)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.SECURITY_HOTSPOT)
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_GENERAL)
                        .name("Security-Hotspot")
                        .description("When enabled all SonarQube issues are flagged as Security-Hotspot.")
                        .defaultValue(Boolean.toString(DependencyCheckConstants.SECURITY_HOTSPOT_DEFAULT))
                        .type(PropertyType.BOOLEAN)
                        .build(),
                PropertyDefinition.builder(DependencyCheckConstants.USE_FILEPATH)
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(DependencyCheckConstants.SUB_CATEGORY_GENERAL)
                        .name("Use Filepath")
                        .description("When enabled Filepath is used instead of Filename.")
                        .defaultValue(Boolean.toString(DependencyCheckConstants.USE_FILEPATH_DEFAULT))
                        .type(PropertyType.BOOLEAN)
                        .build()
        );
    }
}
