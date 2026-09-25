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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.LocalConnector;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.dependencycheck.base.DependencyCheckMetrics;
import org.sonar.dependencycheck.report.store.ReportKey;
import org.sonar.dependencycheck.report.store.ReportStore;
import org.sonar.dependencycheck.report.store.ReportStoreException;
import org.sonar.dependencycheck.report.store.ReportStores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Hands out the published Dependency-Check HTML report.
 *
 * <p>The plugin API has no {@code UserSession}, so this service cannot check permissions itself.
 * Instead it asks SonarQube for the report location through {@link Request#localConnector()},
 * which runs {@code api/measures/component} as the calling user. SonarQube enforces the browse
 * permission there, so its answer <em>is</em> the permission check.
 *
 * <p>The storage key is always derived from the component, branch and pull request the permission
 * check ran on. It is never taken from the caller nor from the {@code report_location} measure,
 * whose value is analysis-supplied and only proves that a report was published.
 */
public class DependencyCheckReportWebService implements WebService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyCheckReportWebService.class);
    private static final String CONTROLLER = "api/dependencycheck";
    private static final String PARAM_COMPONENT = "component";
    private static final String PARAM_BRANCH = "branch";
    private static final String PARAM_PULL_REQUEST = "pullRequest";
    private static final String PARAM_REPORT = "report";
    private static final String PARAM_PROJECT = "project";
    private static final String BRANCHES_PATH = "api/project_branches/list";
    private static final String PARAM_DRY_RUN = "dryRun";
    /** Ceiling for an uploaded report, so a caller cannot fill the temp space or the store. */
    static final long MAX_REPORT_BYTES = 100L * 1024 * 1024;

    private final Configuration config;
    private final ObjectMapper mapper = new ObjectMapper();

    public DependencyCheckReportWebService(Configuration config) {
        this.config = config;
    }

    @Override
    public void define(Context context) {
        NewController controller = context.createController(CONTROLLER);
        controller.setDescription("Dependency-Check HTML reports");

        NewAction show = controller.createAction("show");
        show.setDescription("Returns the published Dependency-Check HTML report of a project");
        show.setHandler(this::handleShow);
        show.setInternal(true);
        show.createParam(PARAM_COMPONENT).setDescription("Project key").setRequired(true);
        show.createParam(PARAM_BRANCH).setDescription("Branch name");
        show.createParam(PARAM_PULL_REQUEST).setDescription("Pull request id");

        NewAction upload = controller.createAction("report_upload");
        upload.setDescription("Publishes the Dependency-Check HTML report of a project. Called by the scanner.");
        upload.setHandler(this::handleUpload);
        upload.setPost(true);
        upload.setInternal(true);
        upload.createParam(PARAM_COMPONENT).setDescription("Project key").setRequired(true);
        upload.createParam(PARAM_BRANCH).setDescription("Branch name");
        upload.createParam(PARAM_PULL_REQUEST).setDescription("Pull request id");
        upload.createParam(PARAM_REPORT).setDescription("The HTML report").setRequired(true);

        NewAction cleanup = controller.createAction("cleanup");
        cleanup.setDescription("Reconciles the Dependency-Check report store against SonarQube: deletes every "
                + "report whose project, branch or pull request no longer exists. Requires global administration "
                + "rights.");
        cleanup.setHandler(this::handleCleanup);
        cleanup.setPost(true);
        cleanup.createParam(PARAM_DRY_RUN)
                .setDescription("Only report what would be deleted, without deleting anything")
                .setBooleanPossibleValues()
                .setDefaultValue(true);

        controller.done();
    }

    void handleShow(Request request, Response response) {
        String component = request.mandatoryParam(PARAM_COMPONENT);
        String branch = request.param(PARAM_BRANCH);
        String pullRequest = request.param(PARAM_PULL_REQUEST);

        LocalConnector.LocalResponse measures = readMeasure(request, component, branch, pullRequest);
        if (measures.getStatus() != 200) {
            // Pass the denial through unchanged - this is the permission check.
            fail(response, measures.getStatus(), "Not allowed to read the Dependency-Check report of " + component);
            return;
        }

        // The measure is analysis-supplied data, so its value is never used as a storage key - only
        // its presence, as proof that a report was published for this component. The key itself is
        // derived from the very component, branch and pull request the permission check above ran
        // on, so a crafted measure cannot point at another project's report.
        if (!reportLocation(measures.getBytes()).isPresent()) {
            fail(response, 404, "No Dependency-Check report has been published for this project.");
            return;
        }
        LocalConnector connector = request.localConnector();
        BranchListing branchListing = new BranchListing(connector, component);
        String keyBranch = keyBranch(branchListing, component, branch, pullRequest);
        String key = ReportKey.of(component, keyBranch, pullRequest);

        try (ReportStore store = openStore()) {
            if (store == null) {
                fail(response, 404, "No report store is configured on this SonarQube instance.");
                return;
            }
            if ("HEAD".equalsIgnoreCase(request.method())) {
                // SonarQube's engine only enforces the verb on POST actions, so a HEAD request
                // otherwise runs this handler in full: the whole report is read from the store and
                // streamed into a response the container throws away. The page probes with HEAD on
                // every view, so that is a full transfer before the user has decided to open the
                // report at all.
                if (!store.exists(key) && !existsUnderAlternateKey(store, branchListing, component, branch,
                        pullRequest, keyBranch)) {
                    fail(response, 404, "The published Dependency-Check report is no longer available in the store.");
                    return;
                }
                writeReportHeaders(response);
                response.stream().setMediaType("text/html").setStatus(200);
                return;
            }
            Optional<InputStream> report = store.read(key);
            if (!report.isPresent()) {
                String alternate = alternateKey(branchListing, component, branch, pullRequest, keyBranch);
                if (alternate != null) {
                    report = store.read(alternate);
                }
            }
            if (!report.isPresent()) {
                fail(response, 404, "The published Dependency-Check report is no longer available in the store.");
                return;
            }
            writeReport(response, report.get());
        } catch (ReportStoreException e) {
            LOGGER.warn("Could not read the Dependency-Check report: {}", e.getMessage());
            LOGGER.debug(e.getMessage(), e);
            fail(response, 500, "Could not read the Dependency-Check report.");
        }
    }

    /**
     * Stores the uploaded report. The key is built here and never taken from the caller, so an
     * analysis token cannot drop a report under someone else's project key. Permission is checked
     * the same way {@code show} checks it: by asking SonarQube as the calling user through
     * {@code api/measures/component}, which SonarQube gates on the browse permission - not on
     * Execute Analysis. Consequently, anyone who can browse a project can replace the report shown
     * for it; this is accepted because the server-derived key confines the effect to that one
     * project.
     *
     * <p>A 404 from that call is <em>not</em> a refusal. The sensor runs during the scanner phase,
     * before the Compute Engine creates the component, so the first analysis of a new project, a
     * new branch or a new pull request always finds the component unknown - and a pull request
     * analysed once and then merged would never get a report at all. Such an upload is accepted as
     * long as the caller is authenticated, which {@code api/users/current} settles. The key is
     * still derived from the component, so nothing can be written outside it, and if the project
     * never materialises the {@code cleanup} action reclaims the report as an orphan.
     */
    void handleUpload(Request request, Response response) {
        String component = request.mandatoryParam(PARAM_COMPONENT);
        String branch = request.param(PARAM_BRANCH);
        String pullRequest = request.param(PARAM_PULL_REQUEST);

        LocalConnector.LocalResponse measures = readMeasure(request, component, branch, pullRequest);
        int status = measures.getStatus();
        if (status != 200 && status != 404) {
            fail(response, status, "Not allowed to publish a Dependency-Check report for " + component);
            return;
        }
        if (status == 200 && !isProject(measures.getBytes())) {
            // A file or a portfolio key would store a report nothing could ever reclaim: the
            // branch listing the pruning and the cleanup rely on does not answer for such a
            // component. Only the 200 path knows the qualifier; on the 404 path below there is no
            // component to ask about yet.
            fail(response, 400, "A Dependency-Check report can only be published for a project, and " + component
                    + " is not one.");
            return;
        }
        if (status == 404) {
            if (!isAuthenticated(request.localConnector())) {
                fail(response, 401, "Authentication is required to publish a Dependency-Check report for " + component);
                return;
            }
            LOGGER.debug("SonarQube does not know the component {} yet; accepting the upload of an authenticated "
                    + "caller as a first analysis", component);
        }

        // One listing for the whole upload: the key normalisation below and the pruning further
        // down both need api/project_branches/list, and fetching it twice per analysis is a call
        // the server does not have to serve. Both still read it on their own terms - a listing
        // that did not answer means "do not normalise" here and "delete nothing" there.
        BranchListing branchListing = new BranchListing(request.localConnector(), component);
        String key = ReportKey.of(component, keyBranch(branchListing, component, branch, pullRequest), pullRequest);
        try (ReportStore store = openStore()) {
            if (store == null) {
                LOGGER.debug("Publishing of the Dependency-Check HTML report is disabled");
                fail(response, 204, "");
                return;
            }
            try (InputStream report = new LimitedInputStream(
                    request.mandatoryParamAsPart(PARAM_REPORT).getInputStream(), MAX_REPORT_BYTES)) {
                String location = store.store(key, report);
                LOGGER.info("Dependency-Check HTML report published to {}", location);
            }
            writeKey(response, key);
            // The response is already written and correct at this point. Pruning this project's
            // orphaned scopes is best-effort housekeeping, never allowed to turn a successful
            // upload into a failed one, so any failure here is caught and logged, not propagated.
            pruneOrphans(store, component, request.localConnector(), key, branchListing);
        } catch (ReportStoreException | IOException e) {
            if (isTooLarge(e)) {
                // Both stores write to a temporary object and only publish it once the stream was
                // read completely, so nothing half written stays behind.
                LOGGER.warn("Refused a Dependency-Check report larger than {} bytes for {}", MAX_REPORT_BYTES, component);
                fail(response, 413, "The Dependency-Check report exceeds the limit of " + MAX_REPORT_BYTES
                        + " bytes and was not stored.");
                return;
            }
            LOGGER.warn("Could not publish the Dependency-Check report: {}", e.getMessage());
            LOGGER.debug(e.getMessage(), e);
            fail(response, 500, "Could not publish the Dependency-Check report.");
        }
    }

    /**
     * Reconciliation, part 2: an administrator action that reconciles the whole store in one go,
     * for reports whose project, branch or pull request disappeared without any further analysis
     * ever running for it - which is the one case the per-upload pruning in {@link #handleUpload}
     * can never reach, since nothing runs an upload for a project that is gone.
     *
     * <p>The plugin API has no {@link org.sonar.api.web.UserRole}-style permission check available
     * here, so this asks SonarQube for {@code api/system/info} as the calling user - an endpoint
     * that itself requires global administration rights - and treats anything other than 200 as
     * "not an administrator".
     */
    void handleCleanup(Request request, Response response) {
        LocalConnector connector = request.localConnector();
        LocalConnector.LocalResponse systemInfo = connector.call(new GetRequest("api/system/info", Collections.emptyMap()));
        if (systemInfo.getStatus() != 200) {
            fail(response, 403, "Administrator rights are required to run this action.");
            return;
        }

        Boolean dryRunParam = request.paramAsBoolean(PARAM_DRY_RUN);
        boolean dryRun = dryRunParam == null || dryRunParam;
        CleanupResult result = new CleanupResult(dryRun);
        try (ReportStore store = openStore()) {
            if (store == null) {
                writeNoStoreConfigured(response);
                return;
            }
            reconcileWholeStore(store, connector, result);
        } catch (ReportStoreException | RuntimeException e) {
            LOGGER.warn("Could not reconcile the Dependency-Check report store: {}", e.getMessage());
            LOGGER.debug(e.getMessage(), e);
            if (result.isEmpty()) {
                // Nothing was touched, so there is nothing to account for.
                fail(response, 500, "Could not reconcile the Dependency-Check report store.");
                return;
            }
            // Deleting is irreversible: whatever already happened has to be reported, however the
            // run ended.
            result.abortedWith(e);
        }
        writeCleanupResult(response, result);
    }

    /** What a cleanup run actually did, reported whether it ran to the end or not. */
    private static class CleanupResult {

        private final boolean dryRun;
        private final List<String> deleted = new ArrayList<>();
        private final List<String> failed = new ArrayList<>();
        private final List<String> failedProjects = new ArrayList<>();
        @Nullable
        private String abortedBy;

        CleanupResult(boolean dryRun) {
            this.dryRun = dryRun;
        }

        boolean isEmpty() {
            return deleted.isEmpty() && failed.isEmpty() && failedProjects.isEmpty();
        }

        void abortedWith(Throwable e) {
            abortedBy = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /**
     * Lists the whole store, groups the keys by project, and reconciles each project either
     * against its branches and pull requests (project still exists) or wholesale (project is
     * gone). Never touches a project whose existence check did not settle the question - the
     * safety rule applies to a single flaky check just as much as to a failed query.
     */
    private void reconcileWholeStore(ReportStore store, LocalConnector connector, CleanupResult result)
            throws ReportStoreException {
        Map<String, List<String>> keysByProject = groupByProject(store.list(""));
        for (Map.Entry<String, List<String>> project : keysByProject.entrySet()) {
            try {
                reconcileProject(store, connector, project.getKey(), project.getValue(), result);
            } catch (RuntimeException e) {
                // One project must not abort the run: deleting is irreversible, and an exception
                // on project 7 of 50 would otherwise leave the six already deleted ones
                // unaccounted for.
                LOGGER.warn("Could not reconcile the Dependency-Check reports of {}: {}", project.getKey(), e.getMessage());
                LOGGER.debug(e.getMessage(), e);
                result.failedProjects.add(project.getKey());
            }
        }
    }

    private void reconcileProject(ReportStore store, LocalConnector connector, String projectKey, List<String> keys,
            CleanupResult result) {
        for (String key : orphansOfProject(connector, projectKey, keys)) {
            if (result.dryRun) {
                result.deleted.add(key);
                continue;
            }
            try {
                if (store.delete(key)) {
                    result.deleted.add(key);
                }
            } catch (ReportStoreException e) {
                LOGGER.warn("Could not delete the orphaned Dependency-Check report {}: {}", key, e.getMessage());
                LOGGER.debug(e.getMessage(), e);
                result.failed.add(key);
            }
        }
    }

    /** Every stored key of {@code projectKey}, decoded via {@link ReportKey#projectKeyOf}, grouped together. */
    private Map<String, List<String>> groupByProject(List<String> keys) {
        Map<String, List<String>> byProject = new LinkedHashMap<>();
        for (String key : keys) {
            if (!ReportKey.isReportKey(key)) {
                // Not a key this service ever produced. The store directory may hold unrelated
                // files, so such a key is skipped - never deleted.
                continue;
            }
            Optional<String> projectKey = ReportKey.projectKeyOf(key);
            if (!projectKey.isPresent()) {
                continue;
            }
            byProject.computeIfAbsent(projectKey.get(), k -> new ArrayList<>()).add(key);
        }
        return byProject;
    }

    /** What the existence check could establish about a project. */
    private enum Existence {
        /** SonarQube listed the project - only its dead branches and pull requests are orphans. */
        EXISTS,
        /** SonarQube answered authoritatively without the project - every key under it is an orphan. */
        GONE,
        /** The check did not settle the question - nothing may be deleted for this project. */
        UNKNOWN
    }

    /**
     * The orphaned keys among {@code keys}, all stored under {@code projectKey}: every one of them
     * if the project is provably gone, the ones whose branch or pull request is gone if it still
     * exists, or none at all if the existence check did not settle the question.
     */
    private List<String> orphansOfProject(LocalConnector connector, String projectKey, List<String> keys) {
        Existence existence = existenceOf(connector, projectKey);
        if (existence == Existence.GONE) {
            return new ArrayList<>(keys);
        }
        if (existence != Existence.EXISTS) {
            return Collections.emptyList();
        }
        Optional<Set<String>> live = liveScopes(new BranchListing(connector, projectKey), connector, projectKey);
        if (!live.isPresent()) {
            return Collections.emptyList();
        }
        return orphansAmong(keys, live.get());
    }

    /**
     * Asks {@code api/projects/search} whether {@code projectKey} still exists.
     *
     * <p>Absence has to be established positively. {@code api/components/show} is gated on the
     * browse permission, which a global administrator does not automatically hold on a private
     * project, and SonarQube's component APIs answer 404 rather than 403 in places so as not to
     * leak whether a component exists - so a 404 from there is no proof of anything.
     * {@code api/projects/search} is administration scoped instead, and the caller has already
     * proven global administration rights before this runs. Only a 200 whose {@code components}
     * array is present settles the question; everything else is {@link Existence#UNKNOWN}.
     */
    private Existence existenceOf(LocalConnector connector, String projectKey) {
        LocalConnector.LocalResponse search = connector.call(
                new GetRequest("api/projects/search", Collections.singletonMap("projects", projectKey)));
        if (search.getStatus() != 200) {
            return Existence.UNKNOWN;
        }
        try {
            JsonNode components = mapper.readTree(search.getBytes()).path("components");
            if (!components.isArray()) {
                return Existence.UNKNOWN;
            }
            for (JsonNode component : components) {
                if (projectKey.equals(component.path("key").asText(null))) {
                    return Existence.EXISTS;
                }
            }
            return Existence.GONE;
        } catch (IOException e) {
            LOGGER.warn("Could not read the api/projects/search response for {}: {}", projectKey, e.getMessage());
            LOGGER.debug(e.getMessage(), e);
            return Existence.UNKNOWN;
        }
    }

    private void writeCleanupResult(Response response, CleanupResult result) {
        ObjectNode json = mapper.createObjectNode();
        json.put("storeConfigured", true);
        json.put("dryRun", result.dryRun);
        // A dry run deletes nothing, so its findings are not called "deleted" - that field is
        // reserved for reports that really are gone.
        ArrayNode deletedNode = json.putArray(result.dryRun ? "wouldDelete" : "deleted");
        result.deleted.forEach(deletedNode::add);
        ArrayNode failedNode = json.putArray("failed");
        result.failed.forEach(failedNode::add);
        ArrayNode failedProjectsNode = json.putArray("failedProjects");
        result.failedProjects.forEach(failedProjectsNode::add);
        if (result.abortedBy != null) {
            json.put("abortedBy", result.abortedBy);
        }
        writeJson(response, json);
    }

    /** A cleanup without a store is not a cleanup that found nothing - it says so. */
    private void writeNoStoreConfigured(Response response) {
        ObjectNode json = mapper.createObjectNode();
        json.put("storeConfigured", false);
        json.put("message", "No report store is configured on this SonarQube instance, so there is nothing to clean up.");
        writeJson(response, json);
    }

    private void writeJson(Response response, ObjectNode json) {
        try (OutputStream out = response.stream().setMediaType("application/json").setStatus(200).output()) {
            mapper.writeValue(out, json);
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    private static boolean isTooLarge(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ReportTooLargeException) {
                return true;
            }
        }
        return false;
    }

    /** Thrown once a report upload passes {@link #MAX_REPORT_BYTES}. */
    static class ReportTooLargeException extends IOException {

        private static final long serialVersionUID = 1L;

        ReportTooLargeException(long limit) {
            super("The report exceeds the limit of " + limit + " bytes");
        }
    }

    /**
     * Fails instead of passing on the byte that would exceed {@code limit}. The store reads the
     * report through this stream, so it aborts before anything is published.
     */
    static class LimitedInputStream extends FilterInputStream {

        private final long limit;
        private long read;

        LimitedInputStream(InputStream delegate, long limit) {
            super(delegate);
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int value = super.read(buffer, offset, length);
            if (value > 0) {
                count(value);
            }
            return value;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = super.skip(n);
            if (skipped > 0) {
                count(skipped);
            }
            return skipped;
        }

        private void count(long bytes) throws IOException {
            read += bytes;
            if (read > limit) {
                throw new ReportTooLargeException(limit);
            }
        }
    }

    private void writeKey(Response response, String key) {
        try (OutputStream out = response.stream().setMediaType("application/json").setStatus(200).output()) {
            out.write(("{\"key\":\"" + key + "\"}").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    @Nullable
    private ReportStore openStore() throws ReportStoreException {
        return ReportStores.create(config).orElse(null);
    }

    /** Whether the component of an {@code api/measures/component} answer is a project ({@code TRK}). */
    private boolean isProject(byte[] measuresJson) {
        try {
            return "TRK".equals(mapper.readTree(measuresJson).path("component").path("qualifier").asText(null));
        } catch (IOException e) {
            LOGGER.warn("Could not read the measures response: {}", e.getMessage());
            LOGGER.debug(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Whether the caller is an authenticated SonarQube user. {@code api/users/current} answers 200
     * for an analysis token and 401 for an anonymous or bogus one, so it is the cheapest way to
     * tell the two apart when the component itself does not exist yet.
     */
    private boolean isAuthenticated(LocalConnector connector) {
        return connector.call(new GetRequest("api/users/current", Collections.emptyMap())).getStatus() == 200;
    }

    private LocalConnector.LocalResponse readMeasure(Request request, String component, @Nullable String branch,
            @Nullable String pullRequest) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_COMPONENT, component);
        params.put("metricKeys", DependencyCheckMetrics.REPORT_LOCATION.getKey());
        if (branch != null && !branch.trim().isEmpty()) {
            params.put(PARAM_BRANCH, branch);
        }
        if (pullRequest != null && !pullRequest.trim().isEmpty()) {
            params.put(PARAM_PULL_REQUEST, pullRequest);
        }
        return request.localConnector().call(new GetRequest("api/measures/component", params));
    }

    /**
     * The branch the storage key is built with: nothing when {@code branch} names the project's
     * main branch, the supplied value otherwise.
     *
     * <p>Writer and reader name the main branch differently. A pipeline passes
     * {@code -Dsonar.branch.name=main} while SonarQube's own main-branch URL carries no
     * {@code branch} parameter at all, so the report would be written under {@code br-main} and
     * looked up under {@code default} - a permanent mismatch in both directions. Only the server
     * knows which branch is the main one, so the normalisation happens here, on both the writing
     * and the reading side: a main-branch report always lands on {@link ReportKey#DEFAULT_SCOPE},
     * which the reconciliation already keeps live unconditionally.
     *
     * <p>When the branch listing does not answer, the supplied value stands - guessing would move
     * the key, and a moved key is a report nobody ever finds again - and the fallback is logged at
     * WARN, because it is the one case in which the key depends on an endpoint that has nothing to
     * do with the report. {@link #alternateKey} still finds such a report on the way out; the log
     * explains why the two spellings exist at all.
     */
    @Nullable
    private String keyBranch(BranchListing listing, String projectKey, @Nullable String branch,
            @Nullable String pullRequest) {
        if (branch == null || branch.trim().isEmpty() || (pullRequest != null && !pullRequest.trim().isEmpty())) {
            // A pull request scope never carries a branch, so nothing has to be looked up.
            return branch;
        }
        int status = listing.response().getStatus();
        if (status != 200) {
            // Worth a warning, not a debug line: which key this request uses now depends on the
            // health of an unrelated endpoint, and an operator looking at a report that is not
            // where they expect it has no other way to connect the two.
            LOGGER.warn("api/project_branches/list answered {} for {}, so the Dependency-Check report key of branch "
                    + "'{}' keeps that branch name instead of being normalised onto the main-branch scope. If this "
                    + "is the main branch, its report is stored beside the one written while the listing answered.",
                    status, projectKey, branch);
            return branch;
        }
        Optional<String> mainBranch = mainBranchOf(listing, projectKey);
        if (mainBranch.isPresent() && mainBranch.get().equals(branch.trim())) {
            return null;
        }
        return branch;
    }

    /**
     * The {@code api/project_branches/list} answer of one request, fetched at most once.
     *
     * <p>A single upload needs it twice - once to decide whether the branch is the main one and
     * the key therefore lands on the default scope, once to decide which stored scopes are still
     * live - and there is no reason to ask SonarQube twice within one request. The two readers
     * keep interpreting it independently: a listing that did not answer still means "do not
     * normalise the key" for the one and "delete nothing" for the other, which are different
     * answers to the same failure and must not collapse into one.
     */
    private static final class BranchListing {

        private final LocalConnector connector;
        private final String projectKey;
        @Nullable
        private LocalConnector.LocalResponse response;

        BranchListing(LocalConnector connector, String projectKey) {
            this.connector = connector;
            this.projectKey = projectKey;
        }

        /** The answer, fetched on the first call. Never fetched at all when nothing asks for it. */
        LocalConnector.LocalResponse response() {
            if (response == null) {
                response = connector.call(
                        new GetRequest(BRANCHES_PATH, Collections.singletonMap(PARAM_PROJECT, projectKey)));
            }
            return response;
        }
    }

    /** Whether a report of this component sits under {@link #alternateKey}, for the HEAD probe. */
    private boolean existsUnderAlternateKey(ReportStore store, BranchListing listing, String component,
            @Nullable String branch, @Nullable String pullRequest, @Nullable String keyBranch)
            throws ReportStoreException {
        String alternate = alternateKey(listing, component, branch, pullRequest, keyBranch);
        return alternate != null && store.exists(alternate);
    }

    /**
     * The other spelling the report of this component may be stored under, or {@code null} if
     * there is none. Looked up only once the primary key missed, so the normal page view still
     * costs exactly one store lookup.
     *
     * <p>The main branch has two spellings, and both occur in a real store. A report written
     * before the main-branch normalisation existed, or written while
     * {@code api/project_branches/list} was not answering - in which case {@link #keyBranch} keeps
     * the supplied branch - sits under {@code br-<main>}, while a normalised one sits under
     * {@link ReportKey#DEFAULT_SCOPE}. Answering 404 for a report that is demonstrably there is
     * worse than the second lookup: pruning keeps such a report alive forever, because its scope
     * is a live one, so it would stay unreachable for good.
     *
     * <p>A pull request has only ever had one spelling, so it has no alternate.
     */
    @Nullable
    private String alternateKey(BranchListing listing, String component, @Nullable String branch,
            @Nullable String pullRequest, @Nullable String keyBranch) {
        if (pullRequest != null && !pullRequest.trim().isEmpty()) {
            return null;
        }
        if (keyBranch != null && !keyBranch.trim().isEmpty()) {
            // The primary key names the supplied branch, so the other spelling is the default scope.
            return ReportKey.of(component, null, null);
        }
        if (branch != null && !branch.trim().isEmpty()) {
            // The supplied branch was normalised onto the default scope; the other spelling names it.
            return ReportKey.of(component, branch, null);
        }
        // No branch was supplied at all - SonarQube's own main-branch URL carries none - so only
        // the server can say which branch name the other spelling would carry.
        Optional<String> mainBranch = mainBranchOf(listing, component);
        if (!mainBranch.isPresent() || mainBranch.get().trim().isEmpty()) {
            return null;
        }
        return ReportKey.of(component, mainBranch.get(), null);
    }

    /**
     * The name of the branch SonarQube flags as {@code isMain} for {@code projectKey}, or an empty
     * optional if the listing did not answer, could not be parsed, or names no main branch.
     */
    private Optional<String> mainBranchOf(BranchListing listing, String projectKey) {
        LocalConnector.LocalResponse resp = listing.response();
        if (resp.getStatus() != 200) {
            return Optional.empty();
        }
        try {
            JsonNode branches = mapper.readTree(resp.getBytes()).path("branches");
            if (!branches.isArray()) {
                return Optional.empty();
            }
            for (JsonNode candidate : branches) {
                JsonNode name = candidate.path("name");
                if (candidate.path("isMain").asBoolean(false) && name.isTextual()) {
                    return Optional.of(name.asText());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read the api/project_branches/list response for {}: {}", projectKey, e.getMessage());
            LOGGER.debug(e.getMessage(), e);
        }
        return Optional.empty();
    }

    private Optional<String> reportLocation(byte[] measuresJson) {
        try {
            JsonNode measures = mapper.readTree(measuresJson).path("component").path("measures");
            for (JsonNode measure : measures) {
                if (DependencyCheckMetrics.REPORT_LOCATION.getKey().equals(measure.path("metric").asText())) {
                    String value = measure.path("value").asText("");
                    return value.isEmpty() ? Optional.empty() : Optional.of(value);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read the measures response: {}", e.getMessage());
            LOGGER.debug(e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Reconciliation, part 1: after an upload, deletes every report stored for {@code projectKey}
     * whose branch or pull request no longer exists - except {@code freshKey}, the key this
     * request just wrote, which is exempt unconditionally. Never allowed to fail the upload it
     * runs after - every exception is caught and logged here.
     */
    private void pruneOrphans(ReportStore store, String projectKey, LocalConnector connector, String freshKey,
            BranchListing branchListing) {
        try {
            Optional<Set<String>> live = liveScopes(branchListing, connector, projectKey);
            if (!live.isPresent()) {
                // The safety rule: when in doubt, delete nothing.
                return;
            }
            for (String orphan : orphansAmong(store.list(ReportKey.prefixOf(projectKey)), live.get())) {
                if (orphan.equals(freshKey)) {
                    // The key this very request wrote, and the caller has already been told the
                    // upload succeeded. A listing that does not name its scope yet - a branch
                    // deleted and re-pushed, a lagging read replica, a pull request SonarQube has
                    // not registered - must never turn that into an immediate deletion.
                    continue;
                }
                store.delete(orphan);
            }
        } catch (RuntimeException | ReportStoreException e) {
            LOGGER.warn("Could not reconcile the Dependency-Check reports of {}: {}", projectKey, e.getMessage());
            LOGGER.debug(e.getMessage(), e);
        }
    }

    /**
     * The scopes still live for {@code projectKey} according to SonarQube - every branch and pull
     * request it currently has, plus the {@link #DEFAULT_SCOPE} - or an empty optional if either
     * query failed or answered something this method cannot parse. An empty optional means the
     * caller must delete nothing for this project.
     */
    private Optional<Set<String>> liveScopes(BranchListing branchListing, LocalConnector connector, String projectKey) {
        Optional<Set<String>> branches = scopesFrom(branchListing.response(), BRANCHES_PATH, projectKey,
                "branches", "name", ReportKey::branchScope);
        if (!branches.isPresent()) {
            return Optional.empty();
        }
        Optional<Set<String>> pullRequests = scopesFrom(
                connector.call(new GetRequest("api/project_pull_requests/list",
                        Collections.singletonMap(PARAM_PROJECT, projectKey))),
                "api/project_pull_requests/list", projectKey, "pullRequests", "key", ReportKey::pullRequestScope);
        if (!pullRequests.isPresent()) {
            return Optional.empty();
        }
        Set<String> live = new HashSet<>();
        // The scope of a main-branch analysis, which is never an orphan regardless of what the
        // branch and pull request listings answer.
        live.add(ReportKey.DEFAULT_SCOPE);
        live.addAll(branches.get());
        live.addAll(pullRequests.get());
        return Optional.of(live);
    }

    /**
     * Calls the given local API with {@code project=projectKey} and turns the {@code arrayField}
     * array of its JSON answer into scopes, reading {@code nameField} off each element and mapping
     * it through {@code toScope} - always a {@link ReportKey} method, so the live scopes are
     * spelled by the very code that writes the keys and the two cannot drift apart. An empty
     * optional means the answer was not a 200, or could not be parsed the way it was expected to -
     * either way, nothing this project's caller can safely act on.
     */
    private Optional<Set<String>> scopesFrom(LocalConnector.LocalResponse resp, String path, String projectKey,
            String arrayField, String nameField, java.util.function.Function<String, String> toScope) {
        if (resp.getStatus() != 200) {
            return Optional.empty();
        }
        try {
            JsonNode root = mapper.readTree(resp.getBytes());
            JsonNode array = root.path(arrayField);
            if (!array.isArray()) {
                return Optional.empty();
            }
            if (isPaged(root.path("paging"), array.size())) {
                // Entries beyond this page exist but were not returned. They would look gone, so
                // the whole answer is treated as a failed query: when in doubt, delete nothing.
                LOGGER.warn("The {} answer for {} is paged; skipping the reconciliation of this project", path, projectKey);
                return Optional.empty();
            }
            Set<String> scopes = new HashSet<>();
            for (JsonNode element : array) {
                JsonNode name = element.path(nameField);
                if (!name.isTextual() && !name.isNumber()) {
                    // Malformed entry - the whole answer cannot be trusted.
                    return Optional.empty();
                }
                scopes.add(toScope.apply(name.asText()));
            }
            return Optional.of(scopes);
        } catch (IOException e) {
            LOGGER.warn("Could not read the {} response: {}", path, e.getMessage());
            LOGGER.debug(e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Whether {@code paging} says there is more than what these {@code returned} items show:
     * a total above them, or a page index and size that leave room for a further page. Neither
     * branch nor pull request listings page today, but if one ever starts to, every entry past the
     * first page would look gone - so an answer that even might be paged counts as paged.
     */
    private static boolean isPaged(JsonNode paging, int returned) {
        if (!paging.isObject()) {
            return false;
        }
        int total = intField(paging, "total", -1);
        if (total > returned) {
            return true;
        }
        int pageIndex = intField(paging, "pageIndex", intField(paging, "p", -1));
        int pageSize = intField(paging, "pageSize", intField(paging, "ps", -1));
        if (pageIndex > 1) {
            // Not the first page, so earlier entries were not seen here either.
            return true;
        }
        if (pageSize > 0 && returned >= pageSize && total < 0) {
            // A full page and no total to rule out a next one.
            return true;
        }
        return pageIndex > 0 && pageSize > 0 && total > pageIndex * pageSize;
    }

    private static int intField(JsonNode node, String field, int fallback) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : fallback;
    }

    /** The keys among {@code keys} whose scope is not in {@code liveScopes}. */
    private List<String> orphansAmong(List<String> keys, Set<String> liveScopes) {
        List<String> orphans = new ArrayList<>();
        for (String key : keys) {
            Optional<String> scope = ReportKey.scopeOf(key);
            if (scope.isPresent() && !liveScopes.contains(scope.get())) {
                orphans.add(key);
            }
        }
        return orphans;
    }

    /** The headers and status of a report response, without its body. */
    private void writeReportHeaders(Response response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy", "sandbox allow-scripts; default-src 'none'; "
                + "style-src 'unsafe-inline'; script-src 'unsafe-inline'; img-src 'self' data:; "
                + "font-src 'self' data:");
        // Lets report_page.js tell a real answer from this action apart from SonarQube's single-page
        // app, which answers an unknown route (e.g. a wrongly resolved relative URL) with HTTP 200
        // and its HTML shell rather than a 404.
        response.setHeader("X-Dependency-Check-Report", "1");
    }

    /**
     * The Dependency-Check report carries inline JavaScript. Served from the SonarQube origin that
     * would be an XSS vector against every viewer's session, so the response forbids sniffing and
     * carries its own {@code sandbox} policy: the report is opened as a top-level document (see
     * {@code report_page.js}, which deliberately does not use an iframe because SonarQube's own
     * page-wide policy forbids framing), and the sandbox directive puts it in an opaque origin
     * without access to the SonarQube session.
     *
     * <p>{@code img-src} and {@code font-src} allow {@code data:} because a stock Dependency-Check
     * report inlines its Bootstrap and DataTables icons and glyphs as {@code data:} URIs, and CSP
     * has no implicit allowance for them - without these two directives every icon of the report
     * is blocked.
     */
    private void writeReport(Response response, InputStream report) {
        writeReportHeaders(response);
        try (InputStream in = report; OutputStream out = response.stream().setMediaType("text/html").setStatus(200).output()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not stream the Dependency-Check report: {}", e.getMessage());
            LOGGER.debug(e.getMessage(), e);
        }
    }

    private void fail(Response response, int status, String message) {
        try (OutputStream out = response.stream().setMediaType("text/plain").setStatus(status).output()) {
            out.write(message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.debug(e.getMessage(), e);
        }
    }

    /** Minimal {@link LocalConnector.LocalRequest} for a GET on the given local API path. */
    private static class GetRequest implements LocalConnector.LocalRequest {

        private final String path;
        private final Map<String, String> params;

        GetRequest(String path, Map<String, String> params) {
            this.path = path;
            this.params = params;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getMediaType() {
            return "application/json";
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public boolean hasParam(String key) {
            return params.containsKey(key);
        }

        @Override
        public String getParam(String key) {
            return params.get(key);
        }

        @Override
        public List<String> getMultiParam(String key) {
            String value = params.get(key);
            return value == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(value);
        }

        @Override
        public Optional<String> getHeader(String name) {
            return Optional.empty();
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> map = new HashMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                map.put(entry.getKey(), new String[] { entry.getValue() });
            }
            return map;
        }
    }
}
