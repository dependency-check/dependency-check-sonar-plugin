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
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable.IssueBuilder;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.dependencycheck.DependencyCheckSensor;
import org.sonar.dependencycheck.DependencyCheckSensorConfiguration;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DependencyCheckSensorTest {
    private DependencyCheckSensorConfiguration configuration;
    private FileSystem fileSystem;
    private PathResolver pathResolver;
    private DependencyCheckSensor sensor;

    @Before
    public void init() {
        this.configuration = mock(DependencyCheckSensorConfiguration.class);
        this.fileSystem = mock(FileSystem.class);
        this.pathResolver = mock(PathResolver.class);
        this.sensor = new DependencyCheckSensor(this.configuration, this.fileSystem, this.pathResolver);
    }

    @Test
    public void toStringTest() {
        assertThat(this.sensor.toString()).isEqualTo("OWASP Dependency-Check");
    }

    @Test
    public void shouldAnalyse() throws URISyntaxException {
        //todo: Once the Sensor is capable of working properly, populate this unit test.
    }
}
