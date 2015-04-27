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

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.base.NistMetrics;
import org.sonar.dependencycheck.parser.ReportParser;
import org.sonar.dependencycheck.parser.XmlReportFile;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Vulnerability;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

public class DependencyCheckSensor implements Sensor {

    private static final Logger LOGGER = Loggers.get(DependencyCheckSensor.class);

    private static final double BLOCKER_SECURITY_RATING_LEVEL = 1.0;
    private static final double CRITICAL_SECURITY_RATING_LEVEL = 2.0;
    private static final double MAJOR_SECURITY_RATING_LEVEL = 3.0;
    private static final double MINOR_SECURITY_RATING_LEVEL = 4.0;
    private static final double DEFAULT_SECURITY_RATING_LEVEL = 5.0;

    private final DependencyCheckSensorConfiguration configuration;
    private final ResourcePerspectives resourcePerspectives;
    private final FileSystem fileSystem;
    private final ActiveRules activeRules;
    private final XmlReportFile report;

    private int criticalIssuesCount;   // CVSS 7.0 - 10
    private int majorIssuesCount;      // CVSS 4.0 - 6.9
    private int minorIssuesCount;      // CVSS 0 - 3.9

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

    private void addIssue(InputFile inputFile, Analysis analysis, Dependency dependency, Vulnerability vulnerability) {
        Issuable issuable = this.resourcePerspectives.as(Issuable.class, inputFile);
        if (issuable != null) {
            String severity = DependencyCheckUtils.cvssToSonarQubeSeverity(vulnerability.getCvssScore());
            Issue issue = issuable.newIssueBuilder()
                    .ruleKey(RuleKey.of(DependencyCheckPlugin.REPOSITORY_KEY, DependencyCheckPlugin.RULE_KEY))
                    .message(vulnerability.getDescription())
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
            for (Vulnerability vulnerability : dependency.getVulnerabilities()) {
                InputFile inputFile = fileSystem.inputFile(fileSystem.predicates().is(report.getFile()));
                addIssue(inputFile, analysis, dependency, vulnerability);
            }
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
            addIssues(context, project, analysis);
        } catch (Exception e) {
            throw new RuntimeException("Can not process Dependency-Check report", e);
        } finally {
            profiler.stopInfo();
        }
        saveMeasures(context);
    }

    private void saveMeasures(SensorContext context) {
        context.saveMeasure(NistMetrics.CVSS_HIGH, (double) criticalIssuesCount);
        context.saveMeasure(NistMetrics.CVSS_MEDIUM, (double) majorIssuesCount);
        context.saveMeasure(NistMetrics.CVSS_LOW, (double) minorIssuesCount);
        if (this.criticalIssuesCount > 0) {
            context.saveMeasure(NistMetrics.SECURITY_RATING, DependencyCheckSensor.CRITICAL_SECURITY_RATING_LEVEL);
        } else if (this.majorIssuesCount > 0) {
            context.saveMeasure(NistMetrics.SECURITY_RATING, DependencyCheckSensor.MAJOR_SECURITY_RATING_LEVEL);
        } else if (this.minorIssuesCount > 0) {
            context.saveMeasure(NistMetrics.SECURITY_RATING, DependencyCheckSensor.MINOR_SECURITY_RATING_LEVEL);
        } else {
            context.saveMeasure(NistMetrics.SECURITY_RATING, DependencyCheckSensor.DEFAULT_SECURITY_RATING_LEVEL);
        }
    }

    @Override
    public String toString() {
        return "OWASP Dependency-Check";
    }
}
