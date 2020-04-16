/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2019 dependency-check
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
package org.sonar.dependencycheck.reason;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.dependencycheck.base.DependencyCheckConstants;
import org.sonar.dependencycheck.base.DependencyCheckMetric;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Vulnerability;

import edu.umd.cs.findbugs.annotations.NonNull;

public class DependencyReasonSearcher {

    private static final Logger LOGGER = Loggers.get(DependencyReasonSearcher.class);
    private final DependencyCheckMetric projectMetric;

    private Collection<DependencyReason> dependencyreasons;

    public DependencyReasonSearcher(SensorContext context) {
        this.projectMetric = new DependencyCheckMetric(context.project());
        this.dependencyreasons = new LinkedList<>();
        Iterable<InputFile> poms = context.fileSystem().inputFiles(context.fileSystem().predicates().matchesPathPattern("**/pom.xml"));
        for (InputFile pom : poms) {
            DependencyReason pomReason = new MavenDependencyReason(pom);
            if (pomReason.isReasonable()) {
                dependencyreasons.add(pomReason);
                LOGGER.debug("Found reasonable pom.xml file {}", pom);
            } else {
                LOGGER.debug("Found unreasonable pom.xml file {}", pom);
            }
        }
        String[] gradlePathPatterns = {"**/*.gradle", "**/*.gradle.kts"};
        Iterable<InputFile> buildGradles = context.fileSystem().inputFiles(context.fileSystem().predicates().matchesPathPatterns(gradlePathPatterns));
        for (InputFile buildGradle : buildGradles) {
            DependencyReason gradleReason = new GradleDependencyReason(buildGradle);
            if (gradleReason.isReasonable()) {
                dependencyreasons.add(gradleReason);
                LOGGER.debug("Found reasonable gradle file {}", buildGradle);
            } else {
                LOGGER.debug("Found unreasonable gradle file {}", buildGradle);
            }
        }
        String[] npmPathPatterns = {"**/package-lock.json"};
        Iterable<InputFile> packageLocks = context.fileSystem().inputFiles(context.fileSystem().predicates().matchesPathPatterns(npmPathPatterns));
        for (InputFile packageLock : packageLocks) {
            DependencyReason npmReason = new NPMDependencyReason(packageLock);
            if (npmReason.isReasonable()) {
                dependencyreasons.add(npmReason);
                LOGGER.debug("Found reasonable npm file {}", npmReason);
            } else {
                LOGGER.debug("Found unreasonable npm file {}", npmReason);
            }
        }
    }

    /**
     * @return the dependencyreasons
     */
    public Collection<DependencyReason> getDependencyreasons() {
        return dependencyreasons;
    }
    /**
     *
     * @param dependencyReasons - List of DependencyReason
     * @param dependency - Dependency to go on Search
     * @return DependencyReason with a TextRange or the first DependencyReason in list or null if list is empty
     */
    @NonNull
    private DependencyReason getBestDependencyReason(Dependency dependency) {
        LOGGER.debug("Get the best DependencyReason out of {} for {}", dependencyreasons.size(), dependency.getFileName());
        DependencyReason dependencyReasonWinner = getParentConfigurationFile();
        for (DependencyReason dependencyReason : dependencyreasons) {
            if (dependencyReasonWinner.isDependencyReasonBetterForDependencyThen(dependencyReason, dependency) < 0) {
                dependencyReasonWinner = dependencyReason;
            }
        }
        LOGGER.debug("DependencyReasonWinner: " + dependencyReasonWinner.getInputComponent());
        return dependencyReasonWinner;
    }

    private DependencyReason getParentConfigurationFile() {
        DependencyReason parent = dependencyreasons.iterator().next();
        for (DependencyReason dependencyReason : dependencyreasons) {
            // Simple length check, submodules are often in subfolders
            if (parent.getInputComponent().isFile() && dependencyReason.getInputComponent().isFile()) {
                InputFile file1 = (InputFile) parent.getInputComponent();
                InputFile file2 = (InputFile) dependencyReason.getInputComponent();
                if (file1.toString().length() > file2.toString().length()) {
                    parent = dependencyReason;
                }
            }
        }
        return parent;
    }

