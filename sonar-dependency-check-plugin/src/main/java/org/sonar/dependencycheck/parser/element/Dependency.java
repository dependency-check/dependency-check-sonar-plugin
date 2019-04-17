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
package org.sonar.dependencycheck.parser.element;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;

public class Dependency {

    private final String fileName;
    private final String filePath;
    private final String md5Hash;
    private final String sha1Hash;
    private final Collection<Evidence> evidenceCollected;
    private final Collection<Identifier> identifiersCollected;
    private final List<Vulnerability> vulnerabilities;

    public Dependency(@NonNull String fileName, @NonNull String filePath, @NonNull String md5Hash, @NonNull String sha1Hash, Collection<Evidence> evidenceCollected, Collection<Identifier> identifiersCollected, List<Vulnerability> vulnerabilities) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.md5Hash = md5Hash;
        this.sha1Hash = sha1Hash;
        this.evidenceCollected = evidenceCollected;
        this.identifiersCollected = identifiersCollected;
        this.vulnerabilities = vulnerabilities;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public String getSha1Hash() {
        return sha1Hash;
    }

    public Collection<Evidence> getEvidenceCollected() {
        return evidenceCollected;
    }

    public List<Vulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    public Collection<Identifier> getIdentifiersCollected() {
        return identifiersCollected;
    }

    public void sortVulnerabilityBycvssScore() {
        final Comparator<Vulnerability> comp = (vul1, vul2) -> Float.compare( vul1.getCvssScore(), vul2.getCvssScore());
        Collections.sort(this.vulnerabilities, comp.reversed());
    }

}
