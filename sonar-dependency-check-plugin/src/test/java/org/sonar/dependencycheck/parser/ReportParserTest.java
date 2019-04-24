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
package org.sonar.dependencycheck.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Evidence;
import org.sonar.dependencycheck.parser.element.Identifier;
import org.sonar.dependencycheck.parser.element.Vulnerability;

public class ReportParserTest {

    @Test
    public void parseReport400() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("report/dependency-check-report-400.xml");
        Analysis analysis = ReportParser.parse(inputStream);
        assertEquals("4.0.2", analysis.getScanInfo().getEngineVersion());
        assertEquals("Multi-Module Maven Example", analysis.getProjectInfo().get().getName());
        assertEquals("2019-04-18T11:30:25.206+0200", analysis.getProjectInfo().get().getReportDate());
        assertEquals("This report contains data retrieved from the National Vulnerability Database: https://nvd.nist.gov, NPM Public Advisories: https://www.npmjs.com/advisories, and the RetireJS community.", analysis.getProjectInfo().get().getCredits());

        // struts-1.2.8.jar
        Collection<Dependency> dependencies = analysis.getDependencies();
        assertEquals(20, dependencies.size());
        Iterator<Dependency> iterator = dependencies.iterator();
        Dependency dependency = (Dependency) iterator.next();

        assertEquals("struts-1.2.8.jar", dependency.getFileName());
        assertEquals("/to/path/struts/struts/1.2.8/struts-1.2.8.jar", dependency.getFilePath());
        assertEquals("8af31c3a406cfbfd991a6946102d583a", dependency.getMd5Hash());
        assertEquals("5919caff42c3f42fb251fd82a58af4a7880826dd", dependency.getSha1Hash());

        Collection<Evidence> evidenceCollected = dependency.getEvidenceCollected();
        assertEquals(26, evidenceCollected.size());
        for (Evidence evidence : evidenceCollected) {
            assertFalse(evidence.getSource().isEmpty());
            assertFalse(evidence.getName().isEmpty());
            assertFalse(evidence.getValue().isEmpty());
        }

        Collection<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
        assertEquals(8, vulnerabilities.size());
        Iterator<Vulnerability> vulnIterator = vulnerabilities.iterator();
        Vulnerability vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2006-1546", vulnerability.getName());
        assertEquals(7.5f, vulnerability.getCvssScore(), 0.0f);
        assertEquals("High", vulnerability.getSeverity());
        assertFalse(vulnerability.getCwe().isPresent());
        assertEquals("Apache Software Foundation (ASF) Struts ...", vulnerability.getDescription());

        vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2006-1547", vulnerability.getName());
        assertEquals(7.8f , vulnerability.getCvssScore(), 0.0f);
        assertEquals("High", vulnerability.getSeverity());
        assertFalse(vulnerability.getCwe().isPresent());
        assertEquals("ActionForm in Apache Software Foundation (ASF) Struts before 1.2.9 ...", vulnerability.getDescription());

        // commons-beanutils-1.7.0.jar
        dependency = (Dependency) iterator.next();
        assertEquals(14, dependency.getEvidenceCollected().size());
        assertEquals(1, dependency.getVulnerabilities().size());

        // commons-digester-1.6.jar
        dependency = (Dependency) iterator.next();
        assertEquals(15, dependency.getEvidenceCollected().size());
        assertTrue(dependency.getVulnerabilities().isEmpty());

        // commons-collections-2.1.jar
        dependency = (Dependency) iterator.next();
        assertEquals(17, dependency.getEvidenceCollected().size());
        assertEquals(2, dependency.getVulnerabilities().size());
        assertEquals(2, dependency.getIdentifiersCollected().size());
        Collection<Identifier> identifiers = dependency.getIdentifiersCollected();
        Identifier identifier = identifiers.iterator().next();
        assertEquals(Confidence.HIGHEST, identifier.getConfidence().get());
        assertEquals("commons-collections:commons-collections:2.1", identifier.getName());
        assertEquals("maven", identifier.getType());
        vulnerabilities = dependency.getVulnerabilities();
        assertEquals(2, vulnerabilities.size());
        vulnIterator = vulnerabilities.iterator();
        vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2015-6420", vulnerability.getName());
        assertEquals(7.5f, vulnerability.getCvssScore(), 0.0f);

