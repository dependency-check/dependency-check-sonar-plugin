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

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.config.Configuration;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Identifier;
import org.sonar.dependencycheck.parser.element.Vulnerability;
import org.sonar.dependencycheck.reason.DependencyReason;
import org.sonar.dependencycheck.reason.Language;
import org.sonar.dependencycheck.reason.SoftwareDependency;
import org.sonar.dependencycheck.reason.maven.MavenDependency;
import org.sonar.dependencycheck.reason.npm.NPMDependency;

import edu.umd.cs.findbugs.annotations.NonNull;

public final class DependencyCheckUtils {

    private static final String CRITICAL = "critical";
    private static final String HIGH = "high";
    private static final String MEDIUM = "medium";
    private static final String MODERATE = "moderate";
    private static final String LOW = "low";

    private DependencyCheckUtils() {
    }

    public static Severity cvssToSonarQubeSeverity(Float cvssScore, Float high, Float medium) {
        if (high >= 0 && cvssScore >= high) {
            return Severity.HIGH;
        } else if (medium >= 0 && cvssScore >= medium) {
            return Severity.MEDIUM;
        } else {
            return Severity.LOW;
        }
    }

    public static Severity cvssToSonarQubeSeverity(Float cvssScore, Configuration config) {
        Float severityHigh = config.getFloat(DependencyCheckConstants.SEVERITY_HIGH).orElse(DependencyCheckConstants.SEVERITY_HIGH_DEFAULT);
        Float severityMedium = config.getFloat(DependencyCheckConstants.SEVERITY_MEDIUM).orElse(DependencyCheckConstants.SEVERITY_MEDIUM_DEFAULT);
        return DependencyCheckUtils.cvssToSonarQubeSeverity(cvssScore, severityHigh, severityMedium);
    }

    public static String getRuleKey(Configuration config) {
        return config.getBoolean(DependencyCheckConstants.SECURITY_HOTSPOT)
                .orElse(DependencyCheckConstants.SECURITY_HOTSPOT_DEFAULT)
                        ? DependencyCheckConstants.RULE_KEY_WITH_SECURITY_HOTSPOT
                        : DependencyCheckConstants.RULE_KEY;
    }

    /**
     * We are using following sources for score calculation
     * https://nvd.nist.gov/vuln-metrics/cvss
     * https://docs.npmjs.com/about-audit-reports#severity
     *
     * @param severity
     * @param critical
     * @param high
     * @param medium
     * @param low
     * @return score based on severity
     */
    public static Float severityToCVSSScore(String severity, Float critical, Float high, Float medium, Float low) {
        if (critical >= 0 && StringUtils.equalsAnyIgnoreCase(severity, CRITICAL)) {
            return critical;
        } else if (high >= 0 && StringUtils.equalsAnyIgnoreCase(severity, CRITICAL, HIGH)) {
            return high;
        } else if (medium >= 0 && StringUtils.equalsAnyIgnoreCase(severity, CRITICAL, HIGH, MEDIUM, MODERATE)) {
            return medium;
        } else if (low >= 0 && StringUtils.equalsAnyIgnoreCase(severity, CRITICAL, HIGH, MEDIUM, MODERATE, LOW)) {
            return low;
        } else {
            return 0.0f;
        }
    }

    public static Float severityToCVSSScore(String severity) {
        return DependencyCheckUtils.severityToCVSSScore(severity, DependencyCheckConstants.CVSS_CRITICAL_SCORE,
            DependencyCheckConstants.CVSS_HIGH_SCORE, DependencyCheckConstants.CVSS_MEDIUM_SCORE,
            DependencyCheckConstants.CVSS_LOW_SCORE);
    }

