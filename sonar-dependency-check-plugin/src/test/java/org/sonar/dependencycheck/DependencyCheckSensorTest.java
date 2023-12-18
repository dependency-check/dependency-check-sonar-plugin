/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2023 dependency-check
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.dependencycheck.base.DependencyCheckConstants;
import org.sonar.dependencycheck.base.DependencyCheckMetrics;

class DependencyCheckSensorTest {

    private PathResolver pathResolver;
    private DependencyCheckSensor sensor;

    private File sampleJsonReport;
    private File sampleHtmlReport;
    private File sampleJsonExceptionReport;

    @BeforeEach
    public void init() throws URISyntaxException {
        FileSystem fileSystem = mock(FileSystem.class, RETURNS_DEEP_STUBS);
        this.pathResolver = mock(PathResolver.class);
        this.sensor = new DependencyCheckSensor(fileSystem, this.pathResolver, null);

        // load some sample reports
        final URL sampleJsonResourceURI = getClass().getClassLoader().getResource("reportMultiModuleMavenExample/dependency-check-report.json");
        assertNotNull(sampleJsonResourceURI);
        this.sampleJsonReport = Paths.get(sampleJsonResourceURI.toURI()).toFile();

        final URL sampleHtmlResourceURI = getClass().getClassLoader().getResource("reportMultiModuleMavenExample/dependency-check-report.html");
        assertNotNull(sampleHtmlResourceURI);
        this.sampleHtmlReport = Paths.get(sampleHtmlResourceURI.toURI()).toFile();

        final URL sampleExceptionResourceURI = getClass().getClassLoader().getResource("reportWithExceptions/dependency-check-report.json");
        assertNotNull(sampleExceptionResourceURI);
        this.sampleJsonExceptionReport = Paths.get(sampleExceptionResourceURI.toURI()).toFile();
    }

    @Test
    void toStringTest() {
        assertEquals("Dependency-Check", this.sensor.toString());
    }

    @Test
    void testDescribe() {
        final SensorDescriptor descriptor = mock(SensorDescriptor.class);
        sensor.describe(descriptor);
        verify(descriptor).name("Dependency-Check");
    }
    @Test
    void shouldAnalyse() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Plugin Configuration
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
        Configuration config = settings.asConfig();
        context.setSettings(settings);

        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT)))).thenReturn(sampleJsonReport);
        sensor.execute(context);
        assertEquals(40, context.allIssues().size());
    }

    @Test
    void shouldSkipIfReportWasNotFound() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Plugin Configuration
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
        Configuration config = settings.asConfig();
        context.setSettings(settings);

        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT)))).thenReturn(null);
        sensor.execute(context);
        assertEquals(0, context.allIssues().size());
    }

    @Test
    void shouldAddAnIssueForAVulnerability() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Plugin Configuration
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
        Configuration config = settings.asConfig();
        context.setSettings(settings);

        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT)))).thenReturn(sampleJsonReport);
        sensor.execute(context);
        assertEquals(40, context.allIssues().size());
        for (Issue issue : context.allIssues()) {
            assertEquals(DependencyCheckConstants.RULE_KEY, issue.ruleKey().rule());
        }
    }

    @Test
    void shouldPersistTotalMetrics() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Plugin Configuration
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
        Configuration config = settings.asConfig();
        context.setSettings(settings);

        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT)))).thenReturn(sampleJsonReport);
        sensor.execute(context);
        assertEquals(8, context.measures("projectKey").size());

    }

    @Test
    void shouldPersistMetricsOnReport() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Plugin Configuration
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
        Configuration config = settings.asConfig();
        context.setSettings(settings);

        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT)))).thenReturn(sampleJsonReport);
        sensor.execute(context);
        assertNotNull(context.measures("projectKey"));
    }

    @Test
    void shouldPersistHtmlReport() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Plugin Configuration
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
        Configuration config = settings.asConfig();
        context.setSettings(settings);

        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.HTML_REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.HTML_REPORT_PATH_DEFAULT)))).thenReturn(sampleHtmlReport);
        sensor.execute(context);
        assertNotNull(context.measure("projectKey", DependencyCheckMetrics.REPORT).value());

    }

    @Test
    void shouldPersistSummarizeIssues() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Plugin Configuration
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
        settings.setProperty(DependencyCheckConstants.SUMMARIZE_PROPERTY, Boolean.TRUE);
        Configuration config = settings.asConfig();
        context.setSettings(settings);

        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT)))).thenReturn(sampleJsonReport);
        sensor.execute(context);
        assertEquals(8, context.allIssues().size());
    }

    @Test
    void shouldSkipPlugin() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Plugin Configuration
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.xml");
        settings.setProperty(DependencyCheckConstants.SKIP_PROPERTY, Boolean.TRUE);
        Configuration config = settings.asConfig();
        context.setSettings(settings);

        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT)))).thenReturn(sampleJsonReport);
        sensor.execute(context);
        assertEquals(0, context.allIssues().size());
    }

    @Test
    void shouldAddWarningsPlugin() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Plugin Configuration
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
        Configuration config = settings.asConfig();
        context.setSettings(settings);

        // Sensor with analysisWarnings
        FileSystem fileSystem = mock(FileSystem.class, RETURNS_DEEP_STUBS);
        List<String> analysisWarnings = new ArrayList<>();
        sensor = new DependencyCheckSensor(fileSystem, this.pathResolver, analysisWarnings::add);

        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq(config.get(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY).orElse(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT)))).thenReturn(sampleJsonExceptionReport);
        sensor.execute(context);
        assertTrue(StringUtils.contains(analysisWarnings.get(0), "Dependency-Check - "));
        assertTrue(StringUtils.contains(analysisWarnings.get(1),"Dependency-Check - "));
        assertFalse(StringUtils.equals(analysisWarnings.get(0), analysisWarnings.get(1)));
        assertEquals(2, analysisWarnings.size());
    }

    @Test
    void shouldAddSecurityHotspots() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Plugin Configuration
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.xml");
        settings.setProperty(DependencyCheckConstants.SECURITY_HOTSPOT, Boolean.TRUE);
        Configuration config = settings.asConfig();
        context.setSettings(settings);

        when(pathResolver
                .relativeFile(Mockito.any(File.class),
                        Mockito.eq(config.get(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY)
                                .orElse(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT))))
                                        .thenReturn(sampleJsonReport);
        sensor.execute(context);
        assertEquals(40, context.allIssues().size());
        for (Issue issue : context.allIssues()) {
            assertEquals(DependencyCheckConstants.RULE_KEY_WITH_SECURITY_HOTSPOT, issue.ruleKey().rule());
        }

    }
}
