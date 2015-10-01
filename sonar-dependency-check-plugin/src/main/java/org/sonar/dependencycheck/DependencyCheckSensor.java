/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015 Steve Springett
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.dependencycheck;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.base.DependencyCheckMetrics;
import org.sonar.dependencycheck.parser.ReportParser;
import org.sonar.dependencycheck.parser.XmlReportFile;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Vulnerability;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DependencyCheckSensor implements Sensor {

    private static final Logger LOGGER = Loggers.get(DependencyCheckSensor.class);

    private final DependencyCheckSensorConfiguration configuration;
    private final ResourcePerspectives resourcePerspectives;
    private final FileSystem fileSystem;
    private final ActiveRules activeRules;
    private final XmlReportFile report;

    private int totalDependencies;
    private int vulnerableDependencies;
    private int vulnerabilityCount;
    private int criticalIssuesCount;
    private int majorIssuesCount;
    private int minorIssuesCount;

    public DependencyCheckSensor(
            DependencyCheckSensorConfiguration configuration,
            ResourcePerspectives resourcePerspectives,
            FileSystem fileSystem,
            ActiveRules activeRules) {
        this.configuration = configuration;
        this.resourcePerspectives = resourcePerspectives;
        this.fileSystem = fileSystem;
        this.activeRules = activeRules;
        this.report = new XmlReportFile(configuration, fileSystem);
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        return this.report.exist();
    }

    private void addIssue(Project project, Analysis analysis, Dependency dependency, Vulnerability vulnerability) {
        Issuable issuable = this.resourcePerspectives.as(Issuable.class, (Resource)project);
        if (issuable != null) {
            String severity = DependencyCheckUtils.cvssToSonarQubeSeverity(vulnerability.getCvssScore());
            Issue issue = issuable.newIssueBuilder()
                    .ruleKey(RuleKey.of(DependencyCheckPlugin.REPOSITORY_KEY, DependencyCheckPlugin.RULE_KEY))
                    .message(formatDescription(dependency, vulnerability))
                    .severity(severity)
                    .attribute("cve", vulnerability.getName())
                    .attribute("file", dependency.getFileName())
                    .line(null)
                    .build();
            if (issuable.addIssue(issue)) {
                incrementCount(vulnerability, severity);
            }
        }
    }

    /**
     *      todo: Add Markdown formatting if and when Sonar supports it
     *      https://jira.codehaus.org/browse/SONAR-4161
     */
    private String formatDescription(Dependency dependency, Vulnerability vulnerability) {
        StringBuilder sb = new StringBuilder();
        sb.append("Filename: ").append(dependency.getFileName()).append(" | ");
        sb.append("Reference: " ).append(vulnerability.getName()).append(" | ");
        sb.append("CVSS Score: " ).append(vulnerability.getCvssScore()).append(" | ");
        if (StringUtils.isNotBlank(vulnerability.getCwe())) {
            sb.append("Category: " ).append(vulnerability.getCwe()).append(" | ");
        }
        sb.append(vulnerability.getDescription());
        return sb.toString();
    }

    private void incrementCount(Vulnerability vulnerability, String severity) {
        switch (severity) {
            case Severity.CRITICAL:
                this.criticalIssuesCount++;
                break;
            case Severity.MAJOR:
                this.majorIssuesCount++;
                break;
            case Severity.MINOR:
                this.minorIssuesCount++;
                break;
        }
    }

    private void addIssues(SensorContext context, Project project, Analysis analysis) {
        if (analysis.getDependencies() == null) {
            return;
        }
        for (Dependency dependency : analysis.getDependencies()) {
            InputFile testFile = fileSystem.inputFile(fileSystem.predicates().is(new File(dependency.getFilePath())));

            int depVulnCount = dependency.getVulnerabilities().size();

            if (depVulnCount > 0) {
                vulnerableDependencies++;
                saveMetricOnFile(context, testFile, DependencyCheckMetrics.VULNERABLE_DEPENDENCIES, (double) depVulnCount);
            }
            saveMetricOnFile(context, testFile, DependencyCheckMetrics.TOTAL_VULNERABILITIES, (double) depVulnCount);
            saveMetricOnFile(context, testFile, DependencyCheckMetrics.TOTAL_DEPENDENCIES, (double) depVulnCount);

            for (Vulnerability vulnerability : dependency.getVulnerabilities()) {
                addIssue(project, analysis, dependency, vulnerability);
                vulnerabilityCount++;
            }
        }
    }

    private void saveMetricOnFile(SensorContext context, InputFile inputFile, Metric metric, double value) {
        if (inputFile != null) {
            context.saveMeasure(inputFile, new Measure(metric, value));
        }
    }

    private Analysis parseAnalysis() throws IOException, ParserConfigurationException, SAXException {
        try (InputStream stream = this.report.getInputStream()) {
            return new ReportParser().parse(stream);
        }
    }

    @Override
    public void analyse(Project project, SensorContext context) {
        Profiler profiler = Profiler.create(LOGGER);
        profiler.startInfo("Process Dependency-Check report");
        try {
            Analysis analysis = parseAnalysis();
            totalDependencies = analysis.getDependencies().size();
            addIssues(context, project, analysis);
        } catch (Exception e) {
            throw new RuntimeException("Can not process Dependency-Check report. Ensure the report and all dependencies being analyzed are located within the project workspace and that sonar.sources is set to reflect these paths (or set sonar.sources=.)", e);
        } finally {
            profiler.stopInfo();
        }
        saveMeasures(context);
    }

    private void saveMeasures(SensorContext context) {
        context.saveMeasure(DependencyCheckMetrics.HIGH_SEVERITY_VULNS, (double) criticalIssuesCount);
        context.saveMeasure(DependencyCheckMetrics.MEDIUM_SEVERITY_VULNS, (double) majorIssuesCount);
        context.saveMeasure(DependencyCheckMetrics.LOW_SEVERITY_VULNS, (double) minorIssuesCount);
        context.saveMeasure(DependencyCheckMetrics.TOTAL_DEPENDENCIES, (double) totalDependencies);
        context.saveMeasure(DependencyCheckMetrics.VULNERABLE_DEPENDENCIES, (double) vulnerableDependencies);
        context.saveMeasure(DependencyCheckMetrics.TOTAL_VULNERABILITIES, (double) vulnerabilityCount);

        context.saveMeasure(DependencyCheckMetrics.INHERITED_RISK_SCORE, DependencyCheckMetrics.inheritedRiskScore(criticalIssuesCount, majorIssuesCount, minorIssuesCount));
        context.saveMeasure(DependencyCheckMetrics.VULNERABLE_COMPONENT_RATIO, DependencyCheckMetrics.vulnerableComponentRatio(vulnerabilityCount, vulnerableDependencies));
    }

    @Override
    public String toString() {
        return "OWASP Dependency-Check";
    }
}
