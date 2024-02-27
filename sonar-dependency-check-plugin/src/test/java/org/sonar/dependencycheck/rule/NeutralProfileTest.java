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
package org.sonar.dependencycheck.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import org.sonar.dependencycheck.base.DependencyCheckConstants;

class NeutralProfileTest {

    @Test
    void test() {
        NeutralProfile profileDef = new NeutralProfile();
        BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
        profileDef.define(context);
        BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context
                .profile(DependencyCheckConstants.LANGUAGE_KEY, "Neutral");
        assertEquals("neutral", profile.language());
        assertEquals("Neutral", profile.name());
        assertEquals(1, profile.rules().size());

        BuiltInActiveRule rule = profile.rules().get(0);
        assertEquals(DependencyCheckConstants.REPOSITORY_KEY, rule.repoKey());
        assertEquals(DependencyCheckConstants.RULE_KEY, rule.ruleKey());
    }

}