        // xml-apis-1.0.b2.jar
        dependency = (Dependency) iterator.next();
        System.out.println(dependency.getFileName());
        assertEquals(32, dependency.getEvidenceCollected().size());
        assertTrue(dependency.getVulnerabilities().isEmpty());

    }
    @Test
    public void parseReport500() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("report/dependency-check-report-500.xml");
        Analysis analysis = ReportParser.parse(inputStream);
        assertEquals("5.0.0-M2", analysis.getScanInfo().getEngineVersion());
        assertEquals("Multi-Module Maven Example", analysis.getProjectInfo().get().getName());
        assertEquals("2019-04-17T18:25:00.460+0200", analysis.getProjectInfo().get().getReportDate());
        assertEquals("This report contains data retrieved from the National Vulnerability Database: https://nvd.nist.gov, NPM Public Advisories: https://www.npmjs.com/advisories, and the RetireJS community.", analysis.getProjectInfo().get().getCredits());

        // struts-1.2.8.jar
        Collection<Dependency> dependencies = analysis.getDependencies();
        assertEquals(34, dependencies.size());
        Iterator<Dependency> iterator = dependencies.iterator();
        Dependency dependency = (Dependency) iterator.next();

        assertEquals("struts-1.2.8.jar", dependency.getFileName());
        assertEquals("/to/path/struts/struts/1.2.8/struts-1.2.8.jar", dependency.getFilePath());
        assertEquals("8af31c3a406cfbfd991a6946102d583a", dependency.getMd5Hash());
        assertEquals("5919caff42c3f42fb251fd82a58af4a7880826dd", dependency.getSha1Hash());

        Collection<Evidence> evidenceCollected = dependency.getEvidenceCollected();
        assertEquals(30, evidenceCollected.size());
        for (Evidence evidence : evidenceCollected) {
            assertFalse(evidence.getSource().isEmpty());
            assertFalse(evidence.getName().isEmpty());
            assertFalse(evidence.getValue().isEmpty());
        }

        Collection<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
        assertEquals(11, vulnerabilities.size());
        Iterator<Vulnerability> vulnIterator = vulnerabilities.iterator();
        Vulnerability vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2006-1546", vulnerability.getName());
        assertEquals(7.5f, vulnerability.getCvssScore(), 0.0f);
        assertEquals("HIGH", vulnerability.getSeverity());
        assertFalse(vulnerability.getCwe().isPresent());
        assertEquals("Apache Software Foundation (ASF) Struts ...", vulnerability.getDescription());

        vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2006-1547", vulnerability.getName());
        assertEquals(7.8f , vulnerability.getCvssScore(), 0.0f);
        assertEquals("HIGH", vulnerability.getSeverity());
        assertFalse(vulnerability.getCwe().isPresent());
        assertEquals("ActionForm in Apache Software Foundation (ASF) Struts before 1.2.9 ...", vulnerability.getDescription());

        // commons-beanutils-1.7.0.jar
        dependency = (Dependency) iterator.next();
        assertEquals(20, dependency.getEvidenceCollected().size());
        assertEquals(1, dependency.getVulnerabilities().size());

        // commons-digester-1.6.jar
        dependency = (Dependency) iterator.next();
        assertEquals(20, dependency.getEvidenceCollected().size());
        assertTrue(dependency.getVulnerabilities().isEmpty());

        // commons-collections-2.1.jar
        dependency = (Dependency) iterator.next();
        assertEquals(21, dependency.getEvidenceCollected().size());
        assertEquals(2, dependency.getVulnerabilities().size());
        assertEquals(2, dependency.getIdentifiersCollected().size());
        Collection<Identifier> identifiers = dependency.getIdentifiersCollected();
        Identifier identifier = identifiers.iterator().next();
        assertEquals(Confidence.HIGH, identifier.getConfidence().get());
        assertEquals("commons-collections:commons-collections:2.1", identifier.getName());
        assertEquals("maven", identifier.getType());
        vulnerabilities = dependency.getVulnerabilities();
        assertEquals(2, vulnerabilities.size());
        vulnIterator = vulnerabilities.iterator();
        vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2015-6420", vulnerability.getName());
        assertEquals(7.5f, vulnerability.getCvssScore(), 0.0f);

        // xml-apis-1.0.b2.jar
        dependency = (Dependency) iterator.next();
        assertEquals(47, dependency.getEvidenceCollected().size());
        assertTrue(dependency.getVulnerabilities().isEmpty());

    }

    @Test
    public void parseReportNode500() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("reportNode.js/dependency-check-report.xml");
        Analysis analysis = ReportParser.parse(inputStream);
        assertEquals("5.0.0-M2", analysis.getScanInfo().getEngineVersion());
        assertEquals("project", analysis.getProjectInfo().get().getName());
        assertEquals("2019-04-23T22:43:06.450+0000", analysis.getProjectInfo().get().getReportDate());
        assertEquals("This report contains data retrieved from the National Vulnerability Database: https://nvd.nist.gov, NPM Public Advisories: https://www.npmjs.com/advisories, and the RetireJS community.", analysis.getProjectInfo().get().getCredits());

    }

}
