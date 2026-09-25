/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2025 dependency-check
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.System2;
import org.sonar.dependencycheck.base.DependencyCheckConstants;
import org.sonar.dependencycheck.base.DependencyCheckMetrics;

import com.sun.net.httpserver.HttpServer;

import edu.umd.cs.findbugs.annotations.Nullable;

class DependencyCheckSensorTest {

    private PathResolver pathResolver;
    private DependencyCheckSensor sensor;

    private File sampleJsonReport;
    private File sampleHtmlReport;
    private File sampleJsonExceptionReport;

    @BeforeEach
    void init() throws URISyntaxException {
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

    @Test
    void shouldAnalyseWithoutAnHtmlReport() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        // Plugin Configuration
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
        Configuration config = settings.asConfig();
        context.setSettings(settings);
        when(pathResolver.relativeFile(Mockito.any(File.class),
                Mockito.eq(config.get(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY)
                        .orElse(DependencyCheckConstants.JSON_REPORT_PATH_DEFAULT)))).thenReturn(sampleJsonReport);
        when(pathResolver.relativeFile(Mockito.any(File.class),
                Mockito.eq(config.get(DependencyCheckConstants.HTML_REPORT_PATH_PROPERTY)
                        .orElse(DependencyCheckConstants.HTML_REPORT_PATH_DEFAULT)))).thenReturn(null);

        sensor.execute(context);

        assertEquals(40, context.allIssues().size());
        assertNull(context.measure("projectKey", DependencyCheckMetrics.REPORT_LOCATION));
    }

    @Test
    void doesNotSaveReportLocationWithoutHostUrl() {
        final SensorContextTester context = SensorContextTester.create(new File(""));
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_PATH_PROPERTY, "dependency-check-report.html");
        context.setSettings(settings);
        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq("dependency-check-report.json")))
                .thenReturn(sampleJsonReport);
        when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq("dependency-check-report.html")))
                .thenReturn(sampleHtmlReport);

        sensor.execute(context);

        assertNull(context.measure(context.project().key(), DependencyCheckMetrics.REPORT_LOCATION.getKey()));
    }

    @Test
    void savesReportLocationAfterSuccessfulUpload() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "{\"key\":\"projectKey/main/dependency-check-report.html\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            final SensorContextTester context = SensorContextTester.create(new File(""));
            MapSettings settings = new MapSettings();
            settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
            settings.setProperty(DependencyCheckConstants.HTML_REPORT_PATH_PROPERTY, "dependency-check-report.html");
            settings.setProperty("sonar.host.url", "http://127.0.0.1:" + server.getAddress().getPort());
            settings.setProperty("sonar.token", "squ_secret");
            context.setSettings(settings);
            when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq("dependency-check-report.json")))
                    .thenReturn(sampleJsonReport);
            when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq("dependency-check-report.html")))
                    .thenReturn(sampleHtmlReport);

            sensor.execute(context);

            assertEquals("projectKey/main/dependency-check-report.html",
                    context.measure(context.project().key(), DependencyCheckMetrics.REPORT_LOCATION.getKey()).value());
        } finally {
            server.stop(0);
        }
    }

    /**
     * Counts the uploads a sensor run performs against a stub server, for the given store setting.
     */
    private int uploadsWithStoreSetting(@Nullable String storeSetting) throws Exception {
        return uploadsWithStoreSetting(storeSetting, false);
    }

    /**
     * Counts the uploads a sensor run performs against a stub server. With
     * {@code withPropertyDefinitions} the settings carry this plugin's property definitions, which
     * is what a real scanner has: every declared property then answers its default value even when
     * nobody configured it.
     */
    private int uploadsWithStoreSetting(@Nullable String storeSetting, boolean withPropertyDefinitions)
            throws Exception {
        final java.util.concurrent.atomic.AtomicInteger uploads = new java.util.concurrent.atomic.AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            uploads.incrementAndGet();
            byte[] body = "{\"key\":\"projectKey/default/dependency-check-report.html\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            final SensorContextTester context = SensorContextTester.create(new File(""));
            MapSettings settings = withPropertyDefinitions
                    ? new MapSettings(new PropertyDefinitions(System2.INSTANCE, DependencyCheckConfiguration.getPropertyDefinitions()))
                    : new MapSettings();
            settings.setProperty(DependencyCheckConstants.JSON_REPORT_PATH_PROPERTY, "dependency-check-report.json");
            settings.setProperty(DependencyCheckConstants.HTML_REPORT_PATH_PROPERTY, "dependency-check-report.html");
            settings.setProperty("sonar.host.url", "http://127.0.0.1:" + server.getAddress().getPort());
            settings.setProperty("sonar.token", "squ_secret");
            if (storeSetting != null) {
                settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, storeSetting);
            }
            context.setSettings(settings);
            when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq("dependency-check-report.json")))
                    .thenReturn(sampleJsonReport);
            when(pathResolver.relativeFile(Mockito.any(File.class), Mockito.eq("dependency-check-report.html")))
                    .thenReturn(sampleHtmlReport);

            sensor.execute(context);
            return uploads.get();
        } finally {
            server.stop(0);
        }
    }

    /**
     * The store is a server setting and {@link org.sonar.api.config.Configuration} cannot tell a
     * value somebody set from the declared default, so the scanner never uses it as a gate: it
     * uploads and lets the server answer 204 when it keeps no store.
     */
    @Test
    void uploadsEvenWhenTheStoreSettingSaysNone() throws Exception {
        assertEquals(1, uploadsWithStoreSetting("NONE"));
    }

    @Test
    void uploadsWhenTheStoreSettingDidNotReachTheScanner() throws Exception {
        // A global server setting may simply be missing from the scanner configuration - skipping
        // then would break the feature for everyone who configured the store on the server only.
        assertEquals(1, uploadsWithStoreSetting(null));
    }

    /**
     * The regression guard: with the plugin's property definitions registered - which is always the
     * case in a real scanner - an unconfigured store property still answers its declared default
     * 'none'. A gate on that value would silently skip every upload.
     */
    @Test
    void uploadsWhenTheStoreSettingIsOnlyTheDeclaredDefault() throws Exception {
        assertEquals(1, uploadsWithStoreSetting(null, true));
    }

    @Test
    void uploadsWhenAStoreIsConfigured() throws Exception {
        assertEquals(1, uploadsWithStoreSetting("filesystem"));
    }
}
