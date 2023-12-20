/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2023 dependency-check
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
package org.sonar.dependencycheck.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.issue.impact.Severity;

import edu.umd.cs.findbugs.annotations.NonNull;

public class DependencyCheckMetric {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyCheckMetric.class);
    private final InputComponent inputcomponent;

    private int totalDependencies;
    private int vulnerableDependencies;
    private int vulnerabilityCount;
    private int highIssuesCount;
    private int mediumIssuesCount;
    private int lowIssuesCount;

    public DependencyCheckMetric(@NonNull InputComponent inputComponent) {
        this.inputcomponent = inputComponent;
        this.totalDependencies = 0;
        this.vulnerableDependencies = 0;
        this.vulnerabilityCount = 0;
        this.highIssuesCount = 0;
        this.mediumIssuesCount = 0;
        this.lowIssuesCount = 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "DependencyCheckMetric [inputcomponent=" + inputcomponent + ", totalDependencies=" + totalDependencies
                + ", vulnerableDependencies=" + vulnerableDependencies + ", vulnerabilityCount=" + vulnerabilityCount
                + ", highIssuesCount=" + highIssuesCount + ", mediumIssuesCount=" + mediumIssuesCount
                + ", lowIssuesCount=" + lowIssuesCount + "]";
    }

    public void incrementCount(Severity severity) {
        switch (severity) {
            case HIGH:
                this.highIssuesCount++;
                break;
            case MEDIUM:
                this.mediumIssuesCount++;
                break;
            case LOW:
                this.lowIssuesCount++;
                break;
            default:
                LOGGER.debug("Unknown severity {}", severity);
                break;
        }
    }

    public void increaseVulnerabilityCount(int amount) {
        this.vulnerabilityCount += amount;
    }

    public void increaseVulnerableDependencies(int amount) {
        this.vulnerableDependencies += amount;
    }

    public void increaseTotalDependencies(int amount) {
        this.totalDependencies += amount;
    }

    public void saveMeasures(SensorContext context) {
        LOGGER.debug("Save measures on {}", inputcomponent);
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.HIGH_SEVERITY_VULNS).on(inputcomponent).withValue(highIssuesCount).save();
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.MEDIUM_SEVERITY_VULNS).on(inputcomponent).withValue(
            mediumIssuesCount).save();
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.LOW_SEVERITY_VULNS).on(inputcomponent).withValue(
            lowIssuesCount).save();
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.TOTAL_DEPENDENCIES).on(inputcomponent).withValue(totalDependencies).save();
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.VULNERABLE_DEPENDENCIES).on(inputcomponent).withValue(vulnerableDependencies).save();
        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.TOTAL_VULNERABILITIES).on(inputcomponent).withValue(vulnerabilityCount).save();

        context.<Integer>newMeasure().forMetric(DependencyCheckMetrics.INHERITED_RISK_SCORE).on(inputcomponent)
            .withValue(DependencyCheckMetrics.inheritedRiskScore(highIssuesCount, mediumIssuesCount, lowIssuesCount)).save();
        context.<Double>newMeasure().forMetric(DependencyCheckMetrics.VULNERABLE_COMPONENT_RATIO).on(inputcomponent)
            .withValue(DependencyCheckMetrics.vulnerableComponentRatio(vulnerabilityCount, vulnerableDependencies)).save();
    }
}
