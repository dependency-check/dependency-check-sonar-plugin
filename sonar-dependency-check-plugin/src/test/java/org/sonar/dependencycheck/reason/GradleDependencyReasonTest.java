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

package org.sonar.dependencycheck.reason;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Identifier;

public class GradleDependencyReasonTest {

    private static final File TEST_DIR = new File("src/test/resources/reason");

    private DefaultInputFile inputFile(String fileName) throws IOException {
        File file = new File(TEST_DIR, fileName);
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        return TestInputFileBuilder.create("key", fileName).setModuleBaseDir(Paths.get(TEST_DIR.getAbsolutePath()))
                .setType(InputFile.Type.MAIN).setLanguage("mytest").setCharset(StandardCharsets.UTF_8)
                .initMetadata(content).build();
    }

    @Test
    public void isReasonable() throws IOException {
        GradleDependencyReason gradle = new GradleDependencyReason(inputFile("build.gradle"));
        assertTrue(gradle.isReasonable());
        assertNotNull(gradle.getInputComponent());
    }

    @Test
    public void isReasonableWithoutContent() throws IOException {
        DefaultInputFile gradlefile = mock(DefaultInputFile.class, RETURNS_DEEP_STUBS);
        when(gradlefile.contents()).thenReturn("");
        GradleDependencyReason gradle = new GradleDependencyReason(gradlefile);
        assertFalse(gradle.isReasonable());
        assertNotNull(gradle.getInputComponent());
    }

    @Test
    public void constructorWithIOException() throws IOException {
        DefaultInputFile gradlefile = mock(DefaultInputFile.class, RETURNS_DEEP_STUBS);
        when(gradlefile.contents()).thenThrow(new IOException());
        GradleDependencyReason gradle = new GradleDependencyReason(gradlefile);
        assertFalse(gradle.isReasonable());
        assertNotNull(gradle.getInputComponent());
    }

    @Test
    public void foundDependency() throws IOException {
        GradleDependencyReason gradle = new GradleDependencyReason(inputFile("build.gradle"));
        // Create Dependency
        Identifier identifier = new Identifier("pkg:maven/org.springframework/spring@2.0", Confidence.HIGHEST);
        Collection<Identifier> identifiersCollected = new ArrayList<>();
        identifiersCollected.add(identifier);
        Dependency dependency = new Dependency(null, null, null, null, Collections.emptyMap(),Collections.emptyList(), identifiersCollected, Collections.emptyList());
        assertNotNull(gradle.getBestTextRange(dependency));
        // verify that same dependency points to the same TextRange, use of HashMap
        assertEquals(gradle.getBestTextRange(dependency), gradle.getBestTextRange(dependency));
        assertEquals(24, gradle.getBestTextRange(dependency).getTextrange().start().line());
    }

    @Test
    public void foundNoDependency() throws IOException {
        GradleDependencyReason gradle = new GradleDependencyReason(inputFile("build.gradle"));
        // Create Dependency
        Identifier identifier = new Identifier("pkg:maven/myvendor/myartefact@2.0", Confidence.HIGHEST);
        Collection<Identifier> identifiersCollected = new ArrayList<>();
        identifiersCollected.add(identifier);
        Dependency dependency = new Dependency(null, null, null, null, Collections.emptyMap(),Collections.emptyList(), identifiersCollected, Collections.emptyList());
        assertNotNull(gradle.getBestTextRange(dependency));
        // verify that same dependency points to the same TextRange, use of HashMap
        assertEquals(gradle.getBestTextRange(dependency), gradle.getBestTextRange(dependency));
        assertEquals(1, gradle.getBestTextRange(dependency).getTextrange().start().line());
    }
}
