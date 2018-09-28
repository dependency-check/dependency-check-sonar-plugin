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
import org.sonar.dependencycheck.base.DependencyCheckMetrics;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DependencyCheckSensorTest {

    private PathResolver pathResolver;
    private DependencyCheckSensor sensor;

    private File sampleXmlReport;
    private File sampleHtmlReport;

    private Configuration config;

    @Before
    public void init() throws URISyntaxException {
        FileSystem fileSystem = mock(FileSystem.class, RETURNS_DEEP_STUBS);
        this.pathResolver = mock(PathResolver.class);
        this.sensor = new DependencyCheckSensor(fileSystem, this.pathResolver);

        // Mock config
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.REPORT_PATH_PROPERTY, "dependency-check-report.xml");
        config = settings.asConfig();
        // mock a sample report
        final URL sampleXmlResourceURI = getClass().getClassLoader().getResource("report/dependency-check-report.xml");
        assert sampleXmlResourceURI != null;
        this.sampleXmlReport = Paths.get(sampleXmlResourceURI.toURI()).toFile();
        final URL sampleHtmlResourceURI = getClass().getClassLoader().getResource("report/dependency-check-report.html");
        assert sampleHtmlResourceURI != null;
        this.sampleHtmlReport = Paths.get(sampleHtmlResourceURI.toURI()).toFile();
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
        when(pathResolver.relativeFile(any(File.class), eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(sampleXmlReport);
        sensor.execute(context);
    }

    @Test
    public void shouldSkipIfReportWasNotFound() throws URISyntaxException {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);

        when(context.config()).thenReturn(config);
        when(pathResolver.relativeFile(any(File.class), eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(null);
        sensor.execute(context);
        verify(context, never()).newIssue();
    }

    @Test
    public void shouldAddAnIssueForAVulnerability() throws URISyntaxException {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);
        when(context.config()).thenReturn(config);
        when(pathResolver.relativeFile(any(File.class), eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(sampleXmlReport);
        sensor.execute(context);

        verify(context, times(3)).newIssue();
    }

    @Test
    public void shouldPersistTotalMetrics() throws URISyntaxException {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);

        when(context.config()).thenReturn(config);
        when(pathResolver.relativeFile(any(File.class), eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(sampleXmlReport);
        sensor.execute(context);

        verify(context.newMeasure(), times(9)).forMetric(any(Metric.class));
    }

    @Test
    public void shouldPersistMetricsOnReport() throws URISyntaxException {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);

        when(context.config()).thenReturn(config);
        when(pathResolver.relativeFile(any(File.class), eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(sampleXmlReport);
        sensor.execute(context);

        verify(context.newMeasure(), atLeastOnce()).on(any(InputComponent.class));
    }

    @Test
    public void shouldPersistHtmlReport() throws URISyntaxException {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);

        when(context.config()).thenReturn(config);
        when(pathResolver.relativeFile(any(File.class), eq(config.get(DependencyCheckConstants.HTML_REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.HTML_REPORT_PATH_DEFAULT)))).thenReturn(sampleHtmlReport);
        sensor.execute(context);

        verify(context.<String>newMeasure().forMetric(DependencyCheckMetrics.REPORT), times(1)).on(any(InputComponent.class));
    }

    @Test
    public void shouldPersistSummarizeIssues() throws URISyntaxException {
        final SensorContext context = mock(SensorContext.class, RETURNS_DEEP_STUBS);
        // Mock config
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.REPORT_PATH_PROPERTY, "dependency-check-report.xml");
        settings.setProperty(DependencyCheckConstants.SUMMARIZE_PROPERTY, Boolean.TRUE);
        config = settings.asConfig();

        when(context.config()).thenReturn(config);
        when(pathResolver.relativeFile(any(File.class), eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(sampleXmlReport);
        sensor.execute(context);

        verify(context, times(2)).newIssue();
    }
}
