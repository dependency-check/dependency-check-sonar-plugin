/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2025 dependency-check
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
package org.sonar.dependencycheck.parser.deserializer;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sonar.dependencycheck.reason.maven.MavenDependencyLocation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import edu.umd.cs.findbugs.annotations.Nullable;

public class MavenDependencyDeserializer extends StdDeserializer<List<MavenDependencyLocation>>{

    /**
     *
     */
    private static final long serialVersionUID = 98135652558542641L;

    protected MavenDependencyDeserializer() {
        this(null);
    }

    protected MavenDependencyDeserializer(@Nullable Class<?> vc) {
        super(vc);
    }

    @Override
    public List<MavenDependencyLocation> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        List<MavenDependencyLocation> mavenDependencies = new LinkedList<>();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            if (StringUtils.equalsIgnoreCase("dependency", jsonParser.currentName())) {
                // We found a dependency
                String groupId = "";
                String artifactId = "";
                String version = "";
                int startLineNr = jsonParser.currentLocation().getLineNr();
                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    if (StringUtils.equalsIgnoreCase("artifactId", jsonParser.currentName())) {
                        artifactId = jsonParser.getValueAsString();
                    }
                    if (StringUtils.equalsIgnoreCase("groupId", jsonParser.currentName())) {
                        groupId = jsonParser.getValueAsString();
                    }
                    if (StringUtils.equalsIgnoreCase("version", jsonParser.currentName())) {
                        version = jsonParser.getValueAsString();
                    }
                }
                int endLineNr = jsonParser.currentLocation().getLineNr();
                mavenDependencies.add(new MavenDependencyLocation(groupId, artifactId, version, startLineNr, endLineNr));
            }
        }
        return mavenDependencies;
    }
}