    public static Optional<MavenDependency> getMavenDependency(@NonNull Dependency dependency) {
        Optional<Collection<Identifier>> packages = dependency.getPackages();
        if (packages.isPresent()) {
            for (Identifier identifier : packages.get()) {
                Optional<SoftwareDependency> softwareDependency = DependencyCheckUtils.convertToSoftwareDependency(identifier.getId());
                if (softwareDependency.isPresent() && softwareDependency.get() instanceof MavenDependency) {
                    return Optional.of((MavenDependency) softwareDependency.get());
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<NPMDependency> getNPMDependency(@NonNull Dependency dependency) {
        Optional<Collection<Identifier>> packages = dependency.getPackages();
        if (packages.isPresent()) {
            for (Identifier identifier : packages.get()) {
                Optional<SoftwareDependency> softwareDependency = DependencyCheckUtils.convertToSoftwareDependency(identifier.getId());
                if (softwareDependency.isPresent() && softwareDependency.get() instanceof NPMDependency) {
                    return Optional.of((NPMDependency) softwareDependency.get());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * TODO: Add Markdown formatting if and when Sonar supports it
     * https://jira.sonarsource.com/browse/SONAR-4161
     */
    public static String formatDescription(Dependency dependency, Vulnerability vulnerability, Configuration config) {
        StringBuilder sb = new StringBuilder();
        if (config.getBoolean(DependencyCheckConstants.USE_FILEPATH)
                .orElse(DependencyCheckConstants.USE_FILEPATH_DEFAULT)) {
            sb.append("Filepath: ").append(dependency.getFilePath()).append(" | ");
        } else {
            sb.append("Filename: ").append(dependency.getFileName()).append(" | ");
        }
        sb.append("Reference: ").append(vulnerability.getName()).append(" | ");
        sb.append("CVSS Score: ").append(vulnerability.getCvssScore()).append(" | ");
        Optional<String[]> vulnerabilityCwe = vulnerability.getCwes();
        if (vulnerabilityCwe.isPresent()) {
            sb.append("Category: ").append(String.join(",", vulnerabilityCwe.get())).append(" | ");
        }
        sb.append(vulnerability.getDescription());
        return sb.toString().trim();
    }

    public static String formatDescription(Dependency dependency, Collection<Vulnerability> vulnerabilities, Vulnerability highestVulnerability, Configuration config) {
        StringBuilder sb = new StringBuilder();
        if (config.getBoolean(DependencyCheckConstants.USE_FILEPATH)
                .orElse(DependencyCheckConstants.USE_FILEPATH_DEFAULT)) {
            sb.append("Filepath: ").append(dependency.getFilePath()).append(" | ");
        } else {
            sb.append("Filename: ").append(dependency.getFileName()).append(" | ");
        }
        sb.append("Highest CVSS Score: ").append(highestVulnerability.getCvssScore()).append(" | ");
        sb.append("Amount of CVSS: ").append(vulnerabilities.size()).append(" | ");
        sb.append("References: ");
        for (Vulnerability vulnerability : vulnerabilities) {
            sb.append(vulnerability.getName()).append(" (").append(vulnerability.getCvssScore()).append(") ");
        }
        return sb.toString().trim();
    }

    public static boolean skipPlugin(Configuration config) {
        return config.getBoolean(DependencyCheckConstants.SKIP_PROPERTY).orElse(DependencyCheckConstants.SKIP_PROPERTY_DEFAULT);
    }

    public static boolean summarizeVulnerabilities(Configuration config) {
        return config.getBoolean(DependencyCheckConstants.SUMMARIZE_PROPERTY).orElse(DependencyCheckConstants.SUMMARIZE_PROPERTY_DEFAULT);
    }

    public static Optional<DependencyReason> getBestDependencyReason(@NonNull Dependency dependency, @NonNull Collection<DependencyReason> dependencyReasons) {

        Comparator<DependencyReason> comparatorTextRange = Comparator.comparing(r -> r.getBestTextRange(dependency));
        // Shorter Files-Names indicates to be a root configuration file
        Comparator<DependencyReason> comparatorFileLength = Comparator.comparingInt(r -> r.getInputComponent().toString().length());

        if (dependency.isJavaDependency() && dependencyReasons.stream().filter(c -> c.getLanguage().equals(Language.JAVA)).count() > 0) {
            // If a Maven Identifier is present we prefer Java dependency reasons
            return dependencyReasons.stream().filter(c -> c.getLanguage().equals(Language.JAVA)).sorted(comparatorFileLength).sorted(comparatorTextRange).findFirst();
        }
        if (dependency.isJavaScriptDependency() && dependencyReasons.stream().filter(c -> c.getLanguage().equals(Language.JAVASCRIPT)).count() > 0) {
            // If a NPM or JavaScript Identifier is present we prefer JavaScript dependency
            // reasons
            return dependencyReasons.stream().filter(c -> c.getLanguage().equals(Language.JAVASCRIPT)).sorted(comparatorFileLength).sorted(comparatorTextRange).findFirst();
        }
        return dependencyReasons.stream().sorted(comparatorFileLength).sorted(comparatorTextRange).findFirst();
    }

    /**
     *
     * @param reference
     * @return
     */
    public static Optional<SoftwareDependency> convertToSoftwareDependency(@NonNull String reference) {
        if (StringUtils.isNotBlank(reference)) {
            if (reference.contains("maven")) {
                return convertToMavenDependency(reference);
            } else if (reference.contains("npm") || reference.contains("javascript")) {
                return convertToNPMDependency(reference);
            }
        }
        return Optional.empty();
    }

    private static Optional<SoftwareDependency> convertToMavenDependency(@NonNull String reference) {
        // pkg:maven/struts/struts@1.2.8 -> struts/struts@1.2.8
        String dependency = StringUtils.substringAfter(reference, "/");
        String groupId = StringUtils.substringBefore(dependency, "/");
        String artifactId = StringUtils.substringBetween(dependency, "/", "@");
        if (StringUtils.isAnyBlank(groupId, artifactId)) {
            return Optional.empty();
        }
        String version = StringUtils.substringAfter(dependency, "@");
        return Optional.of(new MavenDependency(groupId, artifactId, StringUtils.isBlank(version) ? null : version));
    }

    private static Optional<SoftwareDependency> convertToNPMDependency(@NonNull String reference) {
        // pkg:npm/arr-flatten@1.1.0 -> arr-flatten@1.1.0
        // pkg:npm/mime -> mime
        String dependency = StringUtils.substringAfter(reference, "/");
        String name = StringUtils.substringBefore(dependency, "@");
        if (StringUtils.isBlank(name)) {
            return Optional.empty();
        }
        String version = StringUtils.substringAfter(dependency, "@");
        return Optional.of(new NPMDependency(name, StringUtils.isBlank(version) ? null : version));
    }

    public static boolean isMavenDependency(@NonNull SoftwareDependency dep) {
        return dep instanceof MavenDependency;
    }

    public static boolean isNPMDependency(@NonNull SoftwareDependency dep) {
        return dep instanceof NPMDependency;
    }
}
