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
package org.sonar.dependencycheck.base;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.rule.Severity;

public class DependencyCheckUtilsTest {

    public static Stream<Object[]> severities() {
        return Stream.of(new Object[][]{
                // defaults
                {Float.valueOf("10.0"), Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.BLOCKER},
                {Float.valueOf("7.0"),  Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.CRITICAL},
                {Float.valueOf("6.9"),  Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.MAJOR},
                {Float.valueOf("4.0"),  Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"),  Severity.MAJOR},
                {Float.valueOf("3.9"),  Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.MINOR},
                {Float.valueOf("0.0"),  Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.MINOR},

                // custom
                {Float.valueOf("10.0"), Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.BLOCKER},
                {Float.valueOf("9.0"),  Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.BLOCKER},
                {Float.valueOf("7.0"),  Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.CRITICAL},
                {Float.valueOf("6.9"),  Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.CRITICAL},
                {Float.valueOf("4.0"),  Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MAJOR},
                {Float.valueOf("3.9"),  Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MAJOR},
                {Float.valueOf("1.9"),  Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MINOR},
                {Float.valueOf("0.0"),  Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.INFO},

                // custom, blocker deactivated
                {Float.valueOf("10.0"), Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.CRITICAL},
                {Float.valueOf("9.0"),  Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.CRITICAL},
                {Float.valueOf("7.0"),  Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.CRITICAL},
                {Float.valueOf("6.9"),  Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.CRITICAL},
                {Float.valueOf("4.0"),  Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MAJOR},
                {Float.valueOf("3.9"),  Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MAJOR},
                {Float.valueOf("1.9"),  Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MINOR},
                {Float.valueOf("0.0"),  Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.INFO},

                // custom, critical deactivated
                {Float.valueOf("10.0"), Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MAJOR},
                {Float.valueOf("7.0"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MAJOR},
                {Float.valueOf("6.9"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MAJOR},
                {Float.valueOf("4.0"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MAJOR},
                {Float.valueOf("3.9"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MAJOR},
                {Float.valueOf("1.9"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.MINOR},
                {Float.valueOf("0.0"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Severity.INFO},

                // custom, critical and major deactivated
                {Float.valueOf("10.0"), Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Severity.MINOR},
                {Float.valueOf("7.0"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Severity.MINOR},
                {Float.valueOf("6.9"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Severity.MINOR},
                {Float.valueOf("4.0"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Severity.MINOR},
                {Float.valueOf("3.9"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Severity.MINOR},
                {Float.valueOf("1.9"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Severity.MINOR},
                {Float.valueOf("0.0"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Severity.INFO},

                // all vulnerabilites are critical
                {Float.valueOf("10.0"), Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.CRITICAL},
                {Float.valueOf("7.0"),  Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.CRITICAL},
                {Float.valueOf("6.9"),  Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.CRITICAL},
                {Float.valueOf("4.0"),  Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.CRITICAL},
                {Float.valueOf("3.9"),  Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.CRITICAL},
                {Float.valueOf("1.9"),  Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.CRITICAL},
                {Float.valueOf("0.0"),  Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Severity.CRITICAL},

                // all vulnerabilites are MAJOR, critical and blocker is deactivated
                {Float.valueOf("10.0"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("0.0"), Severity.MAJOR},
                {Float.valueOf("7.0"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("0.0"), Severity.MAJOR},
                {Float.valueOf("6.9"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("0.0"), Severity.MAJOR},
                {Float.valueOf("4.0"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("0.0"), Severity.MAJOR},
                {Float.valueOf("3.9"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("0.0"), Severity.MAJOR},
                {Float.valueOf("1.9"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("0.0"), Severity.MAJOR},
                {Float.valueOf("0.0"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Float.valueOf("0.0"), Severity.MAJOR},

                // all vulnerabilites are MINOR, blocker, critical  and major are deactivated
                {Float.valueOf("10.0"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Severity.MINOR},
                {Float.valueOf("7.0"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Severity.MINOR},
                {Float.valueOf("6.9"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Severity.MINOR},
                {Float.valueOf("4.0"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Severity.MINOR},
                {Float.valueOf("3.9"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Severity.MINOR},
                {Float.valueOf("1.9"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Severity.MINOR},
                {Float.valueOf("0.0"),  Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0"), Severity.MINOR},

                // all vulnerabilities are INFO, blocker, critical, major and minor deactivated
                {Float.valueOf("10.0"), Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"),  Float.valueOf("-1"), Severity.INFO},
                {Float.valueOf("7.0"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"),  Float.valueOf("-1"), Severity.INFO},
                {Float.valueOf("6.9"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"),  Float.valueOf("-1"), Severity.INFO},
                {Float.valueOf("4.0"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"),  Float.valueOf("-1"), Severity.INFO},
                {Float.valueOf("3.9"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"),  Float.valueOf("-1"), Severity.INFO},
                {Float.valueOf("1.9"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"),  Float.valueOf("-1"), Severity.INFO},
                {Float.valueOf("0.0"),  Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"),  Float.valueOf("-1"), Severity.INFO}
        });
    }

    @ParameterizedTest(name = "{index} => cvssSeverity={0}, blocker={1}, critical={2}, major={3}, minor={4}, expectedSeverity={5}")
    @MethodSource("severities")
    public void testCvssToSonarQubeSeverity(Float cvssSeverity, Float blocker, Float critical, Float major, Float minor, Severity expectedSeverity) {
        assertEquals(expectedSeverity, DependencyCheckUtils.cvssToSonarQubeSeverity(cvssSeverity, blocker, critical, major, minor));
    }

    public static Stream<Object[]> severitiestoscore() {
        return Stream.of(new Object[][]{
                // defaults
                {"critical", Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Float.valueOf("9.0")},
                {"high",     Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Float.valueOf("7.0")},
                {"moderate", Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Float.valueOf("4.0")},
                {"medium",   Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Float.valueOf("4.0")},
                {"low",      Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Float.valueOf("0.0")},
                {"dummy",    Float.valueOf("9.0"), Float.valueOf("7.0"), Float.valueOf("4.0"), Float.valueOf("0.0"), Float.valueOf("0.0")},

                // custom
                {"critical", Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("9.0")},
                {"high",     Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("5.0")},
                {"medium",   Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("2.0")},
                {"low",      Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("1.0")},
                {"info",     Float.valueOf("9.0"), Float.valueOf("5.0"), Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("0.0")},

                // custom, blocker deactivated
                {"critical", Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("5.0")},
                {"high",     Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("5.0")},
                {"medium",   Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("2.0")},
                {"low",      Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("1.0")},
                {"info",     Float.valueOf("-1"), Float.valueOf("5.0"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("0.0")},

                // custom, blocker, critical deactivated
                {"critical", Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("2.0")},
                {"high",     Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("2.0")},
                {"medium",   Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("2.0")},
                {"low",      Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("1.0")},
                {"info",     Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("2.0"), Float.valueOf("1.0"), Float.valueOf("0.0")},

                // custom, blocker, critical and major deactivated
                {"critical", Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Float.valueOf("1.0")},
                {"high",     Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Float.valueOf("1.0")},
                {"medium",   Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Float.valueOf("1.0")},
                {"low",      Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Float.valueOf("1.0")},
                {"info",     Float.valueOf("-1"), Float.valueOf("-1"),  Float.valueOf("-1"), Float.valueOf("1.0"), Float.valueOf("0.0")},

                // custom, blocker, critical, major and minor deactivated
                {"critical", Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0")},
                {"high",     Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0")},
                {"medium",   Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0")},
                {"low",      Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0")},
                {"info",     Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("-1"), Float.valueOf("0.0")},
        });
    }
    @ParameterizedTest(name = "{index} => severity={0}, blocker={1}, critical={2}, major={3}, minor={4}, expectedScore={5}")
    @MethodSource("severitiestoscore")
    public void testSeverityToScore(String severity, Float blocker, Float critical, Float major, Float minor, Float expectedScore) {
        assertEquals(expectedScore, DependencyCheckUtils.severityToScore(severity, blocker, critical, major, minor));
    }
}
