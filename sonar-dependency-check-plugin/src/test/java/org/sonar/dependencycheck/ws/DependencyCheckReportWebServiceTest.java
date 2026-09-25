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
package org.sonar.dependencycheck.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.LocalConnector;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Request.Part;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.dependencycheck.base.DependencyCheckConstants;

class DependencyCheckReportWebServiceTest {

    private static final String MEASURE_JSON =
            "{\"component\":{\"qualifier\":\"TRK\",\"measures\":[{\"metric\":\"report_location\",\"value\":\"p/default/dependency-check-report.html\"}]}}";

    private MapSettings settingsFor(Path root) {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, "filesystem");
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_FILESYSTEM_PATH_PROPERTY, root.toString());
        return settings;
    }

    private LocalConnector connectorReturning(int status, String body) {
        LocalConnector connector = mock(LocalConnector.class);
        LocalConnector.LocalResponse response = mock(LocalConnector.LocalResponse.class);
        when(response.getStatus()).thenReturn(status);
        when(response.getBytes()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        when(connector.call(any())).thenReturn(response);
        return connector;
    }

    private LocalConnector.LocalResponse responseOf(int status, String body) {
        LocalConnector.LocalResponse response = mock(LocalConnector.LocalResponse.class);
        when(response.getStatus()).thenReturn(status);
        when(response.getBytes()).thenReturn(body.getBytes(StandardCharsets.UTF_8));
        return response;
    }

    /**
     * A connector that answers per request path, with {@code fallback} for anything not listed -
     * the reconciliation calls hit several endpoints in one request and each needs its own canned
     * answer.
     */
    private LocalConnector connectorFor(java.util.Map<String, LocalConnector.LocalResponse> byPath,
            LocalConnector.LocalResponse fallback) {
        LocalConnector connector = mock(LocalConnector.class);
        when(connector.call(any())).thenAnswer(invocation -> {
            LocalConnector.LocalRequest req = invocation.getArgument(0);
            LocalConnector.LocalResponse response = byPath.get(req.getPath());
            return response != null ? response : fallback;
        });
        return connector;
    }

    private Request requestFor(LocalConnector connector) {
        Request request = mock(Request.class);
        when(request.localConnector()).thenReturn(connector);
        when(request.mandatoryParam("component")).thenReturn("p");
        when(request.param("branch")).thenReturn(null);
        when(request.param("pullRequest")).thenReturn(null);
        return request;
    }

    private Request uploadRequestFor(LocalConnector connector, String content) {
        Request request = mock(Request.class);
        when(request.localConnector()).thenReturn(connector);
        when(request.mandatoryParam("component")).thenReturn("p");
        when(request.param("branch")).thenReturn(null);
        when(request.param("pullRequest")).thenReturn(null);
        Part part = mock(Part.class);
        when(part.getInputStream()).thenReturn(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        when(request.mandatoryParamAsPart("report")).thenReturn(part);
        return request;
    }

    private ByteArrayOutputStream body;

    private Response stubbedResponse() {
        Response response = mock(Response.class);
        Response.Stream stream = mock(Response.Stream.class);
        body = new ByteArrayOutputStream();
        when(response.stream()).thenReturn(stream);
        when(stream.setMediaType(any())).thenReturn(stream);
        when(stream.setStatus(anyInt())).thenReturn(stream);
        when(stream.output()).thenReturn(body);
        return response;
    }

    @Test
    void definesControllerAndShowAction(@TempDir Path root) {
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        WebService.Context context = new WebService.Context();
        ws.define(context);
        WebService.Controller controller = context.controller("api/dependencycheck");
        assertNotNull(controller);
        assertNotNull(controller.action("show"));
    }

    @Test
    void streamsTheStoredReport(@TempDir Path root) throws Exception {
        Path report = root.resolve("p/default/dependency-check-report.html");
        Files.createDirectories(report.getParent());
        Files.write(report, "<html>hi</html>".getBytes(StandardCharsets.UTF_8));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = requestFor(connectorReturning(200, MEASURE_JSON));

        Response response = mock(Response.class);
        Response.Stream stream = mock(Response.Stream.class);
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        when(response.stream()).thenReturn(stream);
        when(stream.setMediaType(any())).thenReturn(stream);
        when(stream.setStatus(anyInt())).thenReturn(stream);
        when(stream.output()).thenReturn(body);

        ws.handleShow(request, response);

        assertEquals("<html>hi</html>", new String(body.toByteArray(), StandardCharsets.UTF_8));
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader(eq("Content-Security-Policy"), contains("sandbox"));
        verify(response).setHeader("X-Dependency-Check-Report", "1");
        verify(stream).setMediaType("text/html");
    }

    /**
     * A stock Dependency-Check report inlines its Bootstrap and DataTables icons and glyphs as
     * data: URIs. CSP has no implicit allowance for those, so without these two directives every
     * icon of the report is blocked on the one path this feature exists to serve.
     */
    @Test
    void reportPolicyAllowsTheInlinedImagesAndFonts(@TempDir Path root) throws Exception {
        Path report = root.resolve("p/default/dependency-check-report.html");
        Files.createDirectories(report.getParent());
        Files.write(report, "<html>hi</html>".getBytes(StandardCharsets.UTF_8));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Response response = stubbedResponse();

        ws.handleShow(requestFor(connectorReturning(200, MEASURE_JSON)), response);

        verify(response).setHeader(eq("Content-Security-Policy"), contains("img-src 'self' data:"));
        verify(response).setHeader(eq("Content-Security-Policy"), contains("font-src 'self' data:"));
    }

    /**
     * The engine only enforces the verb on POST actions, so a HEAD request would otherwise run
     * handleShow in full and stream the whole report into a response the container discards - on
     * every single page view.
     */
    @Test
    void headProbeAnswersWithoutReadingTheReport(@TempDir Path root) throws Exception {
        Path report = root.resolve("p/default/dependency-check-report.html");
        Files.createDirectories(report.getParent());
        Files.write(report, "<html>hi</html>".getBytes(StandardCharsets.UTF_8));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = requestFor(connectorReturning(200, MEASURE_JSON));
        when(request.method()).thenReturn("HEAD");
        Response response = stubbedResponse();

        ws.handleShow(request, response);

        assertEquals(0, body.size(), "a HEAD response must carry no body");
        verify(response).setHeader("X-Dependency-Check-Report", "1");
        verify(response.stream()).setStatus(200);
        verify(response.stream()).setMediaType("text/html");
    }

    @Test
    void headProbeAnswersNotFoundWhenTheStoreHasNoReport(@TempDir Path root) throws Exception {
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = requestFor(connectorReturning(200, MEASURE_JSON));
        when(request.method()).thenReturn("HEAD");
        Response response = stubbedResponse();

        ws.handleShow(request, response);

        verify(response.stream()).setStatus(404);
    }

    @Test
    void answersNotFoundWhenNoMeasureExists(@TempDir Path root) throws Exception {
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = requestFor(connectorReturning(200, "{\"component\":{\"measures\":[]}}"));

        Response response = mock(Response.class);
        Response.Stream stream = mock(Response.Stream.class);
        when(response.stream()).thenReturn(stream);
        when(stream.setMediaType(any())).thenReturn(stream);
        when(stream.setStatus(anyInt())).thenReturn(stream);
        when(stream.output()).thenReturn(new ByteArrayOutputStream());

        ws.handleShow(request, response);

        verify(stream).setStatus(404);
    }

    @Test
    void passesPermissionDenialThrough(@TempDir Path root) throws Exception {
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = requestFor(connectorReturning(403, "{}"));

        Response response = mock(Response.class);
        Response.Stream stream = mock(Response.Stream.class);
        when(response.stream()).thenReturn(stream);
        when(stream.setMediaType(any())).thenReturn(stream);
        when(stream.setStatus(anyInt())).thenReturn(stream);
        when(stream.output()).thenReturn(new ByteArrayOutputStream());

        ws.handleShow(request, response);

        verify(stream).setStatus(403);
    }

    @Test
    void forwardsBranchToTheMeasuresCall(@TempDir Path root) throws Exception {
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        LocalConnector connector = connectorReturning(200, MEASURE_JSON);
        Request request = mock(Request.class);
        when(request.localConnector()).thenReturn(connector);
        when(request.mandatoryParam("component")).thenReturn("p");
        when(request.param("branch")).thenReturn("release/7.x");
        when(request.param("pullRequest")).thenReturn(null);

        ws.handleShow(request, stubbedResponse());

        // The measures call is the permission check and comes first; the branch listing that
        // follows only decides which scope the key names.
        ArgumentCaptor<LocalConnector.LocalRequest> captor = ArgumentCaptor.forClass(LocalConnector.LocalRequest.class);
        verify(connector, org.mockito.Mockito.atLeastOnce()).call(captor.capture());
        LocalConnector.LocalRequest measures = captor.getAllValues().get(0);
        assertEquals("api/measures/component", measures.getPath());
        assertEquals("p", measures.getParam("component"));
        assertEquals("release/7.x", measures.getParam("branch"));
    }

    @Test
    void forwardsPullRequestToTheMeasuresCall(@TempDir Path root) throws Exception {
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        LocalConnector connector = connectorReturning(200, MEASURE_JSON);
        Request request = mock(Request.class);
        when(request.localConnector()).thenReturn(connector);
        when(request.mandatoryParam("component")).thenReturn("p");
        when(request.param("branch")).thenReturn(null);
        when(request.param("pullRequest")).thenReturn("42");

        ws.handleShow(request, stubbedResponse());

        ArgumentCaptor<LocalConnector.LocalRequest> captor = ArgumentCaptor.forClass(LocalConnector.LocalRequest.class);
        verify(connector).call(captor.capture());
        assertEquals("42", captor.getValue().getParam("pullRequest"));
    }

    @Test
    void answersNotFoundWithoutAConfiguredStore() throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, "none");
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settings.asConfig());
        Request request = requestFor(connectorReturning(200, MEASURE_JSON));

        Response response = mock(Response.class);
        Response.Stream stream = mock(Response.Stream.class);
        when(response.stream()).thenReturn(stream);
        when(stream.setMediaType(any())).thenReturn(stream);
        when(stream.setStatus(anyInt())).thenReturn(stream);
        when(stream.output()).thenReturn(new ByteArrayOutputStream());

        ws.handleShow(request, response);

        verify(stream).setStatus(404);
    }

    /**
     * The report_location measure is analysis-supplied data. Even if it names another project's
     * storage key, show must serve the key derived from the requested component - or an analysis
     * token on a throwaway project would be a cross-project read of the store.
     */
    @Test
    void ignoresAMeasureValuePointingAtAnotherProject(@TempDir Path root) throws Exception {
        Path victim = root.resolve("victim/default/dependency-check-report.html");
        Files.createDirectories(victim.getParent());
        Files.write(victim, "<html>secret</html>".getBytes(StandardCharsets.UTF_8));

        String crafted = "{\"component\":{\"measures\":[{\"metric\":\"report_location\","
                + "\"value\":\"victim/default/dependency-check-report.html\"}]}}";
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = requestFor(connectorReturning(200, crafted));

        Response response = stubbedResponse();
        Response.Stream stream = response.stream();

        ws.handleShow(request, response);

        verify(stream).setStatus(404);
        assertEquals("", new String(body.toByteArray(), StandardCharsets.UTF_8).replace(
                "The published Dependency-Check report is no longer available in the store.", ""));
    }

    @Test
    void definesUploadAction(@TempDir Path root) {
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        WebService.Context context = new WebService.Context();
        ws.define(context);
        WebService.Action upload = context.controller("api/dependencycheck").action("report_upload");
        assertNotNull(upload);
        org.junit.jupiter.api.Assertions.assertTrue(upload.isPost());
    }

    @Test
    void uploadStoresTheReportUnderAServerSideKey(@TempDir Path root) throws Exception {
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = uploadRequestFor(connectorReturning(200, MEASURE_JSON), "<html>fresh</html>");

        ws.handleUpload(request, stubbedResponse());

        Path stored = root.resolve("p/default/dependency-check-report.html");
        assertEquals("<html>fresh</html>", new String(Files.readAllBytes(stored), StandardCharsets.UTF_8));
        assertEquals("{\"key\":\"p/default/dependency-check-report.html\"}",
                new String(body.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void uploadRefusedWithoutBrowsePermission(@TempDir Path root) throws Exception {
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = uploadRequestFor(connectorReturning(403, "{}"), "<html>x</html>");

        Response response = stubbedResponse();
        Response.Stream stream = response.stream();

        ws.handleUpload(request, response);

        verify(stream).setStatus(403);
        org.junit.jupiter.api.Assertions.assertFalse(
                Files.exists(root.resolve("p/default/dependency-check-report.html")));
    }

    /**
     * The very first analysis of a new project, branch or pull request runs before the Compute
     * Engine ever creates the component, so api/measures/component answers 404. Refusing there
     * would mean a pull request analysed once and merged never gets a report at all.
     */
    @Test
    void uploadIsAcceptedWhenTheComponentDoesNotExistYetAndTheCallerIsAuthenticated(@TempDir Path root)
            throws Exception {
        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(404, "{}"));
        byPath.put("api/users/current", responseOf(200, "{\"login\":\"scanner\"}"));
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = uploadRequestFor(connectorFor(byPath, responseOf(404, "{}")), "<html>first</html>");

        ws.handleUpload(request, stubbedResponse());

        Path stored = root.resolve("p/default/dependency-check-report.html");
        assertEquals("<html>first</html>", new String(Files.readAllBytes(stored), StandardCharsets.UTF_8));
    }

    /** An unknown component plus an unauthenticated caller is an upload from nobody - refused. */
    @Test
    void uploadIsRefusedWhenTheComponentDoesNotExistAndTheCallerIsNotAuthenticated(@TempDir Path root)
            throws Exception {
        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(404, "{}"));
        byPath.put("api/users/current", responseOf(401, "{}"));
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = uploadRequestFor(connectorFor(byPath, responseOf(404, "{}")), "<html>x</html>");

        Response response = stubbedResponse();
        Response.Stream stream = response.stream();

        ws.handleUpload(request, response);

        verify(stream).setStatus(401);
        org.junit.jupiter.api.Assertions.assertFalse(
                Files.exists(root.resolve("p/default/dependency-check-report.html")));
    }

    /**
     * A file or a portfolio is readable through api/components/tree, so its key could be passed
     * here. A report stored under it could never be reclaimed - the branch listing used by the
     * pruning 404s for such a component - so the upload is refused instead.
     */
    @Test
    void uploadIsRefusedForAComponentThatIsNotAProject(@TempDir Path root) throws Exception {
        String fileComponent = "{\"component\":{\"qualifier\":\"FIL\",\"measures\":[]}}";
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = uploadRequestFor(connectorReturning(200, fileComponent), "<html>x</html>");

        Response response = stubbedResponse();
        Response.Stream stream = response.stream();

        ws.handleUpload(request, response);

        verify(stream).setStatus(400);
        org.junit.jupiter.api.Assertions.assertFalse(
                Files.exists(root.resolve("p/default/dependency-check-report.html")));
    }

    /**
     * The pruning runs after the upload, over a listing that already contains the key just
     * written. A branch listing that does not name that scope - a branch deleted and re-pushed, a
     * lagging read replica, a pull request SonarQube has not registered yet - would classify the
     * fresh key as an orphan and delete it milliseconds after the scanner was told the upload
     * succeeded. The key written by this very request is therefore never an orphan.
     */
    @Test
    void uploadNeverPrunesTheKeyItJustWrote(@TempDir Path root) throws Exception {
        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list",
                responseOf(200, "{\"branches\":[{\"name\":\"main\",\"isMain\":true}]}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = uploadRequestFor(connectorFor(byPath, responseOf(404, "{}")), "<html>fresh</html>");
        when(request.param("branch")).thenReturn("feature/x");

        ws.handleUpload(request, stubbedResponse());

        Path stored = root.resolve("p/br-feature%2Fx/dependency-check-report.html");
        assertEquals("<html>fresh</html>", new String(Files.readAllBytes(stored), StandardCharsets.UTF_8));
    }

    @Test
    void uploadAnswersNoContentWithoutStore() throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, "none");
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settings.asConfig());
        Request request = uploadRequestFor(connectorReturning(200, MEASURE_JSON), "<html>x</html>");

        Response response = stubbedResponse();
        Response.Stream stream = response.stream();

        ws.handleUpload(request, response);

        verify(stream).setStatus(204);
    }

    /**
     * A pipeline commonly passes -Dsonar.branch.name=main while SonarQube's main-branch URL
     * carries no branch parameter. Both sides have to settle on the same key, so the server drops
     * the branch when it is the project's main branch.
     */
    @Test
    void uploadOfTheMainBranchByNameLandsOnTheDefaultScope(@TempDir Path root) throws Exception {
        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200,
                "{\"branches\":[{\"name\":\"main\",\"isMain\":true}]}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = uploadRequestFor(connector, "<html>main</html>");
        when(request.param("branch")).thenReturn("main");

        ws.handleUpload(request, stubbedResponse());

        assertEquals("<html>main</html>",
                new String(Files.readAllBytes(root.resolve("p/default/dependency-check-report.html")), StandardCharsets.UTF_8));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(root.resolve("p/br-main/dependency-check-report.html")),
                "the main branch must not get a branch scope of its own");
        assertEquals("{\"key\":\"p/default/dependency-check-report.html\"}", body.toString("UTF-8"));
    }

    @Test
    void showOfTheMainBranchByNameReadsTheDefaultScope(@TempDir Path root) throws Exception {
        Path report = root.resolve("p/default/dependency-check-report.html");
        Files.createDirectories(report.getParent());
        Files.write(report, "<html>main</html>".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200,
                "{\"branches\":[{\"name\":\"main\",\"isMain\":true}]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = requestFor(connector);
        when(request.param("branch")).thenReturn("main");

        ws.handleShow(request, stubbedResponse());

        assertEquals("<html>main</html>", body.toString("UTF-8"));
    }

    @Test
    void uploadOfANonMainBranchKeepsItsOwnScope(@TempDir Path root) throws Exception {
        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200,
                "{\"branches\":[{\"name\":\"main\",\"isMain\":true},{\"name\":\"release/7.x\"}]}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = uploadRequestFor(connector, "<html>rel</html>");
        when(request.param("branch")).thenReturn("release/7.x");

        ws.handleUpload(request, stubbedResponse());

        assertEquals("<html>rel</html>", new String(Files.readAllBytes(
                root.resolve(org.sonar.dependencycheck.report.store.ReportKey.of("p", "release/7.x", null))),
                StandardCharsets.UTF_8));
    }

    /** When the branch listing does not answer, the supplied value stands - nothing is guessed. */
    @Test
    void uploadKeepsTheSuppliedBranchWhenTheBranchLookupFails(@TempDir Path root) throws Exception {
        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(500, "{}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = uploadRequestFor(connector, "<html>main</html>");
        when(request.param("branch")).thenReturn("main");

        ws.handleUpload(request, stubbedResponse());

        assertEquals("<html>main</html>",
                new String(Files.readAllBytes(root.resolve("p/br-main/dependency-check-report.html")), StandardCharsets.UTF_8));
    }

    @Test
    void uploadPrunesAnOrphanedBranchScopeButKeepsALiveOne(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("p/br-main"));
        Files.write(root.resolve("p/br-main/dependency-check-report.html"), "live".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(root.resolve("p/br-old"));
        Files.write(root.resolve("p/br-old/dependency-check-report.html"), "orphan".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200, "{\"branches\":[{\"name\":\"main\"}]}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = uploadRequestFor(connector, "<html>fresh</html>");

        ws.handleUpload(request, stubbedResponse());

        assertEquals("live", new String(Files.readAllBytes(root.resolve("p/br-main/dependency-check-report.html")),
                StandardCharsets.UTF_8));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(root.resolve("p/br-old/dependency-check-report.html")),
                "the orphaned branch scope must be pruned");
    }

    @Test
    void uploadNeverDeletesTheDefaultScope(@TempDir Path root) throws Exception {
        Path defaultReport = root.resolve("p/default/dependency-check-report.html");
        Files.createDirectories(defaultReport.getParent());
        Files.write(defaultReport, "main analysis".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200, "{\"branches\":[]}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[{\"key\":\"42\"}]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = mock(Request.class);
        when(request.localConnector()).thenReturn(connector);
        when(request.mandatoryParam("component")).thenReturn("p");
        when(request.param("branch")).thenReturn(null);
        when(request.param("pullRequest")).thenReturn("42");
        Request.Part part = mock(Request.Part.class);
        when(part.getInputStream()).thenReturn(new ByteArrayInputStream("<html>pr</html>".getBytes(StandardCharsets.UTF_8)));
        when(request.mandatoryParamAsPart("report")).thenReturn(part);

        ws.handleUpload(request, stubbedResponse());

        assertEquals("main analysis", new String(Files.readAllBytes(defaultReport), StandardCharsets.UTF_8),
                "the default scope must never be pruned, even though it is not a branch or pull request");
    }

    /**
     * The live scopes and the stored keys must be spelled by the same code. This test derives both
     * paths from {@link org.sonar.dependencycheck.report.store.ReportKey}, so a change to the scope
     * vocabulary can never pass here while the reconciliation deletes every live report.
     */
    @Test
    void uploadKeepsALiveBranchWhoseKeyIsDerivedFromReportKey(@TempDir Path root) throws Exception {
        Path live = root.resolve(org.sonar.dependencycheck.report.store.ReportKey.of("p", "release/7.x", null));
        Files.createDirectories(live.getParent());
        Files.write(live, "live".getBytes(StandardCharsets.UTF_8));
        Path orphan = root.resolve(org.sonar.dependencycheck.report.store.ReportKey.of("p", "release/6.x", null));
        Files.createDirectories(orphan.getParent());
        Files.write(orphan, "orphan".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200, "{\"branches\":[{\"name\":\"release/7.x\"}]}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        ws.handleUpload(uploadRequestFor(connector, "<html>fresh</html>"), stubbedResponse());

        assertEquals("live", new String(Files.readAllBytes(live), StandardCharsets.UTF_8),
                "the branch SonarQube still lists must survive");
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(orphan), "the removed branch must be pruned");
    }

    @Test
    void uploadDeletesNothingWhenTheBranchQueryFails(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("p/br-old"));
        Files.write(root.resolve("p/br-old/dependency-check-report.html"), "orphan".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(500, "{}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = uploadRequestFor(connector, "<html>fresh</html>");

        ws.handleUpload(request, stubbedResponse());

        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(root.resolve("p/br-old/dependency-check-report.html")),
                "nothing may be deleted when the branch query fails");
    }

    /**
     * If the branch or pull request listing ever starts to page, everything past the first page
     * would look gone. A paged answer therefore counts as a failed query.
     */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "{\"branches\":[{\"name\":\"main\"}],\"paging\":{\"pageIndex\":1,\"pageSize\":1,\"total\":2}}",
        "{\"branches\":[{\"name\":\"main\"}],\"paging\":{\"p\":2,\"ps\":1}}",
        "{\"branches\":[{\"name\":\"main\"}],\"paging\":{\"p\":1,\"ps\":1}}"
    })
    void uploadDeletesNothingWhenTheBranchListingIsPaged(String branchesJson, @TempDir Path root) throws Exception {
        Path orphan = root.resolve(org.sonar.dependencycheck.report.store.ReportKey.of("p", "old", null));
        Files.createDirectories(orphan.getParent());
        Files.write(orphan, "orphan".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200, branchesJson));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        ws.handleUpload(uploadRequestFor(connector, "<html>fresh</html>"), stubbedResponse());

        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(orphan),
                "a paged listing does not prove a branch is gone, so nothing may be deleted");
    }

    @Test
    void uploadStillPrunesWhenTheListingPagingCoversEverything(@TempDir Path root) throws Exception {
        Path orphan = root.resolve(org.sonar.dependencycheck.report.store.ReportKey.of("p", "old", null));
        Files.createDirectories(orphan.getParent());
        Files.write(orphan, "orphan".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200,
                "{\"branches\":[{\"name\":\"main\"}],\"paging\":{\"pageIndex\":1,\"pageSize\":100,\"total\":1}}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        ws.handleUpload(uploadRequestFor(connector, "<html>fresh</html>"), stubbedResponse());

        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(orphan),
                "a complete single page is a usable answer");
    }

    private Request cleanupRequestFor(LocalConnector connector, Boolean dryRun) {
        Request request = mock(Request.class);
        when(request.localConnector()).thenReturn(connector);
        when(request.paramAsBoolean("dryRun")).thenReturn(dryRun);
        return request;
    }

    private JsonNode cleanupResultOf() throws Exception {
        return new ObjectMapper().readTree(body.toByteArray());
    }

    @Test
    void definesCleanupAction(@TempDir Path root) {
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        WebService.Context context = new WebService.Context();
        ws.define(context);
        WebService.Action cleanup = context.controller("api/dependencycheck").action("cleanup");
        assertNotNull(cleanup);
        org.junit.jupiter.api.Assertions.assertTrue(cleanup.isPost());
        org.junit.jupiter.api.Assertions.assertFalse(cleanup.isInternal(),
                "administrators must be able to find this action in the web API documentation");
    }

    @Test
    void cleanupAnswersForbiddenWithoutGlobalAdministration(@TempDir Path root) throws Exception {
        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/system/info", responseOf(403, "{}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = cleanupRequestFor(connector, null);
        Response response = stubbedResponse();

        ws.handleCleanup(request, response);

        verify(response.stream()).setStatus(403);
    }

    @Test
    void cleanupDryRunDeletesNothingButReportsTheOrphans(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("p/default"));
        Files.write(root.resolve("p/default/dependency-check-report.html"), "x".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(root.resolve("p/br-main"));
        Files.write(root.resolve("p/br-main/dependency-check-report.html"), "x".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(root.resolve("p/br-old"));
        Files.write(root.resolve("p/br-old/dependency-check-report.html"), "x".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/system/info", responseOf(200, "{}"));
        byPath.put("api/projects/search", responseOf(200, "{\"components\":[{\"key\":\"p\"}]}"));
        byPath.put("api/project_branches/list", responseOf(200, "{\"branches\":[{\"name\":\"main\"}]}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = cleanupRequestFor(connector, null); // dryRun defaults to true
        Response response = stubbedResponse();

        ws.handleCleanup(request, response);

        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(root.resolve("p/br-old/dependency-check-report.html")),
                "dryRun must not delete anything");
        JsonNode result = cleanupResultOf();
        org.junit.jupiter.api.Assertions.assertTrue(result.path("dryRun").asBoolean());
        org.junit.jupiter.api.Assertions.assertEquals(0, result.path("deleted").size(),
                "a dry run deletes nothing, so nothing may be reported as deleted");
        java.util.List<String> wouldDelete = new java.util.ArrayList<>();
        for (JsonNode key : result.path("wouldDelete")) {
            wouldDelete.add(key.asText());
        }
        assertEquals(java.util.Collections.singletonList("p/br-old/dependency-check-report.html"), wouldDelete,
                "exactly the orphan must be reported - the default and the live branch scope must survive");
        org.junit.jupiter.api.Assertions.assertEquals(0, result.path("failed").size());
        org.junit.jupiter.api.Assertions.assertEquals(0, result.path("failedProjects").size());
    }

    /**
     * Deleting is irreversible, so what already happened has to be reported however the run ends.
     * A project that blows up mid-run must not swallow the record of the projects before it.
     */
    @Test
    void cleanupReportsWhatHappenedWhenOneProjectFails(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("gone/default"));
        Files.write(root.resolve("gone/default/dependency-check-report.html"), "x".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(root.resolve("boom/default"));
        Files.write(root.resolve("boom/default/dependency-check-report.html"), "x".getBytes(StandardCharsets.UTF_8));

        LocalConnector connector = mock(LocalConnector.class);
        when(connector.call(any())).thenAnswer(invocation -> {
            LocalConnector.LocalRequest req = invocation.getArgument(0);
            if ("api/system/info".equals(req.getPath())) {
                return responseOf(200, "{}");
            }
            if ("api/projects/search".equals(req.getPath())) {
                if ("boom".equals(req.getParam("projects"))) {
                    throw new IllegalStateException("the connector blew up");
                }
                return responseOf(200, "{\"components\":[]}");
            }
            return responseOf(404, "{}");
        });

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Response response = stubbedResponse();

        ws.handleCleanup(cleanupRequestFor(connector, Boolean.FALSE), response);

        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(root.resolve("gone/default/dependency-check-report.html")),
                "the project that could be checked is still reconciled");
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(root.resolve("boom/default/dependency-check-report.html")),
                "the failing project must lose nothing");
        JsonNode result = cleanupResultOf();
        org.junit.jupiter.api.Assertions.assertEquals(1, result.path("deleted").size(),
                "the deletion that did happen must be reported: " + result);
        assertEquals("gone/default/dependency-check-report.html", result.path("deleted").get(0).asText());
        assertEquals(1, result.path("failedProjects").size(), "the failing project must be recorded: " + result);
        assertEquals("boom", result.path("failedProjects").get(0).asText());
    }

    @Test
    void cleanupSaysThatNoStoreIsConfigured() throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, "none");

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/system/info", responseOf(200, "{}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settings.asConfig());
        Response response = stubbedResponse();

        ws.handleCleanup(cleanupRequestFor(connector, Boolean.FALSE), response);

        JsonNode result = cleanupResultOf();
        org.junit.jupiter.api.Assertions.assertFalse(result.path("storeConfigured").asBoolean(true),
                "an unconfigured store must not look like a store that held nothing: " + result);
        org.junit.jupiter.api.Assertions.assertTrue(result.path("message").asText().contains("No report store is configured"),
                "the answer must say why nothing was cleaned up: " + result);
    }

    /**
     * A directory named "münchen" that happens to hold a file called
     * dependency-check-report.html is not a key this plugin ever wrote. Recognising it
     * structurally meant decoding a mangled project key, hearing that no such project exists, and
     * deleting a stranger's file.
     */
    @Test
    void cleanupNeverDeletesAKeyThisPluginCouldNotHaveWritten(@TempDir Path root) throws Exception {
        Path stranger = root.resolve("münchen/default/dependency-check-report.html");
        Files.createDirectories(stranger.getParent());
        Files.write(stranger, "x".getBytes(StandardCharsets.UTF_8));
        Path otherStranger = root.resolve("p/br-münchen/dependency-check-report.html");
        Files.createDirectories(otherStranger.getParent());
        Files.write(otherStranger, "x".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/system/info", responseOf(200, "{}"));
        byPath.put("api/projects/search", responseOf(200, "{\"components\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());

        ws.handleCleanup(cleanupRequestFor(connector, Boolean.FALSE), stubbedResponse());

        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(stranger),
                "a directory this plugin could not have written must be left alone");
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(otherStranger));
        JsonNode result = cleanupResultOf();
        org.junit.jupiter.api.Assertions.assertEquals(0, result.path("deleted").size(), result.toString());
    }

    @Test
    void cleanupDeletesTheWholePrefixOfAGoneProject(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("gone/default"));
        Files.write(root.resolve("gone/default/dependency-check-report.html"), "x".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(root.resolve("gone/br-main"));
        Files.write(root.resolve("gone/br-main/dependency-check-report.html"), "x".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/system/info", responseOf(200, "{}"));
        byPath.put("api/projects/search", responseOf(200, "{\"components\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = cleanupRequestFor(connector, Boolean.FALSE);
        Response response = stubbedResponse();

        ws.handleCleanup(request, response);

        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(root.resolve("gone/default/dependency-check-report.html")));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(root.resolve("gone/br-main/dependency-check-report.html")));
        JsonNode result = cleanupResultOf();
        org.junit.jupiter.api.Assertions.assertFalse(result.path("dryRun").asBoolean());
        org.junit.jupiter.api.Assertions.assertEquals(2, result.path("deleted").size());
    }

    /**
     * A failing existence check is not proof of absence. Anything but a 200 whose components array
     * settles the question must leave the project alone.
     */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(value = {
        "500|{}",
        "403|{}",
        "404|{}",
        "200|not json at all",
        "200|{}",
        "200|{\"components\":\"nope\"}"
    }, delimiter = '|')
    void cleanupSkipsAProjectWhoseExistenceCheckDoesNotSettleIt(int status, String body, @TempDir Path root)
            throws Exception {
        Files.createDirectories(root.resolve("flaky/default"));
        Files.write(root.resolve("flaky/default/dependency-check-report.html"), "x".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/system/info", responseOf(200, "{}"));
        byPath.put("api/projects/search", responseOf(status, body));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = cleanupRequestFor(connector, Boolean.FALSE);
        Response response = stubbedResponse();

        ws.handleCleanup(request, response);

        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(root.resolve("flaky/default/dependency-check-report.html")),
                "an unsettled existence check must skip the project, deleting nothing");
        JsonNode result = cleanupResultOf();
        org.junit.jupiter.api.Assertions.assertEquals(0, result.path("deleted").size());
    }

    /**
     * A project listed under another key in the answer is not this project. Only an entry whose
     * key matches exactly proves it still exists.
     */
    @Test
    void cleanupTreatsAProjectAsGoneOnlyWhenTheSearchAnswersWithoutIt(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("gone/default"));
        Files.write(root.resolve("gone/default/dependency-check-report.html"), "x".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/system/info", responseOf(200, "{}"));
        byPath.put("api/projects/search", responseOf(200, "{\"components\":[{\"key\":\"gone-but-not-really\"}]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        Request request = cleanupRequestFor(connector, Boolean.FALSE);
        Response response = stubbedResponse();

        ws.handleCleanup(request, response);

        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(root.resolve("gone/default/dependency-check-report.html")),
                "a search that does not list the project proves it is gone");
    }

    @Test
    void cleanupAsksTheAdministrationScopedProjectSearch(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("p/default"));
        Files.write(root.resolve("p/default/dependency-check-report.html"), "x".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/system/info", responseOf(200, "{}"));
        byPath.put("api/projects/search", responseOf(200, "{\"components\":[{\"key\":\"p\"}]}"));
        byPath.put("api/project_branches/list", responseOf(200, "{\"branches\":[]}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        ws.handleCleanup(cleanupRequestFor(connector, Boolean.FALSE), stubbedResponse());

        ArgumentCaptor<LocalConnector.LocalRequest> captor = ArgumentCaptor.forClass(LocalConnector.LocalRequest.class);
        verify(connector, org.mockito.Mockito.atLeastOnce()).call(captor.capture());
        boolean asked = false;
        for (LocalConnector.LocalRequest req : captor.getAllValues()) {
            if ("api/projects/search".equals(req.getPath())) {
                asked = true;
                assertEquals("p", req.getParam("projects"));
            }
            org.junit.jupiter.api.Assertions.assertNotEquals("api/components/show", req.getPath(),
                    "a 404 from a browse-gated endpoint must not be taken as proof of absence");
        }
        org.junit.jupiter.api.Assertions.assertTrue(asked, "existence must be confirmed through api/projects/search");
    }

    /** Produces {@code size} bytes without ever holding them, so the test needs no 100 MB array. */
    private static InputStream endlessHtml(final long size) {
        return new InputStream() {

            private long produced;

            @Override
            public int read() {
                if (produced >= size) {
                    return -1;
                }
                produced++;
                return 'x';
            }

            @Override
            public int read(byte[] buffer, int offset, int length) {
                if (produced >= size) {
                    return -1;
                }
                int count = (int) Math.min(length, size - produced);
                java.util.Arrays.fill(buffer, offset, offset + count, (byte) 'x');
                produced += count;
                return count;
            }
        };
    }

    @Test
    void refusesAnUploadAboveTheSizeLimitWithoutStoringAnything(@TempDir Path root) throws Exception {
        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        LocalConnector connector = connectorReturning(200, MEASURE_JSON);
        Request request = mock(Request.class);
        when(request.localConnector()).thenReturn(connector);
        when(request.mandatoryParam("component")).thenReturn("p");
        when(request.param("branch")).thenReturn(null);
        when(request.param("pullRequest")).thenReturn(null);
        Part part = mock(Part.class);
        when(part.getInputStream()).thenReturn(endlessHtml(DependencyCheckReportWebService.MAX_REPORT_BYTES + 1));
        when(request.mandatoryParamAsPart("report")).thenReturn(part);

        Response response = stubbedResponse();
        Response.Stream stream = response.stream();

        ws.handleUpload(request, response);

        verify(stream).setStatus(413);
        org.junit.jupiter.api.Assertions.assertFalse(
                Files.exists(root.resolve("p/default/dependency-check-report.html")),
                "nothing must be published when the limit trips");
        org.junit.jupiter.api.Assertions.assertEquals(0,
                Files.walk(root).filter(Files::isRegularFile).count(), "no partial file may stay behind");
    }

    @Test
    void limitedInputStreamPassesExactlyTheLimitThrough() throws Exception {
        byte[] payload = new byte[16];
        java.util.Arrays.fill(payload, (byte) 'y');
        try (InputStream limited = new DependencyCheckReportWebService.LimitedInputStream(
                new ByteArrayInputStream(payload), 16)) {
            assertEquals(16, readFully(limited));
        }
        try (InputStream limited = new DependencyCheckReportWebService.LimitedInputStream(
                new ByteArrayInputStream(payload), 15)) {
            org.junit.jupiter.api.Assertions.assertThrows(
                    DependencyCheckReportWebService.ReportTooLargeException.class, () -> readFully(limited));
        }
    }

    /**
     * A report written before the main-branch normalisation existed sits under {@code br-main},
     * while SonarQube's own main-branch page asks without any branch at all - which builds the
     * default scope. Both spellings have to be looked up, or the report is unreachable for good:
     * pruning keeps it alive, because {@code br-main} is a live scope.
     */
    @Test
    void showServesAMainBranchReportStoredUnderTheBranchSpelling(@TempDir Path root) throws Exception {
        Path stored = root.resolve("p/br-main/dependency-check-report.html");
        Files.createDirectories(stored.getParent());
        Files.write(stored, "<html>legacy</html>".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200, "{\"branches\":[{\"name\":\"main\",\"isMain\":true}]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());

        ws.handleShow(requestFor(connector), stubbedResponse());

        assertEquals("<html>legacy</html>", new String(body.toByteArray(), StandardCharsets.UTF_8));
    }

    /** The same, for the HEAD probe the page sends first - probe and fetch have to agree. */
    @Test
    void headProbeFindsAMainBranchReportStoredUnderTheBranchSpelling(@TempDir Path root) throws Exception {
        Path stored = root.resolve("p/br-main/dependency-check-report.html");
        Files.createDirectories(stored.getParent());
        Files.write(stored, "<html>legacy</html>".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200, "{\"branches\":[{\"name\":\"main\",\"isMain\":true}]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        Request request = requestFor(connector);
        when(request.method()).thenReturn("HEAD");
        Response response = stubbedResponse();
        Response.Stream stream = response.stream();

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        ws.handleShow(request, response);

        verify(stream).setStatus(200);
        assertEquals("", new String(body.toByteArray(), StandardCharsets.UTF_8));
    }

    /**
     * The page names the main branch explicitly, so the key is normalised onto the default scope -
     * but the report was written under {@code br-main}. The branch spelling is the alternate here.
     */
    @Test
    void showFallsBackFromTheDefaultScopeToTheSuppliedBranch(@TempDir Path root) throws Exception {
        Path stored = root.resolve("p/br-main/dependency-check-report.html");
        Files.createDirectories(stored.getParent());
        Files.write(stored, "<html>branch spelling</html>".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200, "{\"branches\":[{\"name\":\"main\",\"isMain\":true}]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        Request request = requestFor(connector);
        when(request.param("branch")).thenReturn("main");

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        ws.handleShow(request, stubbedResponse());

        assertEquals("<html>branch spelling</html>", new String(body.toByteArray(), StandardCharsets.UTF_8));
    }

    /**
     * The other direction: the branch listing is down, so the key keeps the supplied branch - but
     * the report was written under the default scope while the listing still answered.
     */
    @Test
    void showFallsBackFromTheSuppliedBranchToTheDefaultScope(@TempDir Path root) throws Exception {
        Path stored = root.resolve("p/default/dependency-check-report.html");
        Files.createDirectories(stored.getParent());
        Files.write(stored, "<html>default spelling</html>".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(500, "{}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        Request request = requestFor(connector);
        when(request.param("branch")).thenReturn("main");

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        ws.handleShow(request, stubbedResponse());

        assertEquals("<html>default spelling</html>", new String(body.toByteArray(), StandardCharsets.UTF_8));
    }

    /** Both spellings missing is still a 404 - the fallback must not invent a report. */
    @Test
    void showAnswersNotFoundWhenNeitherSpellingIsStored(@TempDir Path root) throws Exception {
        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(200, "{\"branches\":[{\"name\":\"main\",\"isMain\":true}]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        Request request = requestFor(connector);
        when(request.param("branch")).thenReturn("main");
        Response response = stubbedResponse();
        Response.Stream stream = response.stream();

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        ws.handleShow(request, response);

        verify(stream).setStatus(404);
    }

    /**
     * The key normalisation and the pruning both need the branch listing. One upload must ask
     * SonarQube for it once, not once per reader.
     */
    @Test
    void uploadFetchesTheBranchListingOnlyOnce(@TempDir Path root) throws Exception {
        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list",
                responseOf(200, "{\"branches\":[{\"name\":\"main\",\"isMain\":true},{\"name\":\"feature/x\"}]}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        Request request = uploadRequestFor(connector, "<html>x</html>");
        when(request.param("branch")).thenReturn("feature/x");

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        ws.handleUpload(request, stubbedResponse());

        ArgumentCaptor<LocalConnector.LocalRequest> captor = ArgumentCaptor.forClass(LocalConnector.LocalRequest.class);
        verify(connector, org.mockito.Mockito.atLeastOnce()).call(captor.capture());
        int listings = 0;
        for (LocalConnector.LocalRequest req : captor.getAllValues()) {
            if ("api/project_branches/list".equals(req.getPath())) {
                listings++;
            }
        }
        assertEquals(1, listings, "one upload must fetch the branch listing exactly once");
        org.junit.jupiter.api.Assertions.assertTrue(
                Files.exists(root.resolve("p/br-feature%2Fx/dependency-check-report.html")));
    }

    /**
     * A failed listing has two different meanings, and sharing one answer must not merge them:
     * the key keeps the supplied branch, and the pruning deletes nothing.
     */
    @Test
    void uploadKeepsBothMeaningsOfAFailedBranchListing(@TempDir Path root) throws Exception {
        Path other = root.resolve("p/br-gone/dependency-check-report.html");
        Files.createDirectories(other.getParent());
        Files.write(other, "<html>old</html>".getBytes(StandardCharsets.UTF_8));

        java.util.Map<String, LocalConnector.LocalResponse> byPath = new java.util.HashMap<>();
        byPath.put("api/measures/component", responseOf(200, MEASURE_JSON));
        byPath.put("api/project_branches/list", responseOf(500, "{}"));
        byPath.put("api/project_pull_requests/list", responseOf(200, "{\"pullRequests\":[]}"));
        LocalConnector connector = connectorFor(byPath, responseOf(404, "{}"));

        Request request = uploadRequestFor(connector, "<html>x</html>");
        when(request.param("branch")).thenReturn("main");

        DependencyCheckReportWebService ws = new DependencyCheckReportWebService(settingsFor(root).asConfig());
        ws.handleUpload(request, stubbedResponse());

        org.junit.jupiter.api.Assertions.assertTrue(
                Files.exists(root.resolve("p/br-main/dependency-check-report.html")),
                "a failed listing must not normalise the key");
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(other),
                "a failed listing must delete nothing");
    }

    private static int readFully(InputStream in) throws java.io.IOException {
        int total = 0;
        byte[] buffer = new byte[4];
        int read;
        while ((read = in.read(buffer, 0, buffer.length)) != -1) {
            total += read;
        }
        return total;
    }
}
