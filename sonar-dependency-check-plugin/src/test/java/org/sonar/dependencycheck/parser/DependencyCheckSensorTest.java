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
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.dependencycheck.DependencyCheckSensor;
import org.sonar.dependencycheck.base.DependencyCheckConstants;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class DependencyCheckSensorTest {
    private FileSystem fileSystem;
    private PathResolver pathResolver;
    private DependencyCheckSensor sensor;

    private File sampleReport;

    private Configuration config;

    @Before
    public void init() throws URISyntaxException {
        this.fileSystem = mock(FileSystem.class, RETURNS_DEEP_STUBS);
        this.pathResolver = mock(PathResolver.class);
        this.sensor = new DependencyCheckSensor(this.fileSystem, this.pathResolver);

        // Mock config
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.REPORT_PATH_PROPERTY, "dependency-check-report.xml");
        config = settings.asConfig();
        // mock a sample report
        final URL sampleResourceURI = getClass().getClassLoader().getResource("report/dependency-check-report.xml");
        assert sampleResourceURI != null;
        this.sampleReport = Paths.get(sampleResourceURI.toURI()).toFile();
    }

    @Test
    public void toStringTest() {
        assertThat(this.sensor.toString()).isEqualTo("Dependency-Check");
    }

    @Test
    public void testDescribe() {
        final SensorDescriptor descriptor = mock(SensorDescriptor.class);
        sensor.describe(descriptor);
        verify(descriptor).name("Dependency-Check");
    }
    @Test
    public void shouldAnalyse() throws URISyntaxException {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);

        when(context.config()).thenReturn(config);
        when(pathResolver.relativeFile(any(File.class), anyString())).thenReturn(sampleReport);
        sensor.execute(context);
    }

    @Test
    public void shouldSkipIfReportWasNotFound() throws URISyntaxException {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);

        when(context.config()).thenReturn(config);
        when(pathResolver.relativeFile(any(File.class), anyString())).thenReturn(null);
        sensor.execute(context);
        verify(context, never()).newIssue();
    }

    @Test
    public void shouldAddAnIssueForAVulnerability() throws URISyntaxException {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);
        when(context.config()).thenReturn(config);
        when(pathResolver.relativeFile(any(File.class), anyString())).thenReturn(sampleReport);
        sensor.execute(context);

        verify(context, times(3)).newIssue();
    }

    @Test
    public void shouldPersistTotalMetrics() throws URISyntaxException {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);

        when(context.config()).thenReturn(config);
        when(pathResolver.relativeFile(any(File.class), anyString())).thenReturn(sampleReport);
        sensor.execute(context);

        verify(context.newMeasure(), times(9)).forMetric(any(Metric.class));
    }

    @Test
    public void shouldPersistMetricsOnReport() throws URISyntaxException {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);

        when(context.config()).thenReturn(config);
        when(pathResolver.relativeFile(any(File.class), anyString())).thenReturn(sampleReport);
        sensor.execute(context);

        verify(context.newMeasure(), atLeastOnce()).on(any(InputComponent.class));
    }

    @Test
    public void shouldSkipPlugin() {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);

        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.SKIP_PLUGIN, "true");
        when(context.config()).thenReturn(settings.asConfig());
        sensor.execute(context);

        verify(context, times(0)).newMeasure();
        verify(context, times(0)).newIssue();
    }
}
