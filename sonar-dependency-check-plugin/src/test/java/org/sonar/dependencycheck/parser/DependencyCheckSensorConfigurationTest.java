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
package org.sonar.dependencycheck.parser;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.dependencycheck.DependencyCheckSensorConfiguration;
import org.sonar.dependencycheck.base.DependencyCheckConstants;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DependencyCheckSensorConfigurationTest {
    private Settings settings;
    private DependencyCheckSensorConfiguration sensorConfiguration;

    @Before
    public void init() {
        this.settings = mock(Settings.class);
        this.sensorConfiguration = new DependencyCheckSensorConfiguration( this.settings);
    }

    @Test
    public void testGetReportPath() {
        when(this.settings.getString(DependencyCheckConstants.REPORT_PATH_PROPERTY)).thenReturn("location");
        assertThat(this.sensorConfiguration.getReportPath()).isEqualTo("location");
    }

}
