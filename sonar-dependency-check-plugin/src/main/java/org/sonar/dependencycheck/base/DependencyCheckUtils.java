/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2019 SonarSecurityCommunity
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

import java.util.Collection;
import java.util.Optional;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Configuration;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Identifier;
import org.sonar.dependencycheck.parser.element.Vulnerability;

import edu.umd.cs.findbugs.annotations.NonNull;

public final class DependencyCheckUtils {

    private static final String CRITICAL = "critical";
    private static final String HIGH = "high";
    private static final String MEDIUM = "medium";
    private static final String MODERATE = "moderate";
    private static final String LOW = "low";

    private DependencyCheckUtils() {
    }

    public static SMInputFactory newStaxParser() throws FactoryConfigurationError {
        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        return new SMInputFactory(xmlFactory);
    }

    public static Severity cvssToSonarQubeSeverity(Float cvssScore, Float blocker, Float critical, Float major, Float minor) {
        if (blocker.floatValue() >= 0 && cvssScore.floatValue() >= blocker.doubleValue()) {
            return Severity.BLOCKER;
        } else if (critical.floatValue() >= 0 && cvssScore.floatValue() >= critical.floatValue()) {
            return Severity.CRITICAL;
        } else if (major.floatValue() >= 0 && cvssScore.floatValue() >= major.floatValue()) {
            return Severity.MAJOR;
        } else if (minor.floatValue() >= 0 && cvssScore.floatValue() >= minor.floatValue()) {
            return Severity.MINOR;
        } else {
            return Severity.INFO;
        }
    }

    public static Severity cvssToSonarQubeSeverity(Float cvssScore, Configuration config) {
        Float severityBlocker = config.getFloat(DependencyCheckConstants.SEVERITY_BLOCKER).orElse(DependencyCheckConstants.SEVERITY_BLOCKER_DEFAULT);
        Float severityCritical = config.getFloat(DependencyCheckConstants.SEVERITY_CRITICAL).orElse(DependencyCheckConstants.SEVERITY_CRITICAL_DEFAULT);
        Float severityMajor = config.getFloat(DependencyCheckConstants.SEVERITY_MAJOR).orElse(DependencyCheckConstants.SEVERITY_MAJOR_DEFAULT);
        Float severityMinor = config.getFloat(DependencyCheckConstants.SEVERITY_MINOR).orElse(DependencyCheckConstants.SEVERITY_MINOR_DEFAULT);
        return DependencyCheckUtils.cvssToSonarQubeSeverity(cvssScore, severityBlocker ,severityCritical, severityMajor, severityMinor);
    }

    /**
     * We are using following sources for score calculation
     * https://nvd.nist.gov/vuln-metrics/cvss
     * https://docs.npmjs.com/about-audit-reports#severity
     * @param severity
     * @param blocker
     * @param critical
     * @param major
     * @param minor
     * @return score based on severity
     */
    public static Float severityToScore(String severity, Float blocker, Float critical, Float major, Float minor) {
        if (blocker >= 0 && StringUtils.equalsAnyIgnoreCase(severity, CRITICAL)) {
            return blocker;
        } else if (critical >= 0 && StringUtils.equalsAnyIgnoreCase(severity, CRITICAL, HIGH)) {
            return critical;
        } else if (major >= 0 && StringUtils.equalsAnyIgnoreCase(severity, CRITICAL, HIGH, MEDIUM, MODERATE)) {
            return major;
        } else if (minor >= 0 && StringUtils.equalsAnyIgnoreCase(severity, CRITICAL, HIGH, MEDIUM, MODERATE, LOW)) {
            return minor;
        } else {
            return 0.0f;
        }
    }

    public static Float severityToScore(String severity, Configuration config) {
        Float severityBlocker = config.getFloat(DependencyCheckConstants.SEVERITY_BLOCKER).orElse(DependencyCheckConstants.SEVERITY_BLOCKER_DEFAULT);
        Float severityCritical = config.getFloat(DependencyCheckConstants.SEVERITY_CRITICAL).orElse(DependencyCheckConstants.SEVERITY_CRITICAL_DEFAULT);
        Float severityMajor = config.getFloat(DependencyCheckConstants.SEVERITY_MAJOR).orElse(DependencyCheckConstants.SEVERITY_MAJOR_DEFAULT);
        Float severityMinor = config.getFloat(DependencyCheckConstants.SEVERITY_MINOR).orElse(DependencyCheckConstants.SEVERITY_MINOR_DEFAULT);
        return DependencyCheckUtils.severityToScore(severity, severityBlocker ,severityCritical, severityMajor, severityMinor);
    }

    public static Optional<Identifier> getMavenIdentifier (@NonNull Dependency dependency){
        for (Identifier identifier : dependency.getIdentifiersCollected()) {
            if ("maven".equals(identifier.getType())) {
                return Optional.of(identifier);
            }
        }
        return Optional.empty();
    }

    /**
     * TODO: Add Markdown formatting if and when Sonar supports it
     * https://jira.sonarsource.com/browse/SONAR-4161
     */
    public static String formatDescription(Dependency dependency, Vulnerability vulnerability) {
        StringBuilder sb = new StringBuilder();
        sb.append("Filename: ").append(dependency.getFileName()).append(" | ");
        sb.append("Reference: ").append(vulnerability.getName()).append(" | ");
        sb.append("CVSS Score: ").append(vulnerability.getCvssScore()).append(" | ");
        Optional<String> vulnerabilityCwe = vulnerability.getCwe();
        if (vulnerabilityCwe.isPresent()) {
            sb.append("Category: ").append(vulnerabilityCwe.get()).append(" | ");
        }
        sb.append(vulnerability.getDescription());
        return sb.toString().trim();
    }

    public static String formatDescription(Dependency dependency, Collection<Vulnerability> vulnerabilities, Vulnerability highestVulnerability) {
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
}
