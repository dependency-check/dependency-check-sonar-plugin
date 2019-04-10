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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Evidence;
import org.sonar.dependencycheck.parser.element.Identifier;
import org.sonar.dependencycheck.parser.element.ProjectInfo;
import org.sonar.dependencycheck.parser.element.ScanInfo;
import org.sonar.dependencycheck.parser.element.Vulnerability;

public class ReportParser {

    private static final Logger LOGGER = Loggers.get(ReportParser.class);

    private ReportParser() {
        // do nothing
    }

    public static Analysis parse(InputStream inputStream) throws XMLStreamException, ReportParserException {

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
        scanInfo = Optional.ofNullable(scanInfo).orElseThrow(() -> new ReportParserException("Analysis - scanInfo not found"));
        return new Analysis(scanInfo, projectInfo, dependencies);
    }

    private static Collection<Dependency> processDependencies(SMInputCursor depC) throws XMLStreamException, ReportParserException {
        Collection<Dependency> dependencies = new ArrayList<>();
        SMInputCursor cursor = depC.childElementCursor("dependency");
        while (cursor.getNext() != null) {
            dependencies.add(processDependency(cursor));
        }
        return dependencies;
    }

    private static Dependency processDependency(SMInputCursor depC) throws XMLStreamException, ReportParserException {
        SMInputCursor childCursor = depC.childCursor();
        String fileName = null;
        String filepath = null;
        String md5Hash = null;
        String sha1Hash = null;
        Collection<Evidence> evidences = Collections.emptyList();
        Collection<Identifier> identifiers = Collections.emptyList();
        List<Vulnerability> vulnerabilities = Collections.emptyList();
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if (nodeName == null) {
                continue;
            }
            switch (nodeName) {
                case "fileName":
                    fileName = StringUtils.trim(childCursor.collectDescendantText(false));
                    break;
                case "filePath":
                    filepath = StringUtils.trim(childCursor.collectDescendantText(false));
                    break;
                case "md5":
                    md5Hash = StringUtils.trim(childCursor.collectDescendantText(false));
                    break;
                case "sha1":
                    sha1Hash = StringUtils.trim(childCursor.collectDescendantText(false));
                    break;
                case "evidenceCollected":
                    evidences = processEvidenceCollected(childCursor);
                    break;
                case "vulnerabilities":
                    vulnerabilities = processVulnerabilities(childCursor);
                    break;
                case "identifiers":
                    identifiers = processIdentifiersCollected(childCursor);
                    break;
                default:
                    LOGGER.debug("Depedency Node {} is not used", nodeName);
                    break;
            }
        }
        fileName = Optional.ofNullable(fileName).orElseThrow(() -> new ReportParserException("Dependency - fileName not found"));
        filepath = Optional.ofNullable(filepath).orElseThrow(() -> new ReportParserException("Dependency - filePath not found"));
        md5Hash = Optional.ofNullable(md5Hash).orElseThrow(() -> new ReportParserException("Dependency - md5 not found"));
        sha1Hash = Optional.ofNullable(sha1Hash).orElseThrow(() -> new ReportParserException("Dependency - sha1 not found"));
        return new Dependency(fileName, filepath, md5Hash, sha1Hash, evidences, identifiers, vulnerabilities);
    }

    private static List<Vulnerability> processVulnerabilities(SMInputCursor vulnC) throws XMLStreamException, ReportParserException {
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        SMInputCursor cursor = vulnC.childElementCursor("vulnerability");
        while (cursor.getNext() != null) {
            vulnerabilities.add(processVulnerability(cursor));
        }
        return vulnerabilities;
    }

    private static Vulnerability processVulnerability(SMInputCursor vulnC) throws XMLStreamException, ReportParserException {
        SMInputCursor childCursor = vulnC.childCursor();
        String name = null;
        Float cvssScore = null;
        String severity = null;
        String description = null;
        String cwe = null;
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if ("name".equals(nodeName)) {
                name = StringUtils.trim(childCursor.collectDescendantText(false));
            } else if ("cvssScore".equals(nodeName)) {
                String cvssScoreTmp = StringUtils.trim(childCursor.collectDescendantText(false));
                try {
                    cvssScore = Float.parseFloat(cvssScoreTmp);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Could not parse CVSS-Score {} to Float. Setting CVSS-Score to 0.0", cvssScoreTmp);
                    cvssScore = 0.0f;
                }
            } else if ("severity".equals(nodeName)) {
                severity = StringUtils.trim(childCursor.collectDescendantText(false));
            } else if ("cwe".equals(nodeName)) {
                cwe = StringUtils.trim(childCursor.collectDescendantText(false));
            } else if ("description".equals(nodeName)) {
                description = StringUtils.trim(childCursor.collectDescendantText(false));
            }
        }
        name = Optional.ofNullable(name).orElseThrow(() -> new ReportParserException("Vulnerability - name not found"));
        cvssScore = Optional.ofNullable(cvssScore).orElseThrow(() -> new ReportParserException("Vulnerability - cvssScore not found"));
        severity = Optional.ofNullable(severity).orElseThrow(() -> new ReportParserException("Vulnerability - severity not found"));
        description = Optional.ofNullable(description).orElseThrow(() -> new ReportParserException("Vulnerability - description not found"));
        return new Vulnerability(name, cvssScore, severity, description, cwe);
    }

    private static Collection<Evidence> processEvidenceCollected(SMInputCursor ecC) throws XMLStreamException, ReportParserException {
        Collection<Evidence> evidenceCollection = new ArrayList<>();
        SMInputCursor cursor = ecC.childElementCursor("evidence");
        while (cursor.getNext() != null) {
            evidenceCollection.add(processEvidence(cursor));
        }
        return evidenceCollection;
    }

    private static Evidence processEvidence(SMInputCursor ecC) throws XMLStreamException, ReportParserException {
        SMInputCursor childCursor = ecC.childCursor();
        String source = null;
        String name = null;
        String value = null;
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if ("source".equals(nodeName)) {
                source = StringUtils.trim(childCursor.collectDescendantText(false));
            } else if ("name".equals(nodeName)) {
                name = StringUtils.trim(childCursor.collectDescendantText(false));
            } else if ("value".equals(nodeName)) {
                value = StringUtils.trim(childCursor.collectDescendantText(false));
            }
        }
        source = Optional.ofNullable(source).orElseThrow(() -> new ReportParserException("Evidence - source not found"));
        name = Optional.ofNullable(name).orElseThrow(() -> new ReportParserException("Evidence - name not found"));
        value = Optional.ofNullable(value).orElseThrow(() -> new ReportParserException("Evidence - source not found"));
        return new Evidence(source, name, value);
    }

    private static Collection<Identifier> processIdentifiersCollected(SMInputCursor ifC) throws XMLStreamException, ReportParserException {
        Collection<Identifier> identifierCollection = new ArrayList<>();
        SMInputCursor cursor = ifC.childElementCursor("identifier");
        while (cursor.getNext() != null) {
            identifierCollection.add(processIdentifiers(cursor));
        }
        return identifierCollection;
    }

    private static Identifier processIdentifiers(SMInputCursor ifC) throws XMLStreamException, ReportParserException {
        String type = null;
        Confidence confidence = null;
        String name = null;
        for (int i = 0; i < ifC.getAttrCount(); ++i) {
            if (ifC.getAttrLocalName(i).equals("type")) {
                type = ifC.getAttrValue(i);
            }
            if (ifC.getAttrLocalName(i).equals("confidence")) {
                confidence = Confidence.valueOf(ifC.getAttrValue(i));
            }
        }
        SMInputCursor childCursor = ifC.childCursor();
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if ("name".equals(nodeName)) {
                name = StringUtils.trim(childCursor.collectDescendantText(false));
            }
        }
        type = Optional.ofNullable(type).orElseThrow(() -> new ReportParserException("Identifier - type not found"));
        name = Optional.ofNullable(name).orElseThrow(() -> new ReportParserException("Identifier - name not found"));
        return new Identifier(type, confidence, name);
    }

    private static ScanInfo processScanInfo(SMInputCursor siC) throws XMLStreamException, ReportParserException {
        SMInputCursor childCursor = siC.childCursor();
        String engineVersion = null;
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if (StringUtils.equalsIgnoreCase("engineVersion", nodeName)) {
                engineVersion = StringUtils.trim(childCursor.collectDescendantText(false));
            }
        }
        if (engineVersion == null) {
            throw new ReportParserException("eningeVersion not found");
        }
        return new ScanInfo(engineVersion);
    }

    private static ProjectInfo processProjectInfo(SMInputCursor piC) throws XMLStreamException, ReportParserException {
        SMInputCursor childCursor = piC.childCursor();
        String projectName = null;
        String projectReportDate = null;
        String projectCredits = null;
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if (nodeName == null) {
                continue;
            }
            switch (nodeName) {
            case "name":
                projectName = StringUtils.trim(childCursor.collectDescendantText(false));
                break;
            case "reportDate":
                projectReportDate = StringUtils.trim(childCursor.collectDescendantText(false));
                break;
            case "credits":
                projectCredits = StringUtils.trim(childCursor.collectDescendantText(false));
                break;
            default:
                break;
            }
        }
        projectName = Optional.ofNullable(projectName).orElseThrow(() -> new ReportParserException("project - name not found"));
        projectReportDate = Optional.ofNullable(projectReportDate).orElseThrow(() -> new ReportParserException("project - reportDate not found"));
        projectCredits =  Optional.ofNullable(projectCredits).orElseThrow(() -> new ReportParserException("project - credits not found"));
        return new ProjectInfo(projectName, projectReportDate, projectCredits);
    }

}
