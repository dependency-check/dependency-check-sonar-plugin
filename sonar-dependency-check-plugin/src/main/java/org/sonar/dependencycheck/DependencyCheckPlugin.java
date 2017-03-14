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
package org.sonar.dependencycheck;

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.dependencycheck.base.DependencyCheckConstants;
import org.sonar.dependencycheck.base.DependencyCheckMetrics;
import org.sonar.dependencycheck.rule.KnownCveRuleDefinition;
import org.sonar.dependencycheck.rule.NeutralLanguage;
import org.sonar.dependencycheck.rule.NeutralProfile;
import org.sonar.dependencycheck.ui.DependencyCheckWidget;

public final class DependencyCheckPlugin implements Plugin {

    public static final String REPOSITORY_KEY = "OWASP";
    public static final String LANGUAGE_KEY = "neutral";
    public static final String RULE_KEY = "UsingComponentWithKnownVulnerability";

    @Override
    public void define(Context context) {
        context.addExtensions(
                DependencyCheckSensor.class,
                DependencyCheckMetrics.class,
                NeutralProfile.class,
                NeutralLanguage.class,
                KnownCveRuleDefinition.class,
                DependencyCheckWidget.class);

        context.addExtension(
                PropertyDefinition.builder(DependencyCheckConstants.REPORT_PATH_PROPERTY)
                        .name("Dependency-Check report path")
                        .description("path to the 'dependency-check-report.xml' file")
                        .defaultValue("${WORKSPACE}/dependency-check-report.xml")
                        .build()
        );
    }
}
