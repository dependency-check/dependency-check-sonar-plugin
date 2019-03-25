/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2017 Steve Springett
 * steve.springett@owasp.org
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.dependencycheck.base.DependencyCheckConstants;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.parser.element.Confidence;
import org.sonar.dependencycheck.parser.element.Dependency;
import org.sonar.dependencycheck.parser.element.Identifier;
import org.sonar.dependencycheck.parser.element.ProjectInfo;
import org.sonar.dependencycheck.parser.element.ScanInfo;
import org.sonar.dependencycheck.parser.element.Vulnerability;

public class DependencyReasonSearcherTest {

    private static final File TEST_DIR = new File("src/test/resources/reason");

    private DefaultInputFile inputFile(String fileName) throws IOException {
        File file = new File(TEST_DIR, fileName);
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        return TestInputFileBuilder.create("key", fileName).setModuleBaseDir(Paths.get(TEST_DIR.getAbsolutePath()))
                .setType(InputFile.Type.MAIN).setLanguage("mytest").setCharset(StandardCharsets.UTF_8)
                .initMetadata(content).build();
    }

    @Test
    public void checkForDependencyReasons() throws IOException  {
        SensorContextTester context = SensorContextTester.create(new File(""));
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.REPORT_PATH_PROPERTY, "dependency-check-report.xml");
        context.setSettings(settings);
        context.fileSystem().add(inputFile("pom.xml"));
        context.fileSystem().add(inputFile("build.gradle"));
        DependencyReasonSearcher searcher = new DependencyReasonSearcher(context);
        ScanInfo scanInfo = new ScanInfo();
        ProjectInfo projectInfo = new ProjectInfo();
        Collection<Dependency> dependencies = new LinkedList<>();
        Analysis analysis = new Analysis(scanInfo, projectInfo, dependencies);
        searcher.addDependenciesToProjectConfigurationFiles(analysis, context);
        assertEquals(2, searcher.getDependencyreasons().size());
    }

    @Test
    public void checkForDependencyReasonsMaven() throws IOException  {
        SensorContextTester context = SensorContextTester.create(new File(""));
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.REPORT_PATH_PROPERTY, "dependency-check-report.xml");
        context.setSettings(settings);
        context.fileSystem().add(inputFile("pom.xml"));
        DependencyReasonSearcher searcher = new DependencyReasonSearcher(context);

        ScanInfo scanInfo = new ScanInfo();
        ProjectInfo projectInfo = new ProjectInfo();
        Collection<Dependency> dependencies = new LinkedList<>();
        // First Identifier
        Identifier identifier1 = new Identifier();
        identifier1.setName("struts:struts:1.2.8");
        identifier1.setConfidence(Confidence.HIGHEST);
        identifier1.setType("maven");
        Collection<Identifier> identifiersCollected1 = new ArrayList<>();
        identifiersCollected1.add(identifier1);
        Vulnerability vulnerability1 = new Vulnerability();
        vulnerability1.setCvssScore(5.0f);
        vulnerability1.setDescription("Test description");
        vulnerability1.setName("Test name");
        Dependency dependency1 = new Dependency();
        List<Vulnerability> vulnerabilities1 = new ArrayList<>();
        vulnerabilities1.add(vulnerability1);
        dependency1.setIdentifiersCollected(identifiersCollected1);
        dependency1.setVulnerabilities(vulnerabilities1);
        // Second Identifier
        Identifier identifier2 = new Identifier();
        identifier2.setName("org.springframework:spring:2.0.8");
        identifier2.setConfidence(Confidence.HIGHEST);
        identifier2.setType("maven");
        Collection<Identifier> identifiersCollected2 = new ArrayList<>();
        identifiersCollected2.add(identifier2);
        Vulnerability vulnerability2 = new Vulnerability();
        vulnerability2.setCvssScore(5.0f);
        vulnerability2.setDescription("Test description");
        vulnerability2.setName("Test name");
        Dependency dependency2 = new Dependency();
        List<Vulnerability> vulnerabilities2 = new ArrayList<>();
        vulnerabilities2.add(vulnerability2);
        dependency2.setIdentifiersCollected(identifiersCollected2);
        dependency2.setVulnerabilities(vulnerabilities2);

        // Add dependencies
        dependencies.add(dependency1);
        dependencies.add(dependency2);

        Analysis analysis = new Analysis(scanInfo, projectInfo, dependencies);
        searcher.addDependenciesToProjectConfigurationFiles(analysis, context);
        searcher.saveMeasures(context);
        assertEquals(2, context.allIssues().size());
        assertEquals(1, searcher.getDependencyreasons().size());

        assertTrue(context.allIssues().stream().anyMatch(i -> i.primaryLocation().textRange().start().line() == 53));
        assertTrue(context.allIssues().stream().anyMatch(i -> i.primaryLocation().textRange().start().line() == 43));
    }

    @Test
    public void checkForDependencyReasonsGradle() throws IOException  {
        SensorContextTester context = SensorContextTester.create(new File(""));
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.REPORT_PATH_PROPERTY, "dependency-check-report.xml");
        settings.setProperty(DependencyCheckConstants.SUMMARIZE_PROPERTY, Boolean.TRUE);
        context.setSettings(settings);
        context.fileSystem().add(inputFile("build.gradle"));
        DependencyReasonSearcher searcher = new DependencyReasonSearcher(context);

        ScanInfo scanInfo = new ScanInfo();
        ProjectInfo projectInfo = new ProjectInfo();
        Collection<Dependency> dependencies = new LinkedList<>();
        // Second Identifier
        Identifier identifier = new Identifier();
        identifier.setName("org.springframework:spring:2.0");
        identifier.setConfidence(Confidence.HIGHEST);
        identifier.setType("maven");
        Collection<Identifier> identifiersCollected = new ArrayList<>();
        identifiersCollected.add(identifier);
        Vulnerability vulnerability = new Vulnerability();
        vulnerability.setCvssScore(5.0f);
        vulnerability.setDescription("Test description");
        vulnerability.setName("Test name");
        Dependency dependency = new Dependency();
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        vulnerabilities.add(vulnerability);
        dependency.setIdentifiersCollected(identifiersCollected);
        dependency.setVulnerabilities(vulnerabilities);

        // Add dependencies
        dependencies.add(dependency);

        Analysis analysis = new Analysis(scanInfo, projectInfo, dependencies);
        searcher.addDependenciesToProjectConfigurationFiles(analysis, context);
        searcher.saveMeasures(context);
        assertEquals(1, context.allIssues().size());
        assertEquals(1, searcher.getDependencyreasons().size());
        assertTrue(context.allIssues().stream().anyMatch(i -> i.primaryLocation().textRange().start().line() == 24));
    }

}
