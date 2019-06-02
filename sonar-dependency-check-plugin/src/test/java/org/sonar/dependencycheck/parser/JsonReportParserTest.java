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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Evidence;
import org.sonar.dependencycheck.parser.element.Identifier;
import org.sonar.dependencycheck.parser.element.Vulnerability;

public class JsonReportParserTest {

    @Test
    public void parseReport() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("reportMultiModuleMavenExample/dependency-check-report.json");
        Analysis analysis = JsonReportParser.parse(inputStream);
        assertEquals("5.2.0", analysis.getScanInfo().getEngineVersion());
        assertEquals("Multi-Module Maven Example", analysis.getProjectInfo().get().getName());
        assertEquals("2019-07-26T12:37:05.863Z", analysis.getProjectInfo().get().getReportDate());

        // struts-1.2.8.jar
        Collection<Dependency> dependencies = analysis.getDependencies();
        assertEquals(34, dependencies.size());
        Iterator<Dependency> iterator = dependencies.iterator();
        Dependency dependency = (Dependency) iterator.next();

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
        Vulnerability vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2006-1546", vulnerability.getName());
        assertEquals("NVD", vulnerability.getSource());
        assertEquals(7.5f, vulnerability.getCvssScore(), 0.0f);
        assertEquals("HIGH", vulnerability.getSeverity());
        assertFalse(vulnerability.getCwe().isPresent());
        assertEquals("Apache Software Foundation (ASF) Struts before 1.2.9 allows remote attackers to bypass validation via a request with a 'org.apache.struts.taglib.html.Constants.CANCEL' parameter, which causes the action to be canceled but would not be detected from applications that do not use the isCancelled check.", vulnerability.getDescription());

        vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2006-1547", vulnerability.getName());
        assertEquals("NVD", vulnerability.getSource());
        assertEquals(7.8f , vulnerability.getCvssScore(), 0.0f);
        assertEquals("HIGH", vulnerability.getSeverity());
        assertFalse(vulnerability.getCwe().isPresent());
        assertEquals("ActionForm in Apache Software Foundation (ASF) Struts before 1.2.9 with BeanUtils 1.7 allows remote attackers to cause a denial of service via a multipart/form-data encoded form with a parameter name that references the public getMultipartRequestHandler method, which provides further access to elements in the CommonsMultipartRequestHandler implementation and BeanUtils.", vulnerability.getDescription());

        // commons-beanutils-1.7.0.jar
        dependency = (Dependency) iterator.next();
        assertEquals(3, dependency.getEvidenceCollected().size());
        assertEquals(9, dependency.getEvidenceCollected().get("vendorEvidence").size());
        assertEquals(9, dependency.getEvidenceCollected().get("productEvidence").size());
        assertEquals(2, dependency.getEvidenceCollected().get("versionEvidence").size());
        assertEquals(1, dependency.getVulnerabilities().size());

        // commons-digester-1.6.jar
        dependency = (Dependency) iterator.next();
        assertEquals(3, dependency.getEvidenceCollected().size());
        assertEquals(9, dependency.getEvidenceCollected().get("vendorEvidence").size());
        assertEquals(9, dependency.getEvidenceCollected().get("productEvidence").size());
        assertEquals(2, dependency.getEvidenceCollected().get("versionEvidence").size());
        assertTrue(dependency.getVulnerabilities().isEmpty());

        // commons-collections-2.1.jar
        dependency = (Dependency) iterator.next();
        assertEquals("commons-collections-2.1.jar", dependency.getFileName());
        assertEquals(3, dependency.getEvidenceCollected().size());
        assertEquals(10, dependency.getEvidenceCollected().get("vendorEvidence").size());
        assertEquals(8, dependency.getEvidenceCollected().get("productEvidence").size());
        assertEquals(3, dependency.getEvidenceCollected().get("versionEvidence").size());
        assertEquals(2, dependency.getVulnerabilities().size());
        assertEquals(1, dependency.getPackages().size());
        assertEquals(1, dependency.getVulnerabilityIds().size());
        Collection<Identifier> identifiers = dependency.getPackages();
        Identifier identifier = identifiers.iterator().next();
        assertEquals(Confidence.HIGH, identifier.getConfidence().get());
        assertEquals("pkg:maven/commons-collections/commons-collections@2.1", identifier.getId());
        vulnerabilities = dependency.getVulnerabilities();
        assertEquals(2, vulnerabilities.size());
        vulnIterator = vulnerabilities.iterator();
        vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2015-6420", vulnerability.getName());
        assertEquals("NVD", vulnerability.getSource());
        assertEquals(7.5f, vulnerability.getCvssScore(), 0.0f);

