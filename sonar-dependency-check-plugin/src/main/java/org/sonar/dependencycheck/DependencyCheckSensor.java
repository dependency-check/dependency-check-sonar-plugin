/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2017 Steve Springett
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.dependencycheck;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.measures.Metric;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.dependencycheck.base.DependencyCheckConstants;
import org.sonar.dependencycheck.base.DependencyCheckMetrics;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.parser.ReportParser;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Vulnerability;
import org.sonar.dependencycheck.report.HtmlReportFile;
import org.sonar.dependencycheck.report.XmlReportFile;

public class DependencyCheckSensor implements Sensor {

    private static final Logger LOGGER = Loggers.get(DependencyCheckSensor.class);
    private static final String SENSOR_NAME = "Dependency-Check";

    private final FileSystem fileSystem;
    private final PathResolver pathResolver;

    private int totalDependencies;
    private int vulnerableDependencies;
    private int vulnerabilityCount;
    private int blockerIssuesCount;
    private int criticalIssuesCount;
    private int majorIssuesCount;
    private int minorIssuesCount;
    private int infoIssuesCount;

    public DependencyCheckSensor(FileSystem fileSystem, PathResolver pathResolver) {
        this.fileSystem = fileSystem;
        this.pathResolver = pathResolver;
    }

    private void addIssue(SensorContext context, Dependency dependency, Vulnerability vulnerability) {
        Float severityBlocker = context.config().getFloat(DependencyCheckConstants.SEVERITY_BLOCKER).orElse(DependencyCheckConstants.SEVERITY_BLOCKER_DEFAULT);
        Float severityCritical = context.config().getFloat(DependencyCheckConstants.SEVERITY_CRITICAL).orElse(DependencyCheckConstants.SEVERITY_CRITICAL_DEFAULT);
        Float severityMajor = context.config().getFloat(DependencyCheckConstants.SEVERITY_MAJOR).orElse(DependencyCheckConstants.SEVERITY_MAJOR_DEFAULT);
        Float severityMinor = context.config().getFloat(DependencyCheckConstants.SEVERITY_MINOR).orElse(DependencyCheckConstants.SEVERITY_MINOR_DEFAULT);
        Severity severity = DependencyCheckUtils.cvssToSonarQubeSeverity(vulnerability.getCvssScore(), severityBlocker ,severityCritical, severityMajor, severityMinor);

        context.newIssue()
                .forRule(RuleKey.of(DependencyCheckPlugin.REPOSITORY_KEY, DependencyCheckPlugin.RULE_KEY))
                .at(new DefaultIssueLocation()
                        .on(context.module())
                        .message(formatDescription(dependency, vulnerability))
                )
                .overrideSeverity(severity)
                .save();

        incrementCount(severity);
    }

    private void addIssue(SensorContext context, Dependency dependency) {
        dependency.sortVulnerabilityBycvssScore();
        List<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
        Float severityBlocker = context.config().getFloat(DependencyCheckConstants.SEVERITY_BLOCKER).orElse(DependencyCheckConstants.SEVERITY_BLOCKER_DEFAULT);
        Float severityCritical = context.config().getFloat(DependencyCheckConstants.SEVERITY_CRITICAL).orElse(DependencyCheckConstants.SEVERITY_CRITICAL_DEFAULT);
        Float severityMajor = context.config().getFloat(DependencyCheckConstants.SEVERITY_MAJOR).orElse(DependencyCheckConstants.SEVERITY_MAJOR_DEFAULT);
        Float severityMinor = context.config().getFloat(DependencyCheckConstants.SEVERITY_MINOR).orElse(DependencyCheckConstants.SEVERITY_MINOR_DEFAULT);
        Vulnerability highestVulnerability = vulnerabilities.get(0);
        Severity severity = DependencyCheckUtils.cvssToSonarQubeSeverity(highestVulnerability.getCvssScore(), severityBlocker ,severityCritical, severityMajor, severityMinor);
        context.newIssue()
            .forRule(RuleKey.of(DependencyCheckPlugin.REPOSITORY_KEY, DependencyCheckPlugin.RULE_KEY))
            .at(new DefaultIssueLocation()
                .on(context.module())
                .message(formatDescription(dependency, vulnerabilities, highestVulnerability)))
            .overrideSeverity(severity)
            .save();

        incrementCount(severity);
    }

