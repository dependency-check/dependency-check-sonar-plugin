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
package org.sonar.dependencycheck.base;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sonar.api.batch.rule.Severity;

import java.util.Arrays;
import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(Parameterized.class)
public class DependencyCheckUtilsTest {

    private final Float cvssSeverity;
    private final Float blocker;
    private final Float critical;
    private final Float major;
    private final Float minor;
    private final Severity expectedSeverity;

    public DependencyCheckUtilsTest(Float cvssSeverity, Float blocker, Float critical, Float major, Float minor, Severity expectedSeverity) {
        this.cvssSeverity = cvssSeverity;
        this.blocker = blocker;
        this.critical = critical;
        this.major = major;
        this.minor = minor;
        this.expectedSeverity = expectedSeverity;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> severities() {
        return Arrays.asList(new Object[][]{
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

    @Test
    public void testCvssToSonarQubeSeverity() {
        assertThat(DependencyCheckUtils.cvssToSonarQubeSeverity(this.cvssSeverity, this.blocker, this.critical, this.major, this.minor)).isEqualTo(this.expectedSeverity);
    }

}
