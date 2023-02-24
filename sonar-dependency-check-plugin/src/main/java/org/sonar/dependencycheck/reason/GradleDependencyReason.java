/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2021 dependency-check
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

package org.sonar.dependencycheck.reason;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.IncludedBy;
import org.sonar.dependencycheck.parser.element.Vulnerability;
import org.sonar.dependencycheck.reason.maven.MavenDependency;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class GradleDependencyReason extends DependencyReason {

    private final InputFile buildGradle;
    private String content;
    private final Map<Map<Dependency, Vulnerability>, TextRangeConfidence> dependencyMap;

    private static final Logger LOGGER = Loggers.get(GradleDependencyReason.class);

    public GradleDependencyReason(@NonNull InputFile buildGradle) {
        super(buildGradle, Language.JAVA);
        this.buildGradle = buildGradle;
        dependencyMap = new HashMap<>();
        content = "";
        try {
            content = buildGradle.contents();
        } catch (IOException e) {
            LOGGER.warn("Could not read build.gradle file");
        }
    }

    @Override
    @NonNull
    public TextRangeConfidence getBestTextRange(@NonNull Dependency dependency, @Nullable Vulnerability vulnerability) {
        if (dependencyMap.containsKey(dependency)) {
            return dependencyMap.get(dependency);
        } else {
            Optional<MavenDependency> mavenDependency = DependencyCheckUtils.getMavenDependency(dependency);
            if (mavenDependency.isPresent()) {
                fillArtifactMatch(dependency, vulnerability, mavenDependency.get());
            } else {
                LOGGER.debug("No artifactId found for Dependency {}", dependency.getFileName());
            }
            Optional<Collection<IncludedBy>> includedBys = dependency.getIncludedBy();
            if (includedBys.isPresent()) {
                workOnIncludedBy(dependency, vulnerability, includedBys.get());
            }
            dependencyMap.computeIfAbsent(Collections.singletonMap(dependency, vulnerability), k -> addDependencyToFirstLine(k, buildGradle));
        }
        return dependencyMap.get(Collections.singletonMap(dependency, vulnerability));
    }

    private void workOnIncludedBy(@NonNull Dependency dependency, @Nullable Vulnerability vulnerability, Collection<IncludedBy> includedBys) {
        for (IncludedBy includedBy : includedBys) {
            String reference = includedBy.getReference();
            if (StringUtils.isNotBlank(reference)) {
                Optional<SoftwareDependency> softwareDependency = DependencyCheckUtils.convertToSoftwareDependency(reference);
                if (softwareDependency.isPresent() && DependencyCheckUtils.isMavenDependency(softwareDependency.get())) {
                    fillArtifactMatch(dependency, vulnerability, (MavenDependency) softwareDependency.get());
                }
            }
        }
    }

    private void putDependencyMap(@NonNull Dependency dependency, @Nullable Vulnerability vulnerability, TextRangeConfidence newTextRange) {
        if (dependencyMap.containsKey(dependency)) {
            TextRangeConfidence oldTextRange = dependencyMap.get(dependency);
            if (oldTextRange.getConfidence().compareTo(newTextRange.getConfidence()) > 0) {
                dependencyMap.put(Collections.singletonMap(dependency, vulnerability), newTextRange);
            }
        } else {
            dependencyMap.put(Collections.singletonMap(dependency, vulnerability), newTextRange);
        }
    }

    /**
     *
     * At the moment a simple line parser without transitive dependencies
     *
     * @param mavenDependency Identifier for gradle
     * @return TextRange if found in gradle, else null
     */
    private void fillArtifactMatch(@NonNull Dependency dependency, @Nullable Vulnerability vulnerability, MavenDependency mavenDependency) {
        try (final Scanner scanner = new Scanner(content)) {
            int linenumber = 0;
            while (scanner.hasNextLine()) {
                final String lineFromFile = scanner.nextLine();
                linenumber++;
                if (lineFromFile.contains(mavenDependency.getArtifactId()) &&
                    lineFromFile.contains(mavenDependency.getGroupId())) {
                    Optional<String> depVersion = mavenDependency.getVersion();
                    if (depVersion.isPresent() &&
                        lineFromFile.contains(depVersion.get())) {
                        LOGGER.debug("Found a artifactId, groupId and version match in {}", buildGradle);
                        putDependencyMap(dependency, vulnerability, new TextRangeConfidence(buildGradle.selectLine(linenumber), Confidence.HIGHEST));
                        return;
                    } else {
                    	LOGGER.debug("Found a artifactId and groupId match in {} on line {}", buildGradle, linenumber);
                    	putDependencyMap(dependency, vulnerability, new TextRangeConfidence(buildGradle.selectLine(linenumber), Confidence.HIGH));
                    }
                } else if (lineFromFile.contains(mavenDependency.getArtifactId())) {
                    LOGGER.debug("Found a artifactId match in {} for {}", buildGradle, mavenDependency.getArtifactId());
                    putDependencyMap(dependency, vulnerability, new TextRangeConfidence(buildGradle.selectLine(linenumber), Confidence.MEDIUM));
                } else if (lineFromFile.contains(mavenDependency.getGroupId())) {
                    LOGGER.debug("Found a groupId match in {} for {}", buildGradle, mavenDependency.getGroupId());
                    putDependencyMap(dependency, vulnerability, new TextRangeConfidence(buildGradle.selectLine(linenumber), Confidence.MEDIUM));
                }
            }
        }
    }

    /**
     * Checks if we have a pom File and this pom file is readable and has content Pom Files contains the in maven builds the
     * dependencies
     */
    @Override
    public boolean isReasonable() {
        return buildGradle != null && content != null && !content.isEmpty();
    }

    /**
     * returns pom file
     */
    @NonNull
    @Override
    public InputComponent getInputComponent() {
        return buildGradle;
    }
}
