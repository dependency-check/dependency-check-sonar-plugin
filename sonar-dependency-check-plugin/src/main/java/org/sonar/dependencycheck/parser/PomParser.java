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

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.reason.maven.MavenDependency;
import org.sonar.dependencycheck.reason.maven.MavenParent;
import org.sonar.dependencycheck.reason.maven.MavenPomModel;

public class PomParser {

    private final InputFile pom;

    public PomParser(InputFile pom) {
        this.pom = pom;
    }

    public MavenPomModel parse() throws XMLStreamException, IOException {
        SMInputFactory inputFactory = DependencyCheckUtils.newStaxParser();
        SMHierarchicCursor rootC = inputFactory.rootElementCursor(pom.inputStream());
        rootC.advance(); // <pom>
        SMInputCursor childCursor = rootC.childCursor();

        List<MavenDependency> dependencies = Collections.emptyList();
        MavenParent parent = null;
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if (StringUtils.equalsIgnoreCase("dependencies", nodeName)) {
                dependencies = processDependencies(childCursor);
            } else if (StringUtils.equalsIgnoreCase("parent", nodeName)) {
                parent = processParent(childCursor);
            }
        }
        return new MavenPomModel(dependencies, parent);
    }

    private MavenParent processParent(SMInputCursor pC) throws XMLStreamException {
        String groupId = "";
        TextRange textRange = null;
        SMInputCursor childCursor = pC.childCursor();
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if (StringUtils.equalsIgnoreCase(nodeName, "groupId")) {
                textRange = pom.selectLine(childCursor.getCursorLocation().getLineNumber());
                groupId = StringUtils.trim(childCursor.collectDescendantText(false));
            }
        }
        return new MavenParent(groupId, textRange);
    }

    private List<MavenDependency> processDependencies(SMInputCursor childCursor) throws XMLStreamException {
        List<MavenDependency> dependencies = new LinkedList<>();
        SMInputCursor cursor = childCursor.childElementCursor("dependency");
        while (cursor.getNext() != null) {
            dependencies.add(processDependency(cursor));
        }
        return dependencies;
    }

    private MavenDependency processDependency(SMInputCursor dpC) throws XMLStreamException {
        String groupId = "";
        String artifactId = "";
        TextRange textRange = null;
        SMInputCursor childCursor = dpC.childCursor();
        while (childCursor.getNext() != null) {
            String nodeName = childCursor.getLocalName();
            if (StringUtils.equalsIgnoreCase(nodeName, "groupId")) {
                groupId = StringUtils.trim(childCursor.collectDescendantText(false));
            } else if (StringUtils.equalsIgnoreCase(nodeName, "artifactId")) {
                textRange = pom.selectLine(childCursor.getCursorLocation().getLineNumber());
                artifactId =  StringUtils.trim(childCursor.collectDescendantText(false));
            }
        }
        return new MavenDependency(groupId, artifactId, textRange);
    }
}
