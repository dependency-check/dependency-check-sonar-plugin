/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2024 dependency-check
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.parser.PackageLockParserHelper;
import org.sonar.dependencycheck.parser.ReportParserException;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.reason.npm.NPMDependency;
import org.sonar.dependencycheck.reason.npm.NPMDependencyLocation;
import org.sonar.dependencycheck.reason.npm.PackageLockModel;

import edu.umd.cs.findbugs.annotations.NonNull;

public class NPMDependencyReason extends DependencyReason {

    private final InputFile packageLock;
    private PackageLockModel packageLockModel;
    private final Map<Dependency, TextRangeConfidence> dependencyMap;

    private static final Logger LOGGER = LoggerFactory.getLogger(NPMDependencyReason.class);

    public NPMDependencyReason(InputFile packageLock) {
        super(packageLock, Language.JAVASCRIPT);
        this.packageLock = packageLock;
        dependencyMap = new HashMap<>();
        packageLockModel = null;
        try {
            packageLockModel = PackageLockParserHelper.parse(packageLock.inputStream());
        } catch (ReportParserException | IOException e) {
            LOGGER.warn("Parsing {} failed", packageLock);
            LOGGER.debug(e.getMessage(), e);
        }
    }

    @Override
    public boolean isReasonable() {
        return packageLock != null && packageLockModel != null;
    }

    @NonNull
    @Override
    public InputComponent getInputComponent() {
        return packageLock;
    }

    @NonNull
    @Override
    public TextRangeConfidence getBestTextRange(Dependency dependency) {
        if (!dependencyMap.containsKey(dependency)) {
            Optional<NPMDependency> npmDependency = DependencyCheckUtils.getNPMDependency(dependency);
            if (npmDependency.isPresent()) {
                fillArtifactMatch(dependency, npmDependency.get());
            } else {
                LOGGER.debug("No Identifier with type npm/javascript found for Dependency {}", dependency.getFileName());
            }
            dependencyMap.computeIfAbsent(dependency, k -> addDependencyToFirstLine(k, packageLock));
        }
        return dependencyMap.get(dependency);
    }

    private void fillArtifactMatch(@NonNull Dependency dependency, NPMDependency npmDependency) {
        // Try to find in <dependency>
        for (NPMDependencyLocation npmDependencyLocation : packageLockModel.getDependencies()) {
            checkNPMDependency(npmDependency, npmDependencyLocation)
                    .ifPresent(textrange -> dependencyMap.put(dependency, textrange));
        }
    }

    private Optional<TextRangeConfidence> checkNPMDependency(NPMDependency npmDependency, NPMDependencyLocation npmDependencyLocation) {
        if (StringUtils.equals(npmDependency.getName(), npmDependencyLocation.getName())) {
            Optional<String> npmDepVersion = npmDependency.getVersion();
            Optional<String> npmDepLocVersion = npmDependencyLocation.getVersion();
            if (npmDepVersion.isPresent() && npmDepLocVersion.isPresent() &&
                StringUtils.equals(npmDepVersion.get(), npmDepLocVersion.get())) {
                LOGGER.debug("Found a name and version match in {}", packageLock);
                return Optional.of(new TextRangeConfidence(packageLock.newRange(packageLock.selectLine(npmDependencyLocation.getStartLineNr()).start(), packageLock.selectLine(npmDependencyLocation.getEndLineNr()).end()), Confidence.HIGHEST));
            }
            LOGGER.debug("Found a name match in {} for {}", packageLock, npmDependency.getName());
            return Optional.of(new TextRangeConfidence(packageLock.newRange(packageLock.selectLine(npmDependencyLocation.getStartLineNr()).start(), packageLock.selectLine(npmDependencyLocation.getEndLineNr()).end()), Confidence.HIGH));
        }
        return Optional.empty();
    }

}
