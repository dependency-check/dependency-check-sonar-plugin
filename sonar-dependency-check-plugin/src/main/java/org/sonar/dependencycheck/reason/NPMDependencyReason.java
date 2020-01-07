/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2019 dependency-check
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

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.parser.PackageLockParserHelper;
import org.sonar.dependencycheck.parser.ReportParserException;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Identifier;
import org.sonar.dependencycheck.reason.npm.PackageLockModel;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

public class NPMDependencyReason extends DependencyReason {

    private final InputFile packageLock;
    private PackageLockModel packageLockModel;
    private final Map<Dependency, TextRangeConfidence> dependencyMap;

    private static final Logger LOGGER = Loggers.get(NPMDependencyReason.class);

    public NPMDependencyReason(InputFile packageLock) {
        super(packageLock);
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

    @Override
    @CheckForNull
    public InputComponent getInputComponent() {
        return packageLock;
    }

    @Override
    public TextRangeConfidence getBestTextRange(Dependency dependency) {
        if (!dependencyMap.containsKey(dependency)) {
            Optional<Identifier> javaScriptIdentifier = DependencyCheckUtils.getJavaScriptIdentifier(dependency);
            if (javaScriptIdentifier.isPresent()) {
                fillArtifactMatch(dependency, javaScriptIdentifier.get());
            } else {
                LOGGER.debug("No Identifier with type javascript found for Dependency {}", dependency.getFileName());
            }
            Optional<Identifier> NpmIdentifier = DependencyCheckUtils.getJavaScriptIdentifier(dependency);
            if (NpmIdentifier.isPresent()) {
                fillArtifactMatch(dependency, NpmIdentifier.get());
            } else {
                LOGGER.debug("No Identifier with type javascript found for Dependency {}", dependency.getFileName());
            }
            if (!dependencyMap.containsKey(dependency) || dependencyMap.get(dependency) == null) {
                LOGGER.debug("We doesn't find a TextRange for {} in {}. We link to first line with {} confidence",
                        dependency.getFileName(), packageLock, Confidence.LOW);
                dependencyMap.put(dependency, new TextRangeConfidence(packageLock.selectLine(1), Confidence.LOW));
            }
        }
        return dependencyMap.get(dependency);
    }

    private void fillArtifactMatch(@NonNull Dependency dependency, Identifier mavenIdentifier) {
        // TODO: Implement this
    }

}
