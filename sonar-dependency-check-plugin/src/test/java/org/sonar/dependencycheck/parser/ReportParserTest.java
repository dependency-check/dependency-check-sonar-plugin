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
package org.sonar.dependencycheck.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Evidence;
import org.sonar.dependencycheck.parser.element.Identifier;
import org.sonar.dependencycheck.parser.element.Vulnerability;

abstract class ReportParserTest {

    public void checkAnalyse(Analysis analysis) {
        assertEquals("5.2.0", analysis.getScanInfo().getEngineVersion());
        assertEquals("Multi-Module Maven Example", analysis.getProjectInfo().get().getName());
        assertEquals("2019-07-26T12:37:05.863Z", analysis.getProjectInfo().get().getReportDate());

        // struts-1.2.8.jar
        Collection<Dependency> dependencies = analysis.getDependencies();
        assertEquals(34, dependencies.size());
        Iterator<Dependency> iterator = dependencies.iterator();
        Dependency dependency = iterator.next();

        assertEquals("struts-1.2.8.jar", dependency.getFileName());
        assertEquals("/to/path/struts/struts/1.2.8/struts-1.2.8.jar", dependency.getFilePath());
        assertEquals("8af31c3a406cfbfd991a6946102d583a", dependency.getMd5Hash());
        assertEquals("5919caff42c3f42fb251fd82a58af4a7880826dd", dependency.getSha1Hash());

        Map<String, List<Evidence>> evidenceCollected = dependency.getEvidenceCollected();
        assertEquals(3, evidenceCollected.size());
        List<Evidence> vendorEvidences = evidenceCollected.get("vendorEvidence");
        assertEquals(14, vendorEvidences.size());
        for (Evidence evidence : vendorEvidences) {
            assertFalse(evidence.getSource().isEmpty());
            assertFalse(evidence.getName().isEmpty());
            assertFalse(evidence.getValue().isEmpty());
        }
        List<Evidence> productEvidences = evidenceCollected.get("productEvidence");
        assertEquals(13, productEvidences.size());
        for (Evidence evidence : productEvidences) {
            assertFalse(evidence.getSource().isEmpty());
            assertFalse(evidence.getName().isEmpty());
            assertFalse(evidence.getValue().isEmpty());
        }
        List<Evidence> versionEvidences = evidenceCollected.get("versionEvidence");
        assertEquals(3, versionEvidences.size());
        for (Evidence evidence : versionEvidences) {
            assertFalse(evidence.getSource().isEmpty());
            assertFalse(evidence.getName().isEmpty());
            assertFalse(evidence.getValue().isEmpty());
        }

        Collection<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
        assertEquals(25, vulnerabilities.size());
        Iterator<Vulnerability> vulnIterator = vulnerabilities.iterator();
        Vulnerability vulnerability = vulnIterator.next();
        assertEquals("CVE-2006-1546", vulnerability.getName());
        assertEquals("NVD", vulnerability.getSource());
        assertEquals(7.5f, vulnerability.getCvssScore(null), 0.0f);
        assertEquals("HIGH", vulnerability.getSeverity());
        assertTrue(vulnerability.getCwes().isPresent());
        assertEquals("NVD-CWE-Other", vulnerability.getCwes().get()[0]);
        assertEquals(1, vulnerability.getCwes().get().length);
        assertEquals(
            "Apache Software Foundation (ASF) Struts before 1.2.9 allows remote attackers to bypass validation via a request with a 'org.apache.struts.taglib.html.Constants.CANCEL' parameter, which causes the action to be canceled but would not be detected from applications that do not use the isCancelled check.",
            vulnerability.getDescription());

        vulnerability = vulnIterator.next();
        assertEquals("CVE-2006-1547", vulnerability.getName());
        assertEquals("NVD", vulnerability.getSource());
        assertEquals(7.8f, vulnerability.getCvssScore(null), 0.0f);
        assertEquals("HIGH", vulnerability.getSeverity());
        assertTrue(vulnerability.getCwes().isPresent());
        assertEquals("NVD-CWE-Other", vulnerability.getCwes().get()[0]);
        assertEquals(1, vulnerability.getCwes().get().length);
        assertEquals(
            "ActionForm in Apache Software Foundation (ASF) Struts before 1.2.9 with BeanUtils 1.7 allows remote attackers to cause a denial of service via a multipart/form-data encoded form with a parameter name that references the public getMultipartRequestHandler method, which provides further access to elements in the CommonsMultipartRequestHandler implementation and BeanUtils.",
            vulnerability.getDescription());

        // commons-beanutils-1.7.0.jar
        dependency = iterator.next();
        assertEquals(3, dependency.getEvidenceCollected().size());
        assertEquals(9, dependency.getEvidenceCollected().get("vendorEvidence").size());
        assertEquals(9, dependency.getEvidenceCollected().get("productEvidence").size());
        assertEquals(2, dependency.getEvidenceCollected().get("versionEvidence").size());
        assertEquals(1, dependency.getVulnerabilities().size());

        // commons-digester-1.6.jar
        dependency = iterator.next();
        assertEquals(3, dependency.getEvidenceCollected().size());
        assertEquals(9, dependency.getEvidenceCollected().get("vendorEvidence").size());
        assertEquals(9, dependency.getEvidenceCollected().get("productEvidence").size());
        assertEquals(2, dependency.getEvidenceCollected().get("versionEvidence").size());
        assertTrue(dependency.getVulnerabilities().isEmpty());

        // commons-collections-2.1.jar
        dependency = iterator.next();
        assertEquals("commons-collections-2.1.jar", dependency.getFileName());
        assertEquals(3, dependency.getEvidenceCollected().size());
        assertEquals(10, dependency.getEvidenceCollected().get("vendorEvidence").size());
        assertEquals(8, dependency.getEvidenceCollected().get("productEvidence").size());
        assertEquals(3, dependency.getEvidenceCollected().get("versionEvidence").size());
        assertEquals(2, dependency.getVulnerabilities().size());
        assertEquals(1, dependency.getPackages().size());
        assertEquals(1, dependency.getVulnerabilityIds().size());
        assertEquals(1, dependency.getVulnerabilityIds().size());
        Collection<Identifier> identifiers = dependency.getPackages();
        Identifier identifier = identifiers.iterator().next();
        assertEquals(Confidence.HIGH, identifier.getConfidence().get());
        assertEquals("pkg:maven/commons-collections/commons-collections@2.1", identifier.getId());
        vulnerabilities = dependency.getVulnerabilities();
        assertEquals(2, vulnerabilities.size());
        vulnIterator = vulnerabilities.iterator();
        vulnerability = vulnIterator.next();
        assertEquals("CVE-2015-6420", vulnerability.getName());
        assertEquals("NVD", vulnerability.getSource());
        assertEquals(7.5f, vulnerability.getCvssScore(null), 0.0f);

        // xml-apis-1.0.b2.jar
        dependency = iterator.next();
        assertEquals("xml-apis-1.0.b2.jar", dependency.getFileName());
        assertEquals(3, evidenceCollected.size());
        evidenceCollected = dependency.getEvidenceCollected();
        vendorEvidences = evidenceCollected.get("vendorEvidence");
        assertEquals(18, vendorEvidences.size());
        productEvidences = evidenceCollected.get("productEvidence");
        assertEquals(26, productEvidences.size());
        versionEvidences = evidenceCollected.get("versionEvidence");
        assertEquals(3, versionEvidences.size());
        assertTrue(dependency.getVulnerabilities().isEmpty());

    }
}
