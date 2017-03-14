/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2017 Steve Springett
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.dependencycheck.parser;

import org.junit.Test;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Evidence;
import org.sonar.dependencycheck.parser.element.Vulnerability;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import static org.fest.assertions.Assertions.assertThat;

public class ReportParserTest {

    @Test
    public void parseReport() throws Exception {
        ReportParser parser = new ReportParser();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("report/dependency-check-report.xml");
        Analysis analysis = parser.parse(inputStream);
        assertThat(analysis.getScanInfo().getEngineVersion()).isEqualTo("1.2.3");
        assertThat(analysis.getProjectInfo().getName()).isEqualTo("trunk-scan");
        assertThat(analysis.getProjectInfo().getReportDate()).isEqualTo("2014-12-02T04:57:02.663+0200");
        assertThat(analysis.getProjectInfo().getCredits()).isEqualTo("This report contains data retrieved from the National Vulnerability Database: http://nvd.nist.gov");

        Collection<Dependency> dependencies = analysis.getDependencies();
        assertThat(dependencies.size()).isEqualTo(5);
        Iterator iterator = dependencies.iterator();
        Dependency dependency = (Dependency) iterator.next();

        assertThat(dependency.getFileName()).isEqualTo("axis-1.4.jar");
        assertThat(dependency.getFilePath()).isEqualTo("/path/to/trunk/lib/axis-1.4.jar");
        assertThat(dependency.getMd5Hash()).isEqualTo("F5D153ADF794D67AF135BD281B7B0516");
        assertThat(dependency.getSha1Hash()).isEqualTo("B58604D52D0BF0A9D7C85434D8B770534BAF70B6");

        Collection<Evidence> evidenceCollected = dependency.getEvidenceCollected();
        assertThat(evidenceCollected.size()).isEqualTo(4);
        for (Evidence evidence : evidenceCollected) {
            assertThat(evidence.getSource()).isNotEmpty();
            assertThat(evidence.getName()).isNotEmpty();
            assertThat(evidence.getValue()).isNotEmpty();
        }

        Collection<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
        assertThat(vulnerabilities.size()).isEqualTo(2);
        Iterator vulnIterator = vulnerabilities.iterator();
        Vulnerability vulnerability = (Vulnerability) vulnIterator.next();
        assertThat(vulnerability.getName()).isEqualTo("CVE-2014-3596");
        assertThat(vulnerability.getCvssScore()).isEqualTo("5.8");
        assertThat(vulnerability.getSeverity()).isEqualTo("Medium");
        assertThat(vulnerability.getCwe() == null);
        assertThat(vulnerability.getDescription()).isEqualTo("The getCN function in Apache Axis 1.4 and earlier does not properly verify that the server hostname matches a domain name in the subject's Common Name (CN) or subjectAltName field of the X.509 certificate, which allows man-in-the-middle attackers to spoof SSL servers via a certificate with a subject that specifies a common name in a field that is not the CN field.  NOTE: this issue exists because of an incomplete fix for CVE-2012-5784.");

        vulnerability = (Vulnerability) vulnIterator.next();
        assertThat(vulnerability.getName()).isEqualTo("CVE-2012-5784");
        assertThat(vulnerability.getCvssScore()).isEqualTo("5.8");
        assertThat(vulnerability.getSeverity()).isEqualTo("Medium");
        assertThat(vulnerability.getCwe().equals("CWE-20 Improper Input Validation"));
        assertThat(vulnerability.getDescription()).isEqualTo("Apache Axis 1.4 and earlier, as used in PayPal Payments Pro, PayPal Mass Pay, PayPal Transactional Information SOAP, the Java Message Service implementation in Apache ActiveMQ, and other products, does not verify that the server hostname matches a domain name in the subject's Common Name (CN) or subjectAltName field of the X.509 certificate, which allows man-in-the-middle attackers to spoof SSL servers via an arbitrary valid certificate.");

        dependency = (Dependency) iterator.next();
        assertThat(dependency.getEvidenceCollected().size()).isEqualTo(13);
        assertThat(dependency.getVulnerabilities().size()).isEqualTo(0);

        dependency = (Dependency) iterator.next();
        assertThat(dependency.getEvidenceCollected().size()).isEqualTo(12);
        assertThat(dependency.getVulnerabilities().size()).isEqualTo(0);

        dependency = (Dependency) iterator.next();
        assertThat(dependency.getEvidenceCollected().size()).isEqualTo(32);
        assertThat(dependency.getVulnerabilities().size()).isEqualTo(1);

        dependency = (Dependency) iterator.next();
        assertThat(dependency.getEvidenceCollected().size()).isEqualTo(9);
        assertThat(dependency.getVulnerabilities().size()).isEqualTo(0);
    }

}
