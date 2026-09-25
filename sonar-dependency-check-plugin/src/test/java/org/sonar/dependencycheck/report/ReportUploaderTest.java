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
package org.sonar.dependencycheck.report;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.dependencycheck.base.DependencyCheckConstants;

import com.sun.net.httpserver.HttpServer;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportUploaderTest {

    private HttpServer server;
    private final List<String> paths = new ArrayList<>();
    private final List<String> authorizations = new ArrayList<>();
    private byte[] received = new byte[0];
    private int status = 200;
    private String body = "{\"key\":\"p/main/dependency-check-report.html\"}";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            authorizations.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            received = readAll(exchange.getRequestBody());
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private String hostUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private HtmlReportFile reportFile(Path dir) throws Exception {
        return reportFile(dir, "<html>report</html>".getBytes(StandardCharsets.UTF_8));
    }

    private HtmlReportFile reportFile(Path dir, byte[] content) throws Exception {
        Path html = dir.resolve("dependency-check-report.html");
        Files.write(html, content);
        FileSystem fileSystem = mock(FileSystem.class, RETURNS_DEEP_STUBS);
        PathResolver resolver = mock(PathResolver.class);
        when(resolver.relativeFile(fileSystem.baseDir(), "dependency-check-report.html")).thenReturn(html.toFile());
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_PATH_PROPERTY, "dependency-check-report.html");
        return HtmlReportFile.getHtmlReport(settings.asConfig(), fileSystem, resolver);
    }

    @Test
    void uploadsTheReportAndReturnsTheKey(@TempDir Path dir) throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.host.url", hostUrl());
        settings.setProperty("sonar.token", "squ_secret");

        Optional<String> key = new ReportUploader(settings.asConfig()).upload("p", null, null, reportFile(dir));

        assertTrue(key.isPresent());
        assertEquals("p/main/dependency-check-report.html", key.get());
        assertEquals("/api/dependencycheck/report_upload", paths.get(0));
        assertTrue(authorizations.get(0).startsWith("Bearer "), "expected a bearer token: " + authorizations.get(0));
        assertTrue(new String(received, StandardCharsets.UTF_8).contains("<html>report</html>"));
    }

    @Test
    void emptyWithoutHostUrl(@TempDir Path dir) throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.token", "squ_secret");
        assertFalse(new ReportUploader(settings.asConfig()).upload("p", null, null, reportFile(dir)).isPresent());
        assertTrue(paths.isEmpty());
    }

    @Test
    void emptyWithoutToken(@TempDir Path dir) throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.host.url", hostUrl());
        assertFalse(new ReportUploader(settings.asConfig()).upload("p", null, null, reportFile(dir)).isPresent());
        assertTrue(paths.isEmpty());
    }

    @Test
    void emptyWhenServerRefuses(@TempDir Path dir) throws Exception {
        status = 403;
        body = "denied";
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.host.url", hostUrl());
        settings.setProperty("sonar.token", "squ_secret");
        assertFalse(new ReportUploader(settings.asConfig()).upload("p", null, null, reportFile(dir)).isPresent());
    }

    @Test
    void emptyWhenStoreIsDisabled(@TempDir Path dir) throws Exception {
        status = 204;
        body = "";
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.host.url", hostUrl());
        settings.setProperty("sonar.token", "squ_secret");
        assertFalse(new ReportUploader(settings.asConfig()).upload("p", null, null, reportFile(dir)).isPresent());
    }

    /**
     * A report of a few megabytes, written to disk and streamed: the assertion is that the server
     * received exactly those bytes, which fails the moment the envelope is assembled from a copy
     * that no longer matches the file.
     */
    @Test
    void streamsALargeReportUnchanged(@TempDir Path dir) throws Exception {
        byte[] large = new byte[6 * 1024 * 1024];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) (i % 251);
        }
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.host.url", hostUrl());
        settings.setProperty("sonar.token", "squ_secret");

        assertTrue(new ReportUploader(settings.asConfig()).upload("p", null, null, reportFile(dir, large)).isPresent());

        byte[] sent = reportPartOf(received);
        assertEquals(large.length, sent.length);
        assertArrayEquals(large, sent);
    }

    /** The bytes between the report part's header and the closing boundary. */
    private static byte[] reportPartOf(byte[] body) {
        String text = new String(body, StandardCharsets.ISO_8859_1);
        int header = text.indexOf("name=\"report\"");
        assertTrue(header >= 0, "no report part in the request body");
        int start = text.indexOf("\r\n\r\n", header) + 4;
        int end = text.lastIndexOf("\r\n--dependencycheck");
        return java.util.Arrays.copyOfRange(body, start, end);
    }

    /**
     * Without a per-part Content-Type a servlet container decodes a text field with its POST
     * default, commonly ISO-8859-1, and a branch named feature/grün arrives mojibaked - stored
     * under a key nothing ever asks for and deleted by the next prune.
     */
    @Test
    void sendsTextFieldsAsUtf8(@TempDir Path dir) throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.host.url", hostUrl());
        settings.setProperty("sonar.token", "squ_secret");

        assertTrue(new ReportUploader(settings.asConfig())
                .upload("p", "feature/grün", null, reportFile(dir)).isPresent());

        String sent = new String(received, StandardCharsets.UTF_8);
        assertTrue(sent.contains("name=\"branch\"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\nfeature/grün\r\n"),
                "branch part not sent as UTF-8 text: " + sent.substring(0, Math.min(400, sent.length())));
        assertTrue(sent.contains("name=\"component\"\r\nContent-Type: text/plain; charset=UTF-8\r\n"),
                "component part has no charset");
    }

    /** A line break in a field value can alter the multipart framing, so it is refused, not stripped. */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"feature\nx", "feature\rx"})
    void skipsTheUploadWhenAFieldContainsALineBreak(String branch, @TempDir Path dir) throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.host.url", hostUrl());
        settings.setProperty("sonar.token", "squ_secret");

        assertFalse(new ReportUploader(settings.asConfig()).upload("p", branch, null, reportFile(dir)).isPresent());
        assertTrue(paths.isEmpty(), "a broken field value must not reach the server");
    }

    /**
     * sonar.login historically carries a username paired with sonar.password, and a bearer header
     * built from it is simply wrong. SonarQube has always accepted both a token and a username as
     * the Basic user, so sonar.login goes there.
     */
    @Test
    void usesBasicAuthenticationForALogin(@TempDir Path dir) throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.host.url", hostUrl());
        settings.setProperty("sonar.login", "admin");
        settings.setProperty("sonar.password", "s3cret");

        assertTrue(new ReportUploader(settings.asConfig()).upload("p", null, null, reportFile(dir)).isPresent());

        assertEquals("Basic " + java.util.Base64.getEncoder().encodeToString(
                "admin:s3cret".getBytes(StandardCharsets.UTF_8)), authorizations.get(0));
    }

    @Test
    void usesAnEmptyPasswordWhenOnlyALoginIsSet(@TempDir Path dir) throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.host.url", hostUrl());
        settings.setProperty("sonar.login", "squ_secret");

        assertTrue(new ReportUploader(settings.asConfig()).upload("p", null, null, reportFile(dir)).isPresent());

        assertEquals("Basic " + java.util.Base64.getEncoder().encodeToString(
                "squ_secret:".getBytes(StandardCharsets.UTF_8)), authorizations.get(0));
    }

    @Test
    void prefersTheTokenOverALogin(@TempDir Path dir) throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.host.url", hostUrl());
        settings.setProperty("sonar.token", "squ_token");
        settings.setProperty("sonar.login", "admin");

        assertTrue(new ReportUploader(settings.asConfig()).upload("p", null, null, reportFile(dir)).isPresent());

        assertEquals("Bearer squ_token", authorizations.get(0));
    }

    /** A big report needs a long upload, so the timeout grows with it instead of being flat. */
    @Test
    void scalesTheRequestTimeoutWithTheBodySize() {
        assertEquals(java.time.Duration.ofMinutes(2), ReportUploader.requestTimeout(0));
        assertEquals(java.time.Duration.ofMinutes(2), ReportUploader.requestTimeout(5L * 1024 * 1024));
        assertEquals(java.time.Duration.ofMinutes(3), ReportUploader.requestTimeout(10L * 1024 * 1024));
        assertEquals(java.time.Duration.ofMinutes(12), ReportUploader.requestTimeout(100L * 1024 * 1024));
    }

    @Test
    void usesAFreshBoundaryForEachUpload(@TempDir Path dir) throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.host.url", hostUrl());
        settings.setProperty("sonar.token", "squ_secret");
        ReportUploader uploader = new ReportUploader(settings.asConfig());

        uploader.upload("p", null, null, reportFile(dir));
        String firstBoundary = firstLineOf(received);
        uploader.upload("p", null, null, reportFile(dir));
        String secondBoundary = firstLineOf(received);

        assertNotEquals(firstBoundary, secondBoundary);
        assertTrue(firstBoundary.startsWith("--dependencycheck"), "unexpected boundary line: " + firstBoundary);
    }

    private static String firstLineOf(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8);
        int end = text.indexOf("\r\n");
        return end < 0 ? text : text.substring(0, end);
    }
}
