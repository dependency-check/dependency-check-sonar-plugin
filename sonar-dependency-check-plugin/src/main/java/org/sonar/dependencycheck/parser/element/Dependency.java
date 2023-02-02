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
package org.sonar.dependencycheck.parser.element;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonar.api.config.Configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

@JsonIgnoreProperties({"isVirtual", "sha256", "description", "projectReferences", "license", "relatedDependencies", "suppressedVulnerabilities", "suppressedVulnerabilityIds"})
public class Dependency {

    private final String fileName;
    private final String filePath;
    private final Optional<String> md5;
    private final Optional<String> sha1;
    private final Map<String, List<Evidence>> evidenceCollected;
    private final List<Vulnerability> vulnerabilities;
    private final Optional<Collection<Identifier>> packages;
    private final Optional<Collection<Identifier>> vulnerabilityIds;

    @JsonCreator
    public Dependency(@JsonProperty(value = "fileName", required = true) @NonNull String fileName,
                      @JsonProperty(value = "filePath", required = true) @NonNull String filePath,
                      @JsonProperty(value = "md5") @Nullable String md5Hash,
                      @JsonProperty(value = "sha1") @Nullable String sha1Hash,
                      @JsonProperty(value = "evidenceCollected") Map<String, List<Evidence>> evidenceCollected,
                      @JsonProperty(value = "vulnerabilities") List<Vulnerability> vulnerabilities,
                      @JsonProperty(value = "packages") @Nullable Collection<Identifier> packages,
                      @JsonProperty(value = "vulnerabilityIds") @Nullable Collection<Identifier> vulnerabilityIds)
                      {
        this.fileName = fileName;
        this.filePath = filePath;
        this.md5 = Optional.ofNullable(md5Hash);
        this.sha1 = Optional.ofNullable(sha1Hash);
        this.evidenceCollected = evidenceCollected;
        this.vulnerabilities = vulnerabilities;
        this.packages = Optional.ofNullable(packages);
        this.vulnerabilityIds = Optional.ofNullable(vulnerabilityIds);
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public Optional<String> getMd5Hash() {
        return md5;
    }

    public Optional<String> getSha1Hash() {
        return sha1;
    }

    public Map<String, List<Evidence>> getEvidenceCollected() {
        return evidenceCollected;
    }

    public List<Vulnerability> getVulnerabilities() {
        return Optional.ofNullable(vulnerabilities).orElse(Collections.emptyList());
    }

    public void sortVulnerabilityBycvssScore(Configuration config) {
        final Comparator<Vulnerability> comp = (vul1, vul2) -> Float.compare( vul1.getCvssScore(config), vul2.getCvssScore(config));
        Collections.sort(this.vulnerabilities, comp.reversed());
    }

    public Optional<Collection<Identifier>> getPackages() {
        return packages;
    }

    public Optional<Collection<Identifier>> getVulnerabilityIds() {
        return vulnerabilityIds;
    }

    public boolean isJavaDependency() {
        if (packages.isPresent()) {
            for (Identifier identifier : packages.get()) {
                if (Identifier.isMavenPackage(identifier)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isJavaScriptDependency() {
        if (packages.isPresent()) {
            for (Identifier identifier : packages.get()) {
                if (Identifier.isNPMPackage(identifier) || Identifier.isJavaScriptPackage(identifier)) {
                    return true;
                }
            }
        }
        return false;
    }
}
