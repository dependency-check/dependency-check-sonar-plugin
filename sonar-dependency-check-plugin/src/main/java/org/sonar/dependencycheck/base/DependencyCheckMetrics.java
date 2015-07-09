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
package org.sonar.dependencycheck.base;

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.measures.SumChildValuesFormula;

import java.util.Arrays;
import java.util.List;

public final class DependencyCheckMetrics implements Metrics {

    public static final String DOMAIN = "OWASP-Dependency-Check";

    public static final String INHERITED_RISK_SCORE_KEY = "inherited_risk_score";
    public static final String VULNERABLE_COMPONENT_RATIO_KEY = "vulnerable_component_ratio";

    public static final String TOTAL_DEPENDENCIES_KEY = "total_dependencies";
    public static final String VULNERABLE_DEPENDENCIES_KEY = "vulnerable_dependencies";
    public static final String TOTAL_VULNERABILITIES_KEY = "total_vulnerabilities";
    public static final String HIGH_SEVERITY_VULNS_KEY = "high_severity_vulns";
    public static final String MEDIUM_SEVERITY_VULNS_KEY = "medium_severity_vulns";
    public static final String LOW_SEVERITY_VULNS_KEY = "low_severity_vulns";


    public static double inheritedRiskScore(int high, int medium, int low) {
        return (double) ((high * 5) + (medium * 3) + (low * 1));
    }

    public static double vulnerableComponentRatio(int vulnerabilities, int vulnerableComponents) {
        double ratio = 0.0;
        if(vulnerableComponents > 0) {
            ratio = (double) vulnerabilities / vulnerableComponents;
        }
        return ratio;
    }

    public static final Metric INHERITED_RISK_SCORE = new Metric.Builder(DependencyCheckMetrics.INHERITED_RISK_SCORE_KEY, "Inherited Risk Score", Metric.ValueType.INT)
            .setDescription("Inherited Risk Score")
            .setDirection(Metric.DIRECTION_BETTER)
            .setQualitative(true)
            .setDomain(DependencyCheckMetrics.DOMAIN)
            .setFormula(new SumChildValuesFormula(false))
            .setBestValue(0.0)
            .create();

    public static final Metric VULNERABLE_COMPONENT_RATIO = new Metric.Builder(DependencyCheckMetrics.VULNERABLE_COMPONENT_RATIO_KEY, "Vulnerable Component Ratio", Metric.ValueType.PERCENT)
            .setDescription("Vulnerable Component Ratio")
            .setDirection(Metric.DIRECTION_BETTER)
            .setQualitative(true)
            .setDomain(DependencyCheckMetrics.DOMAIN)
            .setFormula(new SumChildValuesFormula(false))
            .setBestValue(0.0)
            .create();

    public static final Metric HIGH_SEVERITY_VULNS = new Metric.Builder(HIGH_SEVERITY_VULNS_KEY, "High Severity Vulnerabilities", Metric.ValueType.INT)
            .setDescription("High Severity Vulnerabilities")
            .setDirection(Metric.DIRECTION_WORST)
            .setQualitative(false)
            .setDomain(DependencyCheckMetrics.DOMAIN)
            .setFormula(new SumChildValuesFormula(false))
            .setBestValue(0.0)
            .setHidden(false)
            .create();

    public static final Metric MEDIUM_SEVERITY_VULNS = new Metric.Builder(MEDIUM_SEVERITY_VULNS_KEY, "Medium Severity Vulnerabilities", Metric.ValueType.INT)
            .setDescription("Medium Severity Vulnerabilities")
            .setDirection(Metric.DIRECTION_WORST)
            .setQualitative(false)
            .setDomain(DependencyCheckMetrics.DOMAIN)
            .setFormula(new SumChildValuesFormula(false))
            .setBestValue(0.0)
            .setHidden(false)
            .create();

    public static final Metric LOW_SEVERITY_VULNS = new Metric.Builder(LOW_SEVERITY_VULNS_KEY, "Low Severity Vulnerabilities", Metric.ValueType.INT)
            .setDescription("Low Severity Vulnerabilities")
            .setDirection(Metric.DIRECTION_WORST)
            .setQualitative(false)
            .setDomain(DependencyCheckMetrics.DOMAIN)
            .setFormula(new SumChildValuesFormula(false))
            .setBestValue(0.0)
            .setHidden(false)
            .create();

    public static final Metric TOTAL_DEPENDENCIES = new Metric.Builder(TOTAL_DEPENDENCIES_KEY, "Total Dependencies", Metric.ValueType.INT)
            .setDescription("Total Dependencies")
            .setDirection(Metric.DIRECTION_WORST)
            .setQualitative(false)
            .setDomain(DependencyCheckMetrics.DOMAIN)
            .setFormula(new SumChildValuesFormula(false))
            .setHidden(false)
            .create();

    public static final Metric VULNERABLE_DEPENDENCIES = new Metric.Builder(VULNERABLE_DEPENDENCIES_KEY, "Vulnerable Dependencies", Metric.ValueType.INT)
            .setDescription("Vulnerable Dependencies")
            .setDirection(Metric.DIRECTION_WORST)
            .setQualitative(false)
            .setDomain(DependencyCheckMetrics.DOMAIN)
            .setFormula(new SumChildValuesFormula(false))
            .setBestValue(0.0)
            .setHidden(false)
            .create();

    public static final Metric TOTAL_VULNERABILITIES = new Metric.Builder(TOTAL_VULNERABILITIES_KEY, "Total Vulnerabilities", Metric.ValueType.INT)
            .setDescription("Total Vulnerabilities")
            .setDirection(Metric.DIRECTION_WORST)
            .setQualitative(false)
            .setDomain(DependencyCheckMetrics.DOMAIN)
            .setFormula(new SumChildValuesFormula(false))
            .setBestValue(0.0)
            .setHidden(false)
            .create();

    @Override
    public List<Metric> getMetrics() {
        return Arrays.asList(
                DependencyCheckMetrics.INHERITED_RISK_SCORE,
                DependencyCheckMetrics.VULNERABLE_COMPONENT_RATIO,
                DependencyCheckMetrics.HIGH_SEVERITY_VULNS,
                DependencyCheckMetrics.MEDIUM_SEVERITY_VULNS,
                DependencyCheckMetrics.LOW_SEVERITY_VULNS,
                DependencyCheckMetrics.TOTAL_DEPENDENCIES,
                DependencyCheckMetrics.VULNERABLE_DEPENDENCIES,
                DependencyCheckMetrics.TOTAL_VULNERABILITIES
        );
    }
}