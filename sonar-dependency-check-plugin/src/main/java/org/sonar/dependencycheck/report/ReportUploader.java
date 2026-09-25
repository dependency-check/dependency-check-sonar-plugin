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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Sends the HTML report to the SonarQube server, which writes it into the configured store.
 *
 * <p>The plugin API offers a sensor no supported channel to the server, so host url and
 * credentials are read from the scanner configuration and the request is built by hand. Every
 * failure is logged and swallowed: an analysis never fails because of the HTML report.
 */
public class ReportUploader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportUploader.class);

    private static final String HOST_URL_PROPERTY = "sonar.host.url";
    private static final String TOKEN_PROPERTY = "sonar.token";
    private static final String LOGIN_PROPERTY = "sonar.login";
    private static final String PASSWORD_PROPERTY = "sonar.password";
    private static final String PROXY_HOST_PROPERTY = "sonar.scanner.proxyHost";
    private static final String PROXY_PORT_PROPERTY = "sonar.scanner.proxyPort";
    private static final String UPLOAD_PATH = "/api/dependencycheck/report_upload";
    /** Floor of the request timeout, plus {@link #TIMEOUT_PER_BYTES} for every further chunk. */
    private static final Duration TIMEOUT_FLOOR = Duration.ofMinutes(2);
    private static final long TIMEOUT_PER_BYTES = 10L * 1024 * 1024;

    private final Configuration config;

    public ReportUploader(Configuration config) {
        this.config = config;
    }

    public Optional<String> upload(String component, @Nullable String branch, @Nullable String pullRequest,
            HtmlReportFile report) {
        Optional<String> hostUrl = nonBlank(HOST_URL_PROPERTY);
        if (!hostUrl.isPresent()) {
            LOGGER.warn("Dependency-Check HTML report not published: {} is not set", HOST_URL_PROPERTY);
            return Optional.empty();
        }
        Optional<String> authorization = authorization();
        if (!authorization.isPresent()) {
            LOGGER.warn("Dependency-Check HTML report not published: neither {} nor {} is available to the plugin",
                    TOKEN_PROPERTY, LOGIN_PROPERTY);
            return Optional.empty();
        }

        if (rejected("component", component) || rejected("branch", branch) || rejected("pullRequest", pullRequest)) {
            return Optional.empty();
        }

        Path body = null;
        try {
            String boundary = "dependencycheck" + UUID.randomUUID().toString().replace("-", "");
            body = Files.createTempFile("dependency-check-upload", ".multipart");
            writeMultipartBody(body, boundary, component, branch, pullRequest, report);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(hostUrl.get()) + UPLOAD_PATH))
                    .timeout(requestTimeout(Files.size(body)))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Authorization", authorization.get())
                    .POST(HttpRequest.BodyPublishers.ofFile(body))
                    .build();

            HttpClient client = newClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 204) {
                LOGGER.debug("Publishing of the Dependency-Check HTML report is disabled on the server");
                return Optional.empty();
            }
            if (response.statusCode() != 200) {
                LOGGER.warn("Dependency-Check HTML report not published, the server answered {}: {}",
                        response.statusCode(), response.body());
                return Optional.empty();
            }
            return key(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Dependency-Check HTML report not published: interrupted");
            return Optional.empty();
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Dependency-Check HTML report not published: {}", e.getMessage());
            LOGGER.debug(e.getMessage(), e);
            return Optional.empty();
        } finally {
            deleteQuietly(body);
        }
    }

    /**
     * Whether {@code value} cannot go into a multipart text field. A CR or LF would be written
     * straight into the part body and can alter the framing of the request, so such a value is
     * refused rather than silently stripped: a branch name with a newline is a broken input, not
     * something to guess at.
     */
    private static boolean rejected(String name, @Nullable String value) {
        if (value == null || (value.indexOf('\r') < 0 && value.indexOf('\n') < 0)) {
            return false;
        }
        LOGGER.warn("Dependency-Check HTML report not published: the {} contains a line break", name);
        return true;
    }

    private static void deleteQuietly(@Nullable Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.warn("Could not delete the temporary upload body {}: {}", file, e.getMessage());
            LOGGER.debug(e.getMessage(), e);
        }
    }

    /**
     * The Authorization header value, or an empty optional when the scanner configuration holds no
     * credentials at all.
     *
     * <p>{@code sonar.token} is a token and goes into a bearer header. {@code sonar.login} is
     * historically a username paired with {@code sonar.password} - a bearer header built from it
     * is simply wrong - so it goes into HTTP Basic, which SonarQube accepts for a token just as
     * well as for a username. An absent password is the empty one, which is how a token in
     * {@code sonar.login} has always been sent.
     */
    private Optional<String> authorization() {
        Optional<String> token = nonBlank(TOKEN_PROPERTY);
        if (token.isPresent()) {
            return Optional.of("Bearer " + token.get());
        }
        Optional<String> login = nonBlank(LOGIN_PROPERTY);
        if (!login.isPresent()) {
            return Optional.empty();
        }
        String credentials = login.get() + ":" + config.get(PASSWORD_PROPERTY).orElse("");
        return Optional.of("Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * How long the request may take: {@link #TIMEOUT_FLOOR} plus a minute per
     * {@link #TIMEOUT_PER_BYTES}. A flat two minutes is plenty for a small report and far too
     * little for a large one over a slow link, and a timeout that trips means the report is lost.
     */
    static Duration requestTimeout(long bodyBytes) {
        return TIMEOUT_FLOOR.plusMinutes(bodyBytes / TIMEOUT_PER_BYTES);
    }

    /**
     * The client the upload runs on: it follows redirects and honours the scanner's proxy
     * settings.
     *
     * <p>Following redirects does not make a redirected upload succeed. The JDK client re-issues a
     * 301, 302 or 303 as a GET without the body, so the report never reaches the redirect target
     * and the plugin's own action answers that GET with a failure. What this buys is a clean,
     * reported failure instead of a silent one: the default policy is NEVER, under which the
     * scanner took the 3xx itself for a successful upload and every report behind a redirecting
     * front end vanished without a word. A front end that redirects the SonarQube API still has to
     * be fixed - only now it says so.
     *
     * <p>Known gap: {@code sonar.scanner.truststorePath} is not honoured. A private certificate
     * authority therefore has to be trusted by the scanner JVM itself, which is how the scanner is
     * commonly set up anyway.
     */
    private HttpClient newClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL);
        proxy().ifPresent(builder::proxy);
        return builder.build();
    }

    /** The scanner's proxy, when both host and port are set and the port is a number. */
    private Optional<ProxySelector> proxy() {
        Optional<String> host = nonBlank(PROXY_HOST_PROPERTY);
        Optional<String> port = nonBlank(PROXY_PORT_PROPERTY);
        if (!host.isPresent() || !port.isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ProxySelector.of(new InetSocketAddress(host.get(), Integer.parseInt(port.get()))));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ignoring the scanner proxy for the Dependency-Check report upload: {} is not a valid port",
                    port.get());
            LOGGER.debug(e.getMessage(), e);
            return Optional.empty();
        }
    }

    private Optional<String> nonBlank(String property) {
        return config.get(property).map(String::trim).filter(value -> !value.isEmpty());
    }

    private static String trimTrailingSlash(String url) {
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /** Reads the key out of {@code {"key":"..."}} without pulling in a JSON parser on the scanner side. */
    private static Optional<String> key(String body) {
        int start = body.indexOf("\"key\"");
        if (start < 0) {
            return Optional.empty();
        }
        int open = body.indexOf('"', body.indexOf(':', start) + 1);
        int close = open < 0 ? -1 : body.indexOf('"', open + 1);
        if (open < 0 || close < 0) {
            return Optional.empty();
        }
        return Optional.of(body.substring(open + 1, close));
    }

    /**
     * Writes the multipart envelope and the report to {@code body}, streaming the report through a
     * small buffer. Nothing holds the report in memory: a report of a few hundred megabytes used
     * to be buffered three times over here, and the resulting OutOfMemoryError is an Error that no
     * catch clause in the sensor stops - it failed the whole analysis, which this class must never
     * do.
     */
    private void writeMultipartBody(Path body, String boundary, String component, @Nullable String branch,
            @Nullable String pullRequest, HtmlReportFile report) throws IOException {
        try (OutputStream out = Files.newOutputStream(body)) {
            appendField(out, boundary, "component", component);
            if (branch != null && !branch.trim().isEmpty()) {
                appendField(out, boundary, "branch", branch);
            }
            if (pullRequest != null && !pullRequest.trim().isEmpty()) {
                appendField(out, boundary, "pullRequest", pullRequest);
            }
            out.write(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"report\"; filename=\"dependency-check-report.html\"\r\n"
                    + "Content-Type: text/html\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            try (InputStream in = report.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void appendField(OutputStream body, String boundary, String name, String value) throws IOException {
        // Without the per-part Content-Type a servlet container decodes the value with its POST
        // default, commonly ISO-8859-1, which mojibakes every non-ASCII branch name.
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
                + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }
}
