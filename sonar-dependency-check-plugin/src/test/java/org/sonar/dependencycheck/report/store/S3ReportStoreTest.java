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
package org.sonar.dependencycheck.report.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.dependencycheck.base.DependencyCheckConstants;

import com.sun.net.httpserver.HttpServer;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

class S3ReportStoreTest {

    private HttpServer server;
    private S3Client client;
    private final List<String> requests = new ArrayList<>();
    private byte[] storedBody = new byte[0];
    private int responseStatus = 200;
    private List<String> objectKeysToList = Collections.emptyList();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getRawQuery();
            requests.add(method + " " + path + (query != null ? "?" + query : ""));
            if ("PUT".equals(method)) {
                storedBody = readAll(exchange.getRequestBody());
                exchange.getResponseHeaders().add("ETag", "\"deadbeef\"");
                exchange.sendResponseHeaders(200, -1);
            } else if ("GET".equals(method) && query != null && query.contains("list-type=2")) {
                StringBuilder xml = new StringBuilder();
                xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                xml.append("<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">");
                xml.append("<IsTruncated>false</IsTruncated>");
                for (String key : objectKeysToList) {
                    xml.append("<Contents><Key>").append(key).append("</Key></Contents>");
                }
                xml.append("</ListBucketResult>");
                byte[] body = xml.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/xml");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else if ("HEAD".equals(method)) {
                exchange.sendResponseHeaders(responseStatus == 404 ? 404 : 200, -1);
            } else if ("DELETE".equals(method)) {
                exchange.sendResponseHeaders(responseStatus == 404 ? 404 : 204, -1);
            } else if (responseStatus == 404) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                exchange.sendResponseHeaders(200, storedBody.length);
                exchange.getResponseBody().write(storedBody);
            }
            exchange.close();
        });
        server.start();

        client = S3Client.builder()
                .endpointOverride(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        // The test HTTP stub returns a fake ETag, which the SDK would otherwise
                        // reject as a checksum mismatch against the actual uploaded bytes.
                        .checksumValidationEnabled(false)
                        // Over plain HTTP the SDK defaults to SigV4 chunked signed payload
                        // encoding, which the simple test stub does not decode.
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    @AfterEach
    void stopServer() {
        client.close();
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

    @Test
    void storesUnderBucketAndPrefix() throws Exception {
        try (S3ReportStore store = new S3ReportStore(client, "reports", "dependency-check")) {
            String location = store.store("p/main/report.html",
                    new ByteArrayInputStream("<html>hi</html>".getBytes(StandardCharsets.UTF_8)));
            assertEquals("s3://reports/dependency-check/p/main/report.html", location);
        }
        assertTrue(requests.contains("PUT /reports/dependency-check/p/main/report.html"),
                "unexpected requests: " + requests);
        assertEquals("<html>hi</html>", new String(storedBody, StandardCharsets.UTF_8));
    }

    @Test
    void readsBackWhatWasStored() throws Exception {
        try (S3ReportStore store = new S3ReportStore(client, "reports", "dependency-check")) {
            store.store("p/main/report.html",
                    new ByteArrayInputStream("<html>hi</html>".getBytes(StandardCharsets.UTF_8)));
            Optional<InputStream> report = store.read("p/main/report.html");
            assertTrue(report.isPresent());
            assertEquals("<html>hi</html>", new String(readAll(report.get()), StandardCharsets.UTF_8));
        }
    }

    @Test
    void readReturnsEmptyWhenObjectIsMissing() throws Exception {
        responseStatus = 404;
        try (S3ReportStore store = new S3ReportStore(client, "reports", "dependency-check")) {
            assertFalse(store.read("p/main/report.html").isPresent());
        }
    }

    @Test
    void emptyPrefixKeepsTheKeyUnchanged() throws Exception {
        try (S3ReportStore store = new S3ReportStore(client, "reports", "")) {
            String location = store.store("p/main/report.html",
                    new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));
            assertEquals("s3://reports/p/main/report.html", location);
        }
        assertTrue(requests.contains("PUT /reports/p/main/report.html"), "unexpected requests: " + requests);
    }

    @Test
    void rejectsAMalformedRegionWithANamedProperty() {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_S3_BUCKET_PROPERTY, "b");
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_S3_REGION_PROPERTY, "not a region!");
        ReportStoreException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                ReportStoreException.class, () -> S3ReportStore.from(settings.asConfig()));
        assertTrue(thrown.getMessage().contains(DependencyCheckConstants.HTML_REPORT_S3_REGION_PROPERTY),
                "message must name the offending property: " + thrown.getMessage());
    }

    @Test
    void rejectsAMalformedEndpointWithANamedProperty() {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_S3_BUCKET_PROPERTY, "b");
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_S3_ENDPOINT_PROPERTY, "http://exa mple.com");
        ReportStoreException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                ReportStoreException.class, () -> S3ReportStore.from(settings.asConfig()));
        assertTrue(thrown.getMessage().contains(DependencyCheckConstants.HTML_REPORT_S3_ENDPOINT_PROPERTY),
                "message must name the offending property: " + thrown.getMessage());
    }

    @Test
    void listMapsS3KeysBackToStoreKeysWithThePrefixStripped() throws Exception {
        objectKeysToList = java.util.Arrays.asList(
                "dependency-check/p1/main/report.html",
                "dependency-check/p2/main/report.html");
        try (S3ReportStore store = new S3ReportStore(client, "reports", "dependency-check")) {
            List<String> keys = store.list("");
            assertEquals(2, keys.size());
            assertTrue(keys.contains("p1/main/report.html"));
            assertTrue(keys.contains("p2/main/report.html"));
        }
        assertTrue(requests.stream().anyMatch(r -> r.startsWith("GET /reports?") && r.contains("list-type=2")),
                "unexpected requests: " + requests);
    }

    @Test
    void existsAsksWithAHeadAndNeverDownloadsTheObject() throws Exception {
        try (S3ReportStore store = new S3ReportStore(client, "reports", "dependency-check")) {
            assertTrue(store.exists("p/main/report.html"));
        }
        assertTrue(requests.contains("HEAD /reports/dependency-check/p/main/report.html"),
                "unexpected requests: " + requests);
        assertFalse(requests.stream().anyMatch(r -> r.startsWith("GET")), "unexpected requests: " + requests);
    }

    @Test
    void existsIsFalseWhenTheObjectIsMissing() throws Exception {
        responseStatus = 404;
        try (S3ReportStore store = new S3ReportStore(client, "reports", "dependency-check")) {
            assertFalse(store.exists("p/main/report.html"));
        }
    }

    @Test
    void deleteIssuesADeleteToTheRightPathAndReturnsTrue() throws Exception {
        try (S3ReportStore store = new S3ReportStore(client, "reports", "dependency-check")) {
            assertTrue(store.delete("p/main/report.html"));
        }
        assertTrue(requests.contains("DELETE /reports/dependency-check/p/main/report.html"),
                "unexpected requests: " + requests);
    }

    @Test
    void deleteReturnsFalseWhenTheObjectIsMissing() throws Exception {
        responseStatus = 404;
        try (S3ReportStore store = new S3ReportStore(client, "reports", "dependency-check")) {
            assertFalse(store.delete("p/main/report.html"));
        }
        assertFalse(requests.stream().anyMatch(r -> r.startsWith("DELETE")), "unexpected requests: " + requests);
    }
}
