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
package org.sonar.dependencycheck.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.*;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.dependencycheck.DependencyCheckPlugin;
import org.sonar.dependencycheck.base.NistMetrics;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Vulnerability;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

public class DependencyCheckSensor implements Sensor {
    private static final Logger LOG = LoggerFactory.getLogger(DependencyCheckSensor.class);

    private final ResourcePerspectives resourcePerspectives;
    private final XmlReportFile report;

    private int criticalIssuesCount;   // CVSS 7.0 - 10
    private int majorIssuesCount;      // CVSS 4.0 - 6.9
    private int minorIssuesCount;      // CVSS 0 - 3.9

    public DependencyCheckSensor(
            DependencyCheckSensorConfiguration configuration,
            ResourcePerspectives resourcePerspectives,
            FileSystem fileSystem,
            ActiveRules activeRules) {
        this.resourcePerspectives = resourcePerspectives;
        this.report = new XmlReportFile(configuration, fileSystem);
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        return this.report.exist();
    }

    private void addIssue(SensorContext context, Resource resource, Analysis analysis, Vulnerability vulnerability) {
        //context.index(resource);


        Issuable issuable = this.resourcePerspectives.as(Issuable.class, resource);
        if (issuable != null) {
            String severity = vulnerability.getCvssScore();
            Issue issue = issuable.newIssueBuilder()
                    .ruleKey(RuleKey.of(DependencyCheckPlugin.REPOSITORY_KEY, DependencyCheckPlugin.RULE_KEY))
                    .message(vulnerability.getDescription())
                    .severity(severity)
                    .attribute("cve", vulnerability.getName())
                    .build();
            if (issuable.addIssue(issue)) {
                incrementCount(vulnerability, severity);
            }
        }
    }

    private void incrementCount(Vulnerability vulnerability, String severity) {
        if (Severity.CRITICAL.equals(severity)) {
            this.criticalIssuesCount++;
        } else if (Severity.MAJOR.equals(severity)) {
            this.majorIssuesCount++;
        } else if (Severity.MINOR.equals(severity)) {
            this.minorIssuesCount++;
        }
    }

    private void addIssues(SensorContext context, Project project, Analysis analysis) {
        if (analysis.getDependencies() == null) {
            return;
        }
        for (Dependency dependency : analysis.getDependencies()) {
            for (Vulnerability vulnerability : dependency.getVulnerabilities()) {
                Resource resource =  createResource(context, dependency, project);
                addIssue(context, resource, analysis, vulnerability);
            }
        }
    }

    private Resource createResource(SensorContext context, Dependency dependency, Project project) {
        String filePath = dependency.getFilePath();
        File resource = File.fromIOFile(new java.io.File(filePath), project);
        context.index(resource);
        // Reload resource to have it fully initialized
        resource = context.getResource(resource);
        return resource;
    }

    private Analysis parseAnalysis() throws IOException, ParserConfigurationException, SAXException {
        InputStream stream = this.report.getInputStream();
        try {
            return new ReportParser().parse(stream);
        } finally {
            stream.close();
        }
    }

    @Override
    public void analyse(Project project, SensorContext context) {
        TimeProfiler profiler = new TimeProfiler().start("Process Dependency-Check report");
        try {
            Analysis analysis = parseAnalysis();
            addIssues(context, project, analysis);
        } catch (Exception e) {
            throw new SonarException("Can not process Dependency-Check report", e);
        } finally {
            profiler.stop();
        }
        saveMeasures(context);
    }

    private void saveMeasures(SensorContext context) {
        context.saveMeasure(NistMetrics.CVSS_HIGH, (double) criticalIssuesCount);
        context.saveMeasure(NistMetrics.CVSS_MEDIUM, (double) majorIssuesCount);
        context.saveMeasure(NistMetrics.CVSS_LOW, (double) minorIssuesCount);
    }

    @Override
    public String toString() {
        return "Dependency-Check sensor";
    }
}
