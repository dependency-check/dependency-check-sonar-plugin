/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2023 dependency-check
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.parser.PomParserHelper;
import org.sonar.dependencycheck.parser.ReportParserException;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.IncludedBy;
import org.sonar.dependencycheck.reason.maven.MavenDependency;
import org.sonar.dependencycheck.reason.maven.MavenDependencyLocation;
import org.sonar.dependencycheck.reason.maven.MavenPomModel;

import edu.umd.cs.findbugs.annotations.NonNull;

public class MavenDependencyReason extends DependencyReason {

    private final InputFile pom;
    private final Map<Dependency, TextRangeConfidence> dependencyMap;
    private MavenPomModel pomModel;

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenDependencyReason.class);

    public MavenDependencyReason(@NonNull InputFile pom) {
        super(pom, Language.JAVA);
        this.pom = pom;
        dependencyMap = new HashMap<>();
        pomModel = null;
        try {
            pomModel = PomParserHelper.parse(pom.inputStream());
        } catch (ReportParserException | IOException e) {
            LOGGER.warn("Parsing {} failed", pom);
            LOGGER.debug(e.getMessage(), e);
        }
    }

    @Override
    @NonNull
    public TextRangeConfidence getBestTextRange(@NonNull Dependency dependency) {
        if (!dependencyMap.containsKey(dependency)) {
            Optional<MavenDependency> mavenDependency = DependencyCheckUtils.getMavenDependency(dependency);
            if (mavenDependency.isPresent()) {
                fillArtifactMatch(dependency, mavenDependency.get());
            } else {
                LOGGER.debug("No Identifier with type maven found for Dependency {}", dependency.getFileName());
            }
            Optional<Collection<IncludedBy>> includedBys = dependency.getIncludedBy();
            if (includedBys.isPresent()) {
                workOnIncludedBy(dependency, includedBys.get());
            }
            dependencyMap.computeIfAbsent(dependency, k -> addDependencyToFirstLine(k, pom));
        }
        return dependencyMap.get(dependency);
    }

    private void workOnIncludedBy(@NonNull Dependency dependency, Collection<IncludedBy> includedBys) {
        for (IncludedBy includedBy : includedBys) {
            String reference = includedBy.getReference();
            if (StringUtils.isNotBlank(reference)) {
                Optional<SoftwareDependency> softwareDependency = DependencyCheckUtils.convertToSoftwareDependency(reference);
                if (softwareDependency.isPresent() && DependencyCheckUtils.isMavenDependency(softwareDependency.get())) {
                    fillArtifactMatch(dependency, (MavenDependency) softwareDependency.get());
                }
            }
        }
    }
    /**
     *
     * @param dependency
     * @param mavenDependency
     */
    private void fillArtifactMatch(@NonNull Dependency dependency, MavenDependency mavenDependency) {
        // Try to find in <dependency>
        for (MavenDependencyLocation mavenDependencyLocation : pomModel.getDependencies()) {
            checkPomDependency(mavenDependency, mavenDependencyLocation)
                .ifPresent(textRange -> putDependencyMap(dependency, textRange));
        }
        // Check Parent if present
        pomModel.getParent()
            .ifPresent(parent -> checkPomDependency(mavenDependency, parent)
            .ifPresent(textRange -> putDependencyMap(dependency, textRange)));
    }

    private void putDependencyMap(@NonNull Dependency dependency, TextRangeConfidence newTextRange) {
        if (dependencyMap.containsKey(dependency)) {
            TextRangeConfidence oldTextRange = dependencyMap.get(dependency);
            if (oldTextRange.getConfidence().compareTo(newTextRange.getConfidence()) > 0) {
                dependencyMap.put(dependency, newTextRange);
            }
        } else {
            dependencyMap.put(dependency, newTextRange);
        }
    }

    private Optional<TextRangeConfidence> checkPomDependency(MavenDependency mavenDependency, MavenDependencyLocation mavenDependencyLocation) {
        if (StringUtils.equals(mavenDependency.getArtifactId(), mavenDependencyLocation.getArtifactId())
            && StringUtils.equals(mavenDependency.getGroupId(), mavenDependencyLocation.getGroupId())) {
            Optional<String> depVersion = mavenDependency.getVersion();
            Optional<String> depLocVersion = mavenDependencyLocation.getVersion();
            if (depVersion.isPresent() && depLocVersion.isPresent() &&
                StringUtils.equals(depVersion.get(), depLocVersion.get())) {
                LOGGER.debug("Found a artifactId, groupId and version match in {} ({} - {})", pom, mavenDependencyLocation.getStartLineNr(), mavenDependencyLocation.getEndLineNr());
                return Optional.of(new TextRangeConfidence(pom.newRange(pom.selectLine(mavenDependencyLocation.getStartLineNr()).start(), pom.selectLine(mavenDependencyLocation.getEndLineNr()).end()), Confidence.HIGHEST));
            }
            LOGGER.debug("Found a artifactId and groupId match in {} ({} - {})", pom, mavenDependencyLocation.getStartLineNr(), mavenDependencyLocation.getEndLineNr());
            return Optional.of(new TextRangeConfidence(pom.newRange(pom.selectLine(mavenDependencyLocation.getStartLineNr()).start(), pom.selectLine(mavenDependencyLocation.getEndLineNr()).end()), Confidence.HIGH));
        }
        if (StringUtils.equals(mavenDependency.getArtifactId(), mavenDependencyLocation.getArtifactId())) {
            LOGGER.debug("Found a artifactId match in {} ({} - {})", pom, mavenDependencyLocation.getStartLineNr(), mavenDependencyLocation.getEndLineNr());
            return Optional.of(new TextRangeConfidence(pom.newRange(pom.selectLine(mavenDependencyLocation.getStartLineNr()).start(), pom.selectLine(mavenDependencyLocation.getEndLineNr()).end()), Confidence.MEDIUM));
        }
        if (StringUtils.equals(mavenDependency.getGroupId(), mavenDependencyLocation.getGroupId())) {
            LOGGER.debug("Found a groupId match in {} ({} - {})", pom, mavenDependencyLocation.getStartLineNr(), mavenDependencyLocation.getEndLineNr());
            return Optional.of(new TextRangeConfidence(pom.newRange(pom.selectLine(mavenDependencyLocation.getStartLineNr()).start(), pom.selectLine(mavenDependencyLocation.getEndLineNr()).end()), Confidence.MEDIUM));
        }
         return Optional.empty();
    }

    /**
     * Checks if we have a pom File and this pom file is readable and has content
     * Pom Files contains the in maven builds the dependencies
     */
    @Override
    public boolean isReasonable() {
        return pom != null && pomModel != null;
    }

    /**
     * returns pom file
     */
    @NonNull
    @Override
    public InputComponent getInputComponent() {
        return pom;
    }
}
