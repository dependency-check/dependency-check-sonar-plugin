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

public class MavenDependencyReasonTest {

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
        MavenDependencyReason maven = new MavenDependencyReason(inputFile("pom.xml"));
        assertTrue(maven.isReasonable());
        assertNotNull(maven.getInputComponent());
    }

    @Test
    public void isReasonableWithoutContent() throws IOException {
        DefaultInputFile pom = mock(DefaultInputFile.class, RETURNS_DEEP_STUBS);
        when(pom.contents()).thenReturn("");
        MavenDependencyReason maven = new MavenDependencyReason(pom);
        assertFalse(maven.isReasonable());
        assertNotNull(maven.getInputComponent());
    }

    @Test
    public void constructorWithIOException() throws IOException {
        DefaultInputFile pom = mock(DefaultInputFile.class, RETURNS_DEEP_STUBS);
        when(pom.contents()).thenThrow(new IOException());
        MavenDependencyReason maven = new MavenDependencyReason(pom);
        assertFalse(maven.isReasonable());
        assertNotNull(maven.getInputComponent());
    }

    @Test
    public void foundDependency() throws IOException {
        MavenDependencyReason maven = new MavenDependencyReason(inputFile("pom.xml"));
        // Create Dependency
        Identifier identifier1 = new Identifier("pkg:maven/struts/struts@1.2.8", Confidence.HIGHEST);
        Collection<Identifier> packageidentifiers1 = new ArrayList<>();
        packageidentifiers1.add(identifier1);
        Dependency dependency = new Dependency(null, null, null, null, Collections.emptyMap(),Collections.emptyList(), packageidentifiers1, Collections.emptyList());
        assertTrue(maven.isReasonable());
        assertNotNull(maven.getBestTextRange(dependency));
        // verify that same dependency points to the same TextRange, use of HashMap
        assertEquals(maven.getBestTextRange(dependency), maven.getBestTextRange(dependency));
        assertEquals(43, maven.getBestTextRange(dependency).getTextrange().start().line());
    }

    @Test
    public void foundNoDependency() throws IOException {
        MavenDependencyReason maven = new MavenDependencyReason(inputFile("pom.xml"));
        // Create Dependency
        Identifier identifier1 = new Identifier("pkg:maven/myvendor/myartefact@1.2.8", Confidence.HIGHEST);
        Collection<Identifier> packageidentifiers1 = new ArrayList<>();
        packageidentifiers1.add(identifier1);
        Dependency dependency = new Dependency(null, null, null, null, Collections.emptyMap(),Collections.emptyList(), packageidentifiers1, Collections.emptyList());
        TextRangeConfidence textRangeConfidence = maven.getBestTextRange(dependency);
        // Check for default location, first line in file with low confidence
        assertNotNull(textRangeConfidence);
        assertEquals(1, textRangeConfidence.getTextrange().start().line());
        assertEquals(Confidence.LOW, textRangeConfidence.getConfidence());
        // verify that same dependency points to the same TextRange, use of HashMap
        assertEquals(maven.getBestTextRange(dependency), maven.getBestTextRange(dependency));
        assertEquals(1, maven.getBestTextRange(dependency).getTextrange().start().line());
    }
}
