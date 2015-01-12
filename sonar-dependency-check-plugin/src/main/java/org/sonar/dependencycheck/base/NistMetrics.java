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

import java.util.Arrays;
import java.util.List;

public final class NistMetrics implements Metrics {

    public static final String DOMAIN = "NIST";

    public static final String SECURITY_RATING_KEY = "common-vulnerability-scoring-system";
    public static final Metric SECURITY_RATING = new Metric.Builder(NistMetrics.SECURITY_RATING_KEY, "Common Vulnerability Scoring System", Metric.ValueType.INT)
            .setDescription("Common Vulnerability Scoring System")
            .setDirection(Metric.DIRECTION_BETTER)
            .setQualitative(true)
            .setDomain(NistMetrics.DOMAIN)
            .setBestValue(0.0)
            .create();

    /**
     * The following metrics are used for the chart Impact versus Likelihood
     */
    public static final Metric CVSS_HIGH = new Metric.Builder("cvss-high", "CVSS High Severity Issues", Metric.ValueType.INT)
            .setDescription("CVSS High Severity Issues")
            .setDirection(Metric.DIRECTION_WORST)
            .setQualitative(false)
            .setDomain(NistMetrics.DOMAIN)
            .setBestValue(10.0)
            .setHidden(true)
            .create();

    public static final Metric CVSS_MEDIUM = new Metric.Builder("cvss-medium", "CVSS Medium Severity Issues", Metric.ValueType.INT)
            .setDescription("CVSS Medium Severity Issues")
            .setDirection(Metric.DIRECTION_WORST)
            .setQualitative(false)
            .setDomain(NistMetrics.DOMAIN)
            .setBestValue(6.9)
            .setHidden(true)
            .create();

    public static final Metric CVSS_LOW = new Metric.Builder("cvss-low", "CVSS Low Severity Issues", Metric.ValueType.INT)
            .setDescription("CVSS Low Severity Issues")
            .setDirection(Metric.DIRECTION_WORST)
            .setQualitative(false)
            .setDomain(NistMetrics.DOMAIN)
            .setBestValue(3.9)
            .setHidden(true)
            .create();


    @Override
    public List<Metric> getMetrics() {
        return Arrays.asList(
                NistMetrics.SECURITY_RATING,
                NistMetrics.CVSS_HIGH,
                NistMetrics.CVSS_MEDIUM,
                NistMetrics.CVSS_LOW
        );
    }
}
