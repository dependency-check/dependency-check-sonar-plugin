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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Evidence;
import org.sonar.dependencycheck.parser.element.Identifier;
import org.sonar.dependencycheck.parser.element.Vulnerability;

public class ReportParserTest {

    @Test
    public void parseReport() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("report/dependency-check-report.xml");
        Analysis analysis = ReportParser.parse(inputStream);
        assertEquals("1.2.3", analysis.getScanInfo().getEngineVersion());
        assertEquals("trunk-scan", analysis.getProjectInfo().get().getName());
        assertEquals("2014-12-02T04:57:02.663+0200", analysis.getProjectInfo().get().getReportDate());
        assertEquals("This report contains data retrieved from the National Vulnerability Database: http://nvd.nist.gov", analysis.getProjectInfo().get().getCredits());

        // axis-1.4.jar
        Collection<Dependency> dependencies = analysis.getDependencies();
        assertEquals(5, dependencies.size());
        Iterator<Dependency> iterator = dependencies.iterator();
        Dependency dependency = (Dependency) iterator.next();

        assertEquals("axis-1.4.jar", dependency.getFileName());
        assertEquals("/path/to/trunk/lib/axis-1.4.jar", dependency.getFilePath());
        assertEquals("F5D153ADF794D67AF135BD281B7B0516", dependency.getMd5Hash());
        assertEquals("B58604D52D0BF0A9D7C85434D8B770534BAF70B6", dependency.getSha1Hash());

        Collection<Evidence> evidenceCollected = dependency.getEvidenceCollected();
        assertEquals(4, evidenceCollected.size());
        for (Evidence evidence : evidenceCollected) {
            assertFalse(evidence.getSource().isEmpty());
            assertFalse(evidence.getName().isEmpty());
            assertFalse(evidence.getValue().isEmpty());
        }

        Collection<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
        assertEquals(2, vulnerabilities.size());
        Iterator<Vulnerability> vulnIterator = vulnerabilities.iterator();
        Vulnerability vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2014-3596", vulnerability.getName());
        assertEquals(5.8f, vulnerability.getCvssScore(), 0.0f);
        assertEquals("Medium", vulnerability.getSeverity());
        assertFalse(vulnerability.getCwe().isPresent());
        assertEquals("The getCN function in Apache Axis 1.4 and earlier does not properly verify that the server hostname matches a domain name in the subject's Common Name (CN) or subjectAltName field of the X.509 certificate, which allows man-in-the-middle attackers to spoof SSL servers via a certificate with a subject that specifies a common name in a field that is not the CN field.  NOTE: this issue exists because of an incomplete fix for CVE-2012-5784.", vulnerability.getDescription());

        vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2012-5784", vulnerability.getName());
        assertEquals(5.8f , vulnerability.getCvssScore(), 0.0f);
        assertEquals("Medium", vulnerability.getSeverity());
        assertEquals("CWE-20 Improper Input Validation", vulnerability.getCwe().get());
        assertEquals("Apache Axis 1.4 and earlier, as used in PayPal Payments Pro, PayPal Mass Pay, PayPal Transactional Information SOAP, the Java Message Service implementation in Apache ActiveMQ, and other products, does not verify that the server hostname matches a domain name in the subject's Common Name (CN) or subjectAltName field of the X.509 certificate, which allows man-in-the-middle attackers to spoof SSL servers via an arbitrary valid certificate.", vulnerability.getDescription());

        // commons-cli-1.1.jar
        dependency = (Dependency) iterator.next();
        assertEquals(13, dependency.getEvidenceCollected().size());
        assertEquals(0, dependency.getVulnerabilities().size());

        // commons-codec-1.3.jar
        dependency = (Dependency) iterator.next();
        assertEquals(12, dependency.getEvidenceCollected().size());
        assertTrue(dependency.getVulnerabilities().isEmpty());

        // mail-1.4.5.jar
        dependency = (Dependency) iterator.next();
        assertEquals(32, dependency.getEvidenceCollected().size());
        assertEquals(1, dependency.getVulnerabilities().size());
        assertEquals(1, dependency.getIdentifiersCollected().size());
        Collection<Identifier> identifiers = dependency.getIdentifiersCollected();
        Identifier identifier = identifiers.iterator().next();
        assertEquals(Confidence.LOW, identifier.getConfidence().get());
        assertEquals("(javax.mail:mail:1.4.5)", identifier.getName());
        assertEquals("maven", identifier.getType());
        vulnerabilities = dependency.getVulnerabilities();
        assertEquals(1, vulnerabilities.size());
        vulnIterator = vulnerabilities.iterator();
        vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2007-6059", vulnerability.getName());
        assertEquals(0.0f, vulnerability.getCvssScore(), 0.0f);

        // mysql-connector-java-commercial-5.1.25.jar
        dependency = (Dependency) iterator.next();
        assertEquals(9, dependency.getEvidenceCollected().size());
        assertTrue(dependency.getVulnerabilities().isEmpty());

    }

}