        // xml-apis-1.0.b2.jar
        dependency = (Dependency) iterator.next();
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

    @Test
    public void parseReportNode500() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("reportNode.js/dependency-check-report.xml");
        Analysis analysis = JsonReportParser.parse(inputStream);
        assertEquals("5.0.0-M2", analysis.getScanInfo().getEngineVersion());
        assertEquals("project", analysis.getProjectInfo().get().getName());
        assertEquals("2019-04-23T22:43:06.450+0000", analysis.getProjectInfo().get().getReportDate());

        Collection<Dependency> dependencies = analysis.getDependencies();
        assertEquals(3, dependencies.size());
        Iterator<Dependency> iterator = dependencies.iterator();

        // jquery
        Dependency dependency = (Dependency) iterator.next();

        assertEquals("jquery.js", dependency.getFileName());
        assertEquals("project/node_modules/moment-duration-format/test/vendor/jquery.js", dependency.getFilePath());
        assertEquals("cc41e74189d44a6169f56655e44ef69d", dependency.getMd5Hash());
        assertEquals("e0e77771c69fe4acd4cad20a0f20ce7a5086dc56", dependency.getSha1Hash());

        Map<String, List<Evidence>> evidenceCollected = dependency.getEvidenceCollected();
        int evidenceAmount = 0;
        for (Map.Entry<String, List<Evidence>> evidences : evidenceCollected.entrySet()) {
            evidenceAmount += evidences.getValue().size();
            for (Evidence evidence : evidences.getValue()) {
                assertFalse(evidence.getSource().isEmpty());
                assertFalse(evidence.getName().isEmpty());
                assertFalse(evidence.getValue().isEmpty());
                assertFalse(evidence.getConfidence() == null);
                assertFalse(evidence.getType().isEmpty());
            }
        }
        assertEquals(2, evidenceAmount);

        Collection<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
        assertEquals(2, vulnerabilities.size());
        Iterator<Vulnerability> vulnIterator = vulnerabilities.iterator();
        Vulnerability vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2015-9251", vulnerability.getName());
        assertEquals("NVD", vulnerability.getSource());
        assertEquals(4.3f, vulnerability.getCvssScore(false), 0.0f);
        assertEquals(6.1f, vulnerability.getCvssScore(), 0.0f);
        assertEquals("MEDIUM", vulnerability.getSeverity());
        assertEquals("MEDIUM", vulnerability.getSeverity(false));
        assertFalse(vulnerability.getCwe().isPresent());
        assertEquals("jQuery before 3.0.0 is vulnerable to Cross-site Scripting (XSS) attacks when a cross-domain Ajax request is performed without the dataType option, causing text/javascript responses to be executed.", vulnerability.getDescription());

        vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("CVE-2019-11358", vulnerability.getName());
        assertEquals("NVD", vulnerability.getSource());
        assertEquals(4.3f, vulnerability.getCvssScore(false), 0.0f);
        assertEquals(6.1f , vulnerability.getCvssScore(), 0.0f);
        assertEquals("MEDIUM", vulnerability.getSeverity());
        assertFalse(vulnerability.getCwe().isPresent());
        assertEquals("jQuery before 3.4.0, as used in Drupal, Backdrop CMS, and other products, mishandles jQuery.extend(true, {}, ...) because of Object.prototype pollution. If an unsanitized source object contained an enumerable __proto__ property, it could extend the native Object.prototype.", vulnerability.getDescription());

        // kind-of -> no vulnerability
        dependency = (Dependency) iterator.next();

        assertEquals("index.js", dependency.getFileName());
        assertEquals("project/node_modules/jest-config/node_modules/is-number/node_modules/kind-of/index.js", dependency.getFilePath());
        assertEquals("974e0c2803e83c5bf65de52b98bf4f55", dependency.getMd5Hash());
        assertEquals("f9e8418f23f97452410088786d5e0c7a981ced74", dependency.getSha1Hash());

        evidenceCollected = dependency.getEvidenceCollected();
        assertEquals(0, evidenceCollected.size());

        vulnerabilities = dependency.getVulnerabilities();
        assertEquals(0, vulnerabilities.size());


