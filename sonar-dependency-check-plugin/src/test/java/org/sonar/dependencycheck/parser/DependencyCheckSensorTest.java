/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2019 SonarSecurityCommunity
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
package org.sonar.dependencycheck.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.dependencycheck.DependencyCheckSensor;
import org.sonar.dependencycheck.base.DependencyCheckConstants;
import org.sonar.dependencycheck.base.DependencyCheckMetrics;

public class DependencyCheckSensorTest {

    private PathResolver pathResolver;
    private DependencyCheckSensor sensor;

    private File sampleXmlReport;
    private File sampleHtmlReport;

    private Configuration config;
    private MapSettings settings;

    @BeforeEach
    public void init() throws URISyntaxException {
        FileSystem fileSystem = mock(FileSystem.class, RETURNS_DEEP_STUBS);
        this.pathResolver = mock(PathResolver.class);
        this.sensor = new DependencyCheckSensor(fileSystem, this.pathResolver);

        // Mock config
        settings = new MapSettings();
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
        assertEquals("Dependency-Check", this.sensor.toString());
    }

    @Test
    public void testDescribe() {
        final SensorDescriptor descriptor = mock(SensorDescriptor.class);
        sensor.describe(descriptor);
        verify(descriptor).name("Dependency-Check");
    }
    @Test
    public void shouldAnalyse() throws URISyntaxException {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        context.setSettings(settings);
        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(sampleXmlReport);
        sensor.execute(context);
    }

    @Test
    public void shouldSkipIfReportWasNotFound() throws URISyntaxException {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        context.setSettings(settings);
        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(null);
        sensor.execute(context);
        assertEquals(0, context.allIssues().size());
    }

    @Test
    public void shouldAddAnIssueForAVulnerability() throws URISyntaxException {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        context.setSettings(settings);
        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(sampleXmlReport);
        sensor.execute(context);
        assertEquals(3, context.allIssues().size());
    }

    @Test
    public void shouldPersistTotalMetrics() throws URISyntaxException {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        context.setSettings(settings);
        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(sampleXmlReport);
        sensor.execute(context);
        assertEquals(9, context.measures("projectKey").size());

    }

    @Test
    public void shouldPersistMetricsOnReport() throws URISyntaxException {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        context.setSettings(settings);
        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(sampleXmlReport);
        sensor.execute(context);
        assertNotNull(context.measures("projectKey"));

    }

    @Test
    public void shouldPersistHtmlReport() throws URISyntaxException {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        context.setSettings(settings);
        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.HTML_REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.HTML_REPORT_PATH_DEFAULT)))).thenReturn(sampleHtmlReport);
        sensor.execute(context);
        assertNotNull(context.measure("projectKey", DependencyCheckMetrics.REPORT).value());

    }

    @Test
    public void shouldPersistSummarizeIssues() throws URISyntaxException {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Mock config
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.REPORT_PATH_PROPERTY, "dependency-check-report.xml");
        settings.setProperty(DependencyCheckConstants.SUMMARIZE_PROPERTY, Boolean.TRUE);
        context.setSettings(settings);

        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.REPORT_PATH_DEFAULT)))).thenReturn(sampleXmlReport);
        sensor.execute(context);
        assertEquals(2, context.allIssues().size());
    }
}
