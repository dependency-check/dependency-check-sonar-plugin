/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015 Steve Springett
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.dependencycheck;

import org.sonar.api.SonarPlugin;
import org.sonar.dependencycheck.base.NistMetrics;
import org.sonar.dependencycheck.rule.KnownCveRuleDefinition;
import org.sonar.dependencycheck.rule.NeutralLanguage;
import org.sonar.dependencycheck.rule.NeutralProfile;
import org.sonar.dependencycheck.ui.DependencyCheckWidget;

import java.util.Arrays;
import java.util.List;

public final class DependencyCheckPlugin extends SonarPlugin {

    public static final String REPOSITORY_KEY = "OWASP";
    public static final String LANGUAGE_KEY = "neutral";
    public static final String RULE_KEY = "UsingComponentWithKnownVulnerability";

    @Override
    public List getExtensions() {
        return Arrays.asList(
                DependencyCheckSensor.class,
                DependencyCheckSensorConfiguration.class,
                NistMetrics.class,
                NeutralProfile.class,
                NeutralLanguage.class,
                KnownCveRuleDefinition.class,
                DependencyCheckWidget.class);
    }
}