        // braces -> vulnerability from NPM
        dependency = (Dependency) iterator.next();

        assertEquals("braces:1.8.5", dependency.getFileName());
        assertEquals("project/node_modules/braces/package.json", dependency.getFilePath());
        assertEquals("a5663cea473ad651ac4a00a504883a22", dependency.getMd5Hash());
        assertEquals("8082af62f94236e20503e76f0c94b0cf38bf0824", dependency.getSha1Hash());

        evidenceCollected = dependency.getEvidenceCollected();
        evidenceAmount = 0;
        for (Map.Entry<String, List<Evidence>> evidences : evidenceCollected.entrySet()) {
            evidenceAmount += evidences.getValue().size();
            for (Evidence evidence : evidences.getValue()) {
                assertFalse(evidence.getSource().isEmpty());
                assertFalse(evidence.getName().isEmpty());
                assertFalse(evidence.getValue().isEmpty());
                assertFalse(evidence.getConfidence() == null);
                assertFalse(evidence.getType().isEmpty());
            }
        }
        assertEquals(6, evidenceAmount);
        
        vulnerabilities = dependency.getVulnerabilities();
        assertEquals(1, vulnerabilities.size());
        vulnIterator = vulnerabilities.iterator();
        vulnerability = (Vulnerability) vulnIterator.next();
        assertEquals("786", vulnerability.getName());
        assertEquals("NPM", vulnerability.getSource());
        assertEquals(4.0f, vulnerability.getCvssScore(false), 0.0f);
        assertEquals(4.0f, vulnerability.getCvssScore(), 0.0f);
        assertEquals("MEDIUM", vulnerability.getSeverity());
        assertEquals("MEDIUM", vulnerability.getSeverity(false));
        assertFalse(vulnerability.getCwe().isPresent());
        assertEquals("Versions of `braces` prior to 2.3.1 are vulnerable to Regular Expression Denial of\n" +
                "                        Service (ReDoS). Untrusted input may cause catastrophic backtracking while matching regular\n" +
                "                        expressions. This can cause the application to be unresponsive leading to Denial of Service.", vulnerability.getDescription());
    }

    @Test
    public void parseBigReportNode500() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("reportNode.js/big-dependency-check-report.xml");
        Analysis analysis = JsonReportParser.parse(inputStream);
        assertEquals("5.0.0-M2", analysis.getScanInfo().getEngineVersion());
        assertEquals("project", analysis.getProjectInfo().get().getName());
        assertEquals("2019-04-23T22:43:06.450+0000", analysis.getProjectInfo().get().getReportDate());

        Collection<Dependency> dependencies = analysis.getDependencies();
        assertEquals(10765, dependencies.size());
        
        for (Dependency dependency : dependencies) {
            //check jquery
            if ( "e0e77771c69fe4acd4cad20a0f20ce7a5086dc56".equals(dependency.getSha1Hash())){
                assertEquals("jquery.js", dependency.getFileName());
                assertEquals("project/node_modules/moment-duration-format/test/vendor/jquery.js", dependency.getFilePath());
                assertEquals("cc41e74189d44a6169f56655e44ef69d", dependency.getMd5Hash());
                assertEquals("e0e77771c69fe4acd4cad20a0f20ce7a5086dc56", dependency.getSha1Hash());

                Map<String, List<Evidence>> evidenceCollected = dependency.getEvidenceCollected();
                int evidenceAmount = 0;
                for (Map.Entry<String, List<Evidence>> evidences : evidenceCollected.entrySet()) {
                    evidenceAmount += evidences.getValue().size();
                    for (Evidence evidence : evidences.getValue()) {
                        assertFalse(evidence.getSource().isEmpty());
                        assertFalse(evidence.getName().isEmpty());
                        assertFalse(evidence.getValue().isEmpty());
                        assertFalse(evidence.getConfidence() == null);
                        assertFalse(evidence.getType().isEmpty());
                    }
                }
                assertEquals(2, evidenceAmount);

                Collection<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
                assertEquals(2, vulnerabilities.size());
                Iterator<Vulnerability> vulnIterator = vulnerabilities.iterator();
                Vulnerability vulnerability = (Vulnerability) vulnIterator.next();
                assertEquals("CVE-2015-9251", vulnerability.getName());
                assertEquals("NVD", vulnerability.getSource());
                assertEquals(4.3f, vulnerability.getCvssScore(false), 0.0f);
                assertEquals(6.1f, vulnerability.getCvssScore(), 0.0f);
                assertEquals("MEDIUM", vulnerability.getSeverity());
                assertEquals("MEDIUM", vulnerability.getSeverity(false));
                assertFalse(vulnerability.getCwe().isPresent());
                assertEquals("jQuery before 3.0.0 is vulnerable to Cross-site Scripting (XSS) attacks when a cross-domain Ajax request is performed without the dataType option, causing text/javascript responses to be executed.", vulnerability.getDescription());

                vulnerability = (Vulnerability) vulnIterator.next();
                assertEquals("CVE-2019-11358", vulnerability.getName());
                assertEquals("NVD", vulnerability.getSource());
                assertEquals(4.3f, vulnerability.getCvssScore(false), 0.0f);
                assertEquals(6.1f , vulnerability.getCvssScore(), 0.0f);
                assertEquals("MEDIUM", vulnerability.getSeverity());
                assertFalse(vulnerability.getCwe().isPresent());
                assertEquals("jQuery before 3.4.0, as used in Drupal, Backdrop CMS, and other products, mishandles jQuery.extend(true, {}, ...) because of Object.prototype pollution. If an unsanitized source object contained an enumerable __proto__ property, it could extend the native Object.prototype.", vulnerability.getDescription());
            }
            //check kind-of
            if ( "f9e8418f23f97452410088786d5e0c7a981ced74".equals(dependency.getSha1Hash())){
                assertEquals("index.js", dependency.getFileName());
                assertEquals("project/node_modules/jest-config/node_modules/is-number/node_modules/kind-of/index.js", dependency.getFilePath());
                assertEquals("974e0c2803e83c5bf65de52b98bf4f55", dependency.getMd5Hash());
                assertEquals("f9e8418f23f97452410088786d5e0c7a981ced74", dependency.getSha1Hash());

                Map<String, List<Evidence>> evidenceCollected = dependency.getEvidenceCollected();
                assertEquals(0, evidenceCollected.size());

                Collection<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
                assertEquals(0, vulnerabilities.size());
            }
            //check brace:1.8.5 => NPM
            if ("8082af62f94236e20503e76f0c94b0cf38bf0824".equals(dependency.getSha1Hash())){
                assertEquals("braces:1.8.5", dependency.getFileName());
                assertEquals("project/node_modules/braces/package.json", dependency.getFilePath());
                assertEquals("a5663cea473ad651ac4a00a504883a22", dependency.getMd5Hash());
                assertEquals("8082af62f94236e20503e76f0c94b0cf38bf0824", dependency.getSha1Hash());

                Map<String, List<Evidence>> evidenceCollected = dependency.getEvidenceCollected();
                int evidenceAmount = 0;
                for (Map.Entry<String, List<Evidence>> evidences : evidenceCollected.entrySet()) {
                    evidenceAmount += evidences.getValue().size();
                    for (Evidence evidence : evidences.getValue()) {
                        assertFalse(evidence.getSource().isEmpty());
                        assertFalse(evidence.getName().isEmpty());
                        assertFalse(evidence.getValue().isEmpty());
                        assertFalse(evidence.getConfidence() == null);
                        assertFalse(evidence.getType().isEmpty());
                    }
                }
                assertEquals(6, evidenceAmount);

                Collection<Vulnerability> vulnerabilities = dependency.getVulnerabilities();
                assertEquals(1, vulnerabilities.size());
                Iterator<Vulnerability> vulnIterator = vulnerabilities.iterator();
                Vulnerability vulnerability = (Vulnerability) vulnIterator.next();;
                assertEquals("786", vulnerability.getName());
                assertEquals("NPM", vulnerability.getSource());
                assertEquals(4.0f, vulnerability.getCvssScore(false), 0.0f);
                assertEquals(4.0f, vulnerability.getCvssScore(), 0.0f);
                assertEquals("MEDIUM", vulnerability.getSeverity());
                assertEquals("MEDIUM", vulnerability.getSeverity(false));
                assertFalse(vulnerability.getCwe().isPresent());
                assertEquals("Versions of `braces` prior to 2.3.1 are vulnerable to Regular Expression Denial of Service (ReDoS). Untrusted input may cause catastrophic backtracking while matching regular expressions. This can cause the application to be unresponsive leading to Denial of Service.", vulnerability.getDescription());
            }
        }
    }

}
