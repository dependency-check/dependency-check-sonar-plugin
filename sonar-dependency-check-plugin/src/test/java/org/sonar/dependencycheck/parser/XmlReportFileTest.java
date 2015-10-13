/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015 Steve Springett
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.dependencycheck.parser;

import com.google.common.io.Closeables;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.utils.MessageException;
import org.sonar.dependencycheck.DependencyCheckSensorConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XmlReportFileTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    FileSystem fs = mock(FileSystem.class);

    @Before
    public void before() throws Exception {
        when(this.fs.baseDir()).thenReturn(this.temp.newFolder());
    }

    @Test
    public void testExists() {
        DependencyCheckSensorConfiguration configuration = mock(DependencyCheckSensorConfiguration.class);
        when(configuration.getReportPath()).thenReturn("src/test/resources/report/dependency-check-report.xml");

        FileSystem fileSystem = mock(FileSystem.class);
        when(fileSystem.baseDir()).thenReturn(new File(System.getProperty("user.dir")));
        XmlReportFile xmlReportFile = new XmlReportFile(configuration, fileSystem);

        assertTrue(xmlReportFile.exist());
    }

    @Test
    public void testNotExists() {
        DependencyCheckSensorConfiguration configuration = mock(DependencyCheckSensorConfiguration.class);
        when(configuration.getReportPath()).thenReturn(null);

        XmlReportFile xmlReportFile = new XmlReportFile(configuration, this.fs);

        assertFalse(xmlReportFile.exist());
    }

    @Test
    public void testWrongPathExists() {
        DependencyCheckSensorConfiguration configuration = mock(DependencyCheckSensorConfiguration.class);
        when(configuration.getReportPath()).thenReturn("/do/not/exist/dependency-check-report.xml");

        XmlReportFile xmlReportFile = new XmlReportFile(configuration, this.fs);

        assertFalse(xmlReportFile.exist());
    }

    @Test
    public void testDirExists() {
        DependencyCheckSensorConfiguration configuration = mock(DependencyCheckSensorConfiguration.class);
        when(configuration.getReportPath()).thenReturn(System.getProperty("user.dir"));

        XmlReportFile xmlReportFile = new XmlReportFile(configuration, this.fs);

        assertFalse(xmlReportFile.exist());
    }

    @Test
    public void testNotExistgetInputStream() throws IOException {
        DependencyCheckSensorConfiguration configuration = mock(DependencyCheckSensorConfiguration.class);
        when(configuration.getReportPath()).thenReturn(null);

        XmlReportFile xmlReportFile = new XmlReportFile(configuration, this.fs);

        InputStream input = null;
        try {
            input = xmlReportFile.getInputStream();
            fail("An exception is expected!");
        } catch (FileNotFoundException e) {
            // expected
        } finally {
            Closeables.closeQuietly(input);
        }
    }

    @Test
    public void testInputStream() throws IOException {
        DependencyCheckSensorConfiguration configuration = mock(DependencyCheckSensorConfiguration.class);
        when(configuration.getReportPath()).thenReturn(System.getProperty("user.dir") + File.separator + "src/test/resources/report/dependency-check-report.xml");

        XmlReportFile xmlReportFile = new XmlReportFile(configuration, this.fs);

        InputStream input = null;
        try {
            input = xmlReportFile.getInputStream();
        } finally {
            Closeables.closeQuietly(input);
        }
    }
}