    public void addDependenciesToInputComponents(@NonNull Analysis analysis,@NonNull SensorContext context) {
        if (analysis.getDependencies() == null) {
            LOGGER.info("Analyse doesn't report any Dependencies");
            return;
        }
        if (dependencyreasons.isEmpty()) {
            LOGGER.info("We doesn't found any Project configuration file e.g. pom.xml, *.gradle, *.gradle.kts, package-lock.json and can not link dependencies");
            linkIssuesToProject(analysis, context);
            LOGGER.debug("Saving Metrics to project {}", projectMetric.toString());
            projectMetric.saveMeasures(context);
        } else {
            linkIssuesToDependencyReasons(analysis, context);
            for (DependencyReason reason : dependencyreasons) {
                LOGGER.debug("Saving Metrics to Reasonfile {} ", reason.getMetrics().toString());
                reason.getMetrics().saveMeasures(context);
            }
        }
    }
    private void linkIssuesToProject(@NonNull Analysis analysis,@NonNull SensorContext context) {
        LOGGER.info("Linking {} dependencies to project", analysis.getDependencies().size());
        for (Dependency dependency : analysis.getDependencies()) {
            projectMetric.increaseTotalDependencies(1);
            if (!dependency.getVulnerabilities().isEmpty()) {
                Boolean summarize = context.config().getBoolean(DependencyCheckConstants.SUMMARIZE_PROPERTY).orElse(DependencyCheckConstants.SUMMARIZE_PROPERTY_DEFAULT);
                projectMetric.increaseVulnerabilityCount(dependency.getVulnerabilities().size());
                projectMetric.increaseVulnerableDependencies(1);
                if (summarize.booleanValue()) {
                    // One issue per dependency
                    addIssueToProject(context, dependency);
                } else {
                    // One issue per vulnerability
                    for (Vulnerability vulnerability : dependency.getVulnerabilities()) {
                        addIssueToProject(context, dependency, vulnerability);
                    }
                }
            }
        }
    }

    private void linkIssuesToDependencyReasons(@NonNull Analysis analysis,@NonNull SensorContext context) {
        LOGGER.info("Linking {} dependencies to DependencyReasons", analysis.getDependencies().size());
        for (Dependency dependency : analysis.getDependencies()) {
            DependencyReason dependencyReason = getBestDependencyReason(dependency);
            dependencyReason.getMetrics().increaseTotalDependencies(1);
            if (!dependency.getVulnerabilities().isEmpty()) {
                Boolean summarize = context.config().getBoolean(DependencyCheckConstants.SUMMARIZE_PROPERTY).orElse(DependencyCheckConstants.SUMMARIZE_PROPERTY_DEFAULT);
                dependencyReason.getMetrics().increaseVulnerabilityCount(dependency.getVulnerabilities().size());
                dependencyReason.getMetrics().increaseVulnerableDependencies(1);
                if (summarize.booleanValue()) {
                    // One issue per dependency
                    dependencyReason.addIssue(context, dependency);
                } else {
                    // One issue per vulnerability
                    for (Vulnerability vulnerability : dependency.getVulnerabilities()) {
                        dependencyReason.addIssue(context, dependency, vulnerability);
                    }
                }
            }
        }
    }

    public void addIssueToProject(SensorContext context, Dependency dependency, Vulnerability vulnerability) {
        Severity severity = DependencyCheckUtils.cvssToSonarQubeSeverity(vulnerability.getCvssScore(context.config()), context.config());
        NewIssue sonarIssue = context.newIssue();

        NewIssueLocation location = sonarIssue.newLocation()
            .on(context.project())
            .message(DependencyCheckUtils.formatDescription(dependency, vulnerability, context.config()));

        sonarIssue
            .at(location)
            .forRule(RuleKey.of(DependencyCheckConstants.REPOSITORY_KEY, DependencyCheckConstants.RULE_KEY))
            .overrideSeverity(severity)
            .save();
        projectMetric.incrementCount(severity);
    }

    private void addIssueToProject(SensorContext context, Dependency dependency) {
        dependency.sortVulnerabilityBycvssScore(context.config());
        List<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
        Vulnerability highestVulnerability = vulnerabilities.get(0);
        Severity severity = DependencyCheckUtils.cvssToSonarQubeSeverity(highestVulnerability.getCvssScore(context.config()), context.config());

        NewIssue sonarIssue = context.newIssue();

        NewIssueLocation location = sonarIssue.newLocation()
            .on(context.project())
            .message(DependencyCheckUtils.formatDescription(dependency, vulnerabilities, highestVulnerability, context.config()));

        sonarIssue
            .at(location)
            .forRule(RuleKey.of(DependencyCheckConstants.REPOSITORY_KEY, DependencyCheckConstants.RULE_KEY))
            .overrideSeverity(severity)
            .save();
        projectMetric.incrementCount(severity);
    }
}