    /**
     * TODO: Add Markdown formatting if and when Sonar supports it
     * https://jira.sonarsource.com/browse/SONAR-4161
     */
    private String formatDescription(Dependency dependency, Vulnerability vulnerability) {
        StringBuilder sb = new StringBuilder();
        sb.append("Filename: ").append(dependency.getFileName()).append(" | ");
        sb.append("Reference: ").append(vulnerability.getName()).append(" | ");
        sb.append("CVSS Score: ").append(vulnerability.getCvssScore()).append(" | ");
        if (StringUtils.isNotBlank(vulnerability.getCwe())) {
            sb.append("Category: ").append(vulnerability.getCwe()).append(" | ");
        }
        sb.append(vulnerability.getDescription());
        return sb.toString();
    }

    private String formatDescription(Dependency dependency, Collection<Vulnerability> vulnerabilities, Vulnerability highestVulnerability) {
        StringBuilder sb = new StringBuilder();
        sb.append("Filename: ").append(dependency.getFileName()).append(" | ");
        sb.append("Highest CVSS Score: ").append(highestVulnerability.getCvssScore()).append(" | ");
        sb.append("Amount of CVSS: ").append(vulnerabilities.size()).append(" | ");
        sb.append("References: ");
        for (Vulnerability vulnerability : vulnerabilities) {
            sb.append(vulnerability.getName()).append(" (").append(vulnerability.getCvssScore()).append(") ");
        }
        return sb.toString().trim();
    }

    private void incrementCount(Severity severity) {
        switch (severity) {
            case BLOCKER:
                this.blockerIssuesCount++;
                break;
            case CRITICAL:
                this.criticalIssuesCount++;
                break;
            case MAJOR:
                this.majorIssuesCount++;
                break;
            case MINOR:
                this.minorIssuesCount++;
                break;
            case INFO:
                this.infoIssuesCount++;
                break;
            default:
                LOGGER.debug("Unknown severity {}", severity);
                break;
        }
    }

    private void addIssues(SensorContext context, Analysis analysis) {
        if (analysis.getDependencies() == null) {
            return;
        }
        for (Dependency dependency : analysis.getDependencies()) {
            InputFile testFile = fileSystem.inputFile(
                    fileSystem.predicates().hasPath(
                            escapeReservedPathChars(dependency.getFilePath())
                    )
            );

            int depVulnCount = dependency.getVulnerabilities().size();
            vulnerabilityCount += depVulnCount;

            if (depVulnCount > 0) {
                vulnerableDependencies++;
                saveMetricOnFile(context, testFile, DependencyCheckMetrics.VULNERABLE_DEPENDENCIES, depVulnCount);
            }
            saveMetricOnFile(context, testFile, DependencyCheckMetrics.TOTAL_VULNERABILITIES, depVulnCount);
            saveMetricOnFile(context, testFile, DependencyCheckMetrics.TOTAL_DEPENDENCIES, depVulnCount);

            if (!dependency.getVulnerabilities().isEmpty()
                && context.config().getBoolean(DependencyCheckConstants.SUMMARIZE_PROPERTY).orElse(DependencyCheckConstants.SUMMARIZE_PROPERTY_DEFAULT)) {
                // One Issue per dependency
                addIssue(context, dependency);
            } else {
                for (Vulnerability vulnerability : dependency.getVulnerabilities()) {
                    addIssue(context, dependency, vulnerability);
                }
            }
        }
    }

