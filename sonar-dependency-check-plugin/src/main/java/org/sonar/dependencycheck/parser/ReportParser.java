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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Evidence;
import org.sonar.dependencycheck.parser.element.ProjectInfo;
import org.sonar.dependencycheck.parser.element.ScanInfo;
import org.sonar.dependencycheck.parser.element.Vulnerability;

public class ReportParser {

    private static final Logger LOGGER = Loggers.get(ReportParser.class);

    private ReportParser() {
        // do nothing
    }

    public static Analysis parse(InputStream inputStream) throws XMLStreamException {

        SMInputFactory inputFactory = DependencyCheckUtils.newStaxParser();
        SMHierarchicCursor rootC = inputFactory.rootElementCursor(inputStream);
        rootC.advance(); // <analysis>

        SMInputCursor childCursor = rootC.childCursor();

        ScanInfo scanInfo = null;
        ProjectInfo projectInfo = null;
        Collection<Dependency> dependencies = Collections.emptyList();

        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if ("scanInfo".equals(nodeName)) {
                scanInfo = processScanInfo(childCursor);
            } else if ("projectInfo".equals(nodeName)) {
                projectInfo = processProjectInfo(childCursor);
            } else if ("dependencies".equals(nodeName)) {
                dependencies = processDependencies(childCursor);
            }
        }
        return new Analysis(scanInfo, projectInfo, dependencies);
    }

    private static Collection<Dependency> processDependencies(SMInputCursor depC) throws XMLStreamException {
        Collection<Dependency> dependencies = new ArrayList<>();
        SMInputCursor cursor = depC.childElementCursor("dependency");
        while (cursor.getNext() != null) {
            dependencies.add(processDependency(cursor));
        }
        return dependencies;
    }

    private static Dependency processDependency(SMInputCursor depC) throws XMLStreamException {
        Dependency dependency = new Dependency();
        SMInputCursor childCursor = depC.childCursor();
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if ("fileName".equals(nodeName)) {
                dependency.setFileName(StringUtils.trim(childCursor.collectDescendantText(false)));
            } else if ("filePath".equals(nodeName)) {
                dependency.setFilePath(StringUtils.trim(childCursor.collectDescendantText(false)));
            } else if ("md5".equals(nodeName)) {
                dependency.setMd5Hash(StringUtils.trim(childCursor.collectDescendantText(false)));
            } else if ("sha1".equals(nodeName)) {
                dependency.setSha1Hash(StringUtils.trim(childCursor.collectDescendantText(false)));
            } else if ("evidenceCollected".equals(nodeName)) {
                dependency.setEvidenceCollected(processEvidenceCollected(childCursor));
            } else if ("vulnerabilities".equals(nodeName)) {
                dependency.setVulnerabilities(processVulnerabilities(childCursor));
            }

        }
        return dependency;
    }

    private static List<Vulnerability> processVulnerabilities(SMInputCursor vulnC) throws XMLStreamException {
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        SMInputCursor cursor = vulnC.childElementCursor("vulnerability");
        while (cursor.getNext() != null) {
            vulnerabilities.add(processVulnerability(cursor));
        }
        return vulnerabilities;
    }

    private static Vulnerability processVulnerability(SMInputCursor vulnC) throws XMLStreamException {
        Vulnerability vulnerability = new Vulnerability();
        SMInputCursor childCursor = vulnC.childCursor();
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if ("name".equals(nodeName)) {
                vulnerability.setName(StringUtils.trim(childCursor.collectDescendantText(false)));
            } else if ("cvssScore".equals(nodeName)) {
                String cvssScore = StringUtils.trim(childCursor.collectDescendantText(false));
                try {
                    vulnerability.setCvssScore(Float.parseFloat(cvssScore));
                } catch (NumberFormatException e) {
                    LOGGER.warn("Could not parse CVSS-Score {} to Float. Setting CVSS-Score to 0.0", cvssScore);
                    vulnerability.setCvssScore(0.0f);
                }
            } else if ("severity".equals(nodeName)) {
                vulnerability.setSeverity(StringUtils.trim(childCursor.collectDescendantText(false)));
            } else if ("cwe".equals(nodeName)) {
                vulnerability.setCwe(StringUtils.trim(childCursor.collectDescendantText(false)));
            } else if ("description".equals(nodeName)) {
                vulnerability.setDescription(StringUtils.trim(childCursor.collectDescendantText(false)));
            }
        }
        return vulnerability;
    }

    private static Collection<Evidence> processEvidenceCollected(SMInputCursor ecC) throws XMLStreamException {
        Collection<Evidence> evidenceCollection = new ArrayList<>();
        SMInputCursor cursor = ecC.childElementCursor("evidence");
        while (cursor.getNext() != null) {
            evidenceCollection.add(processEvidence(cursor));
        }
        return evidenceCollection;
    }

    private static Evidence processEvidence(SMInputCursor ecC) throws XMLStreamException {
        Evidence evidence = new Evidence();
        SMInputCursor childCursor = ecC.childCursor();
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if ("source".equals(nodeName)) {
                evidence.setSource(StringUtils.trim(childCursor.collectDescendantText(false)));
            } else if ("name".equals(nodeName)) {
                evidence.setName(StringUtils.trim(childCursor.collectDescendantText(false)));
            } else if ("value".equals(nodeName)) {
                evidence.setValue(StringUtils.trim(childCursor.collectDescendantText(false)));
            }
        }
        return evidence;
    }

    private static ScanInfo processScanInfo(SMInputCursor siC) throws XMLStreamException {
        SMInputCursor childCursor = siC.childCursor();
        ScanInfo scanInfo = new ScanInfo();
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if (StringUtils.equalsIgnoreCase("engineVersion", nodeName)) {
                scanInfo.setEngineVersion(StringUtils.trim(childCursor.collectDescendantText(false)));
            }
        }
        return scanInfo;
    }

    private static ProjectInfo processProjectInfo(SMInputCursor piC) throws XMLStreamException {
        SMInputCursor childCursor = piC.childCursor();
        ProjectInfo projectInfo = new ProjectInfo();
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if (StringUtils.equalsIgnoreCase("name", nodeName)) {
                projectInfo.setName(StringUtils.trim(childCursor.collectDescendantText(false)));
            } else if (StringUtils.equalsIgnoreCase("reportDate", nodeName)) {
                projectInfo.setReportDate(StringUtils.trim(childCursor.collectDescendantText(false)));
            } else if (StringUtils.equalsIgnoreCase("credits", nodeName)) {
                projectInfo.setCredits(StringUtils.trim(childCursor.collectDescendantText(false)));
            }
        }
        return projectInfo;
    }

}
