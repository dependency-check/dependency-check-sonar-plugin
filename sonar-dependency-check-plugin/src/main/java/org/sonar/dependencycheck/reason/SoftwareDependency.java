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

import java.util.Optional;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class SoftwareDependency {
    private final String name;
    private final Optional<String> version;
    /**
     * @param version
     * @param name
     */
    public SoftwareDependency(@NonNull String name, @Nullable String version) {
        super();
        this.name = name;
        this.version = Optional.ofNullable(version);
    }
    /**
     * @return the name
     */
    @NonNull
    public String getName() {
        return name;
    }
    /**
     * @return the version
     */
    public Optional<String> getVersion() {
        return version;
    }

}