    private void saveMetricOnFile(SensorContext context, @Nullable InputFile inputFile, Metric<Integer> metric, int value) {
        if (inputFile != null) {
            context.<Integer>newMeasure().on(inputFile).forMetric(metric).withValue(value);
        }
    }

    private Analysis parseAnalysis(SensorContext context) throws IOException, XMLStreamException {
        XmlReportFile report = XmlReportFile.getXmlReport(context.config(), fileSystem, this.pathResolver);
        return ReportParser.parse(report.getInputStream());
    }

    private void saveMeasures(SensorContext context) {
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.CRITICAL_SEVERITY_VULNS).on(context.module()).withValue(blockerIssuesCount).save();
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.HIGH_SEVERITY_VULNS).on(context.module()).withValue(criticalIssuesCount).save();
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.MEDIUM_SEVERITY_VULNS).on(context.module()).withValue(majorIssuesCount).save();
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.LOW_SEVERITY_VULNS).on(context.module()).withValue(minorIssuesCount).save();
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.TOTAL_DEPENDENCIES).on(context.module()).withValue(totalDependencies).save();
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.VULNERABLE_DEPENDENCIES).on(context.module()).withValue(vulnerableDependencies).save();
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.TOTAL_VULNERABILITIES).on(context.module()).withValue(vulnerabilityCount).save();
        LOGGER.debug("Found {} info Issues", infoIssuesCount);

        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.INHERITED_RISK_SCORE).on(context.module())
            .withValue(DependencyCheckMetrics.inheritedRiskScore(blockerIssuesCount, criticalIssuesCount, majorIssuesCount, minorIssuesCount)).save();
        context.<Double>newMeasure().forMetric(DependencyCheckMetrics.VULNERABLE_COMPONENT_RATIO).on(context.module())
            .withValue(DependencyCheckMetrics.vulnerableComponentRatio(vulnerabilityCount, vulnerableDependencies)).save();

        try {
            HtmlReportFile htmlReportFile = HtmlReportFile.getHtmlReport(context.config(), fileSystem, pathResolver);
            String htmlReport = htmlReportFile.getReportContent();
            if (htmlReport != null) {
                LOGGER.info("Upload Dependency-Check HTML-Report");
                context.<String>newMeasure().forMetric(DependencyCheckMetrics.REPORT).on(context.module()).withValue(htmlReport).save();
            }
        } catch (FileNotFoundException e) {
            LOGGER.info(e.getMessage());
            LOGGER.debug(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return SENSOR_NAME;
    }

    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor.name(SENSOR_NAME);
    }

    @Override
    public void execute(SensorContext sensorContext) {
        Profiler profiler = Profiler.create(LOGGER);
        profiler.startInfo("Process Dependency-Check report");
        try {
            Analysis analysis = parseAnalysis(sensorContext);
            this.totalDependencies = analysis.getDependencies().size();
            addIssues(sensorContext, analysis);
        } catch (FileNotFoundException e) {
            LOGGER.info("Analysis skipped/aborted due to missing report file");
            LOGGER.debug(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.warn("Analysis aborted due to: IO Errors", e);
        } catch (XMLStreamException e) {
            LOGGER.warn("Analysis aborted due to: XML is not valid", e);
        }
        saveMeasures(sensorContext);
        profiler.stopInfo();
    }

    /**
     * The following characters are reserved on Windows systems.
     * Some are also reserved on Unix systems.
     *
     * < (less than)
     * > (greater than)
     * : (colon)
     * " (double quote)
     * / (forward slash)
     * \ (backslash)
     * | (vertical bar or pipe)
     * ? (question mark)
     * (asterisk)
     */
    private String escapeReservedPathChars(String path) {
        /*
         * TODO: For the time being, only try to replace ? (question mark) since that is the only reserved character
         * intentionally used by Dependency-Check.
         */
        String replacement = path.contains("/") ? "/" : "\\";
        return path.replace("?", replacement);
    }
}
