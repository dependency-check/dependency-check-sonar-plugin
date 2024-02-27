/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2024 dependency-check
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
package org.sonar.dependencycheck.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.AnalysisException;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Evidence;
import org.sonar.dependencycheck.parser.element.Identifier;
import org.sonar.dependencycheck.parser.element.Vulnerability;

abstract class ReportParserTest {

    public abstract Analysis parseReport(String dir) throws Exception;

    @Test
    void parseReportMultiModuleMavenExample() throws Exception {
        Analysis analysis = parseReport("reportMultiModuleMavenExample");

        assertEquals("8.0.2", analysis.getScanInfo().getEngineVersion());
        assertEquals("Multi-Module Maven Example", analysis.getProjectInfo().get().getName());
        assertEquals("2023-02-02T15:59:49.067Z", analysis.getProjectInfo().get().getReportDate());

        Collection<Dependency> dependencies = analysis.getDependencies();
        assertEquals(34, dependencies.size());

        // struts-1.2.8.jar
        Dependency dependency = findDependency(dependencies, "struts-1.2.8.jar");
        assertNotNull(dependency);
        assertEquals("/to/path/struts/struts/1.2.8/struts-1.2.8.jar", dependency.getFilePath());
        assertEquals("8af31c3a406cfbfd991a6946102d583a", dependency.getMd5Hash().get());
        assertEquals("5919caff42c3f42fb251fd82a58af4a7880826dd", dependency.getSha1Hash().get());

        checkEvidence(dependency.getEvidenceCollected(), 63, 61, 3);
        Collection<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
        assertEquals(8, vulnerabilities.size());
        Iterator<Vulnerability> vulnIterator = vulnerabilities.iterator();
        Vulnerability vulnerability = vulnIterator.next();
        assertEquals("CVE-2016-1182", vulnerability.getName());
        assertEquals("NVD", vulnerability.getSource());
        assertEquals(8.2f, vulnerability.getCvssScore(), 0.0f);
        assertEquals("HIGH", vulnerability.getSeverity());
        assertTrue(vulnerability.getCwes().isPresent());
        assertEquals("CWE-20", vulnerability.getCwes().get()[0]);
        assertEquals(1, vulnerability.getCwes().get().length);
        assertEquals(
            "ActionServlet.java in Apache Struts 1 1.x through 1.3.10 does not properly restrict the Validator configuration, which allows remote attackers to conduct cross-site scripting (XSS) attacks or cause a denial of service via crafted input, a related issue to CVE-2015-0899.",
            vulnerability.getDescription());

        vulnerability = vulnIterator.next();
        assertEquals("CVE-2016-1181", vulnerability.getName());
        assertEquals("NVD", vulnerability.getSource());
        assertEquals(8.1f, vulnerability.getCvssScore(), 0.0f);
        assertEquals("HIGH", vulnerability.getSeverity());
        assertTrue(vulnerability.getCwes().isPresent());
        assertEquals("NVD-CWE-noinfo", vulnerability.getCwes().get()[0]);
        assertEquals(1, vulnerability.getCwes().get().length);
        assertEquals(
            "ActionServlet.java in Apache Struts 1 1.x through 1.3.10 mishandles multithreaded access to an ActionForm instance, which allows remote attackers to execute arbitrary code or cause a denial of service (unexpected memory access) via a multipart request, a related issue to CVE-2015-0899.",
            vulnerability.getDescription());

        // commons-beanutils-1.7.0.jar
        dependency = findDependency(dependencies, "commons-beanutils-1.7.0.jar");
        assertNotNull(dependency);
        checkEvidence(dependency.getEvidenceCollected(), 10, 9, 2);
        assertEquals(2, dependency.getVulnerabilities().size());

        // commons-digester-1.6.jar
        dependency = findDependency(dependencies, "commons-digester-1.6.jar");
        assertNotNull(dependency);
        checkEvidence(dependency.getEvidenceCollected(), 10, 9, 2);
        assertTrue(dependency.getVulnerabilities().isEmpty());

        // commons-collections-2.1.jar
        dependency = findDependency(dependencies, "commons-collections-2.1.jar");
        assertNotNull(dependency);
        checkEvidence(dependency.getEvidenceCollected(), 17, 14, 3);
        assertEquals(1, dependency.getVulnerabilities().size());
        assertEquals(1, dependency.getPackages().get().size());
        assertEquals(1, dependency.getVulnerabilityIds().get().size());
        assertEquals(1, dependency.getVulnerabilityIds().get().size());
        Collection<Identifier> identifiers = dependency.getPackages().get();
        Identifier identifier = identifiers.iterator().next();
        assertEquals(Confidence.HIGH, identifier.getConfidence().get());
        assertEquals("pkg:maven/commons-collections/commons-collections@2.1", identifier.getId());
        vulnerabilities = dependency.getVulnerabilities();
        assertEquals(1, vulnerabilities.size());
        vulnIterator = vulnerabilities.iterator();
        vulnerability = vulnIterator.next();
        assertEquals("CVE-2015-6420", vulnerability.getName());
        assertEquals("NVD", vulnerability.getSource());
        assertEquals(7.5f, vulnerability.getCvssScore(), 0.0f);

        // xml-apis-1.0.b2.jar
        dependency = findDependency(dependencies, "xml-apis-1.0.b2.jar");
        assertNotNull(dependency);
        checkEvidence(dependency.getEvidenceCollected(), 19, 26, 3);
        assertTrue(dependency.getVulnerabilities().isEmpty());
    }

    private void checkEvidence(Map<String, List<Evidence>> evidenceCollected, int vendorEvidence, int productEvidence, int versionEvidence) {
        assertEquals(3, evidenceCollected.size());
        List<Evidence> vendorEvidences = evidenceCollected.get("vendorEvidence");
        assertEquals(vendorEvidence, vendorEvidences.size(), "vendorEvidence doesn't match");
        for (Evidence evidence : vendorEvidences) {
            assertFalse(evidence.getSource().isEmpty());
            assertFalse(evidence.getName().isEmpty());
            assertFalse(evidence.getValue().isEmpty());
        }
        List<Evidence> productEvidences = evidenceCollected.get("productEvidence");
        assertEquals(productEvidence, productEvidences.size(), "productEvidence doesn't match");
        for (Evidence evidence : productEvidences) {
            assertFalse(evidence.getSource().isEmpty());
            assertFalse(evidence.getName().isEmpty());
            assertFalse(evidence.getValue().isEmpty());
        }
        List<Evidence> versionEvidences = evidenceCollected.get("versionEvidence");
        assertEquals(versionEvidence, versionEvidences.size(), "versionEvidence doesn't match");
        for (Evidence evidence : versionEvidences) {
            assertFalse(evidence.getSource().isEmpty());
            assertFalse(evidence.getName().isEmpty());
            assertFalse(evidence.getValue().isEmpty());
        }
    }

    private Dependency findDependency(Collection<Dependency> dependencies, String name) {
        return dependencies.stream().filter(dependency -> name.equals(dependency.getFileName())).findAny().orElse(null);
    }

    @Test
    void parseReportWithExceptions() throws Exception {
        Analysis analysis = parseReport("reportWithExceptions");

        assertEquals("5.2.4", analysis.getScanInfo().getEngineVersion());

        List<AnalysisException> analysisExceptions = analysis.getScanInfo().getExceptions();
        assertEquals(1, analysisExceptions.size());
        AnalysisException analysisException = analysisExceptions.get(0);
        assertEquals("org.owasp.dependencycheck.analyzer.exception.AnalysisException: Failed to request component-reports", analysisException
            .getMessage());
        Throwable cause = analysisException.getCause();
        assertEquals("java.net.SocketTimeoutException: connect timed out", cause.getMessage());
        assertNull(cause.getCause());

        Collection<Dependency> dependencies = analysis.getDependencies();
        assertEquals(2, dependencies.size());
        Iterator<Dependency> iterator = dependencies.iterator();

        Dependency dependency = iterator.next();
        assertEquals("dom4j-1.6.1.jar", dependency.getFileName());
        dependency = iterator.next();
        assertEquals("xml-apis-1.0.b2.jar", dependency.getFileName());
    }
}
