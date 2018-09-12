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

    private final String cvssSeverity;
    private final Double critical;
    private final Double major;
    private final Double minor;
    private final Severity expectedSeverity;

    public DependencyCheckUtilsTest(String cvssSeverity, Double critical, Double major, Double minor, Severity expectedSeverity) {
        this.cvssSeverity = cvssSeverity;
        this.critical = critical;
        this.major = major;
        this.minor = minor;
        this.expectedSeverity = expectedSeverity;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> severities() {
        return Arrays.asList(new Object[][]{
                // defaults
                {"10.0", Double.valueOf("7.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.CRITICAL},
                {"7.0",  Double.valueOf("7.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.CRITICAL},
                {"6.9",  Double.valueOf("7.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.MAJOR},
                {"4.0",  Double.valueOf("7.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.MAJOR},
                {"3.9",  Double.valueOf("7.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.MINOR},
                {"0.0",  Double.valueOf("7.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.MINOR},

                // custom
                {"10.0", Double.valueOf("5.0"), Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.CRITICAL},
                {"7.0",  Double.valueOf("5.0"), Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.CRITICAL},
                {"6.9",  Double.valueOf("5.0"), Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.CRITICAL},
                {"4.0",  Double.valueOf("5.0"), Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.MAJOR},
                {"3.9",  Double.valueOf("5.0"), Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.MAJOR},
                {"1.9",  Double.valueOf("5.0"), Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.MINOR},
                {"0.0",  Double.valueOf("5.0"), Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.INFO},

                // custom, critical deactivated
                {"10.0", Double.valueOf("-1"),  Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.MAJOR},
                {"7.0",  Double.valueOf("-1"),  Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.MAJOR},
                {"6.9",  Double.valueOf("-1"),  Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.MAJOR},
                {"4.0",  Double.valueOf("-1"),  Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.MAJOR},
                {"3.9",  Double.valueOf("-1"),  Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.MAJOR},
                {"1.9",  Double.valueOf("-1"),  Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.MINOR},
                {"0.0",  Double.valueOf("-1"),  Double.valueOf("2.0"), Double.valueOf("1.0"), Severity.INFO},

                // custom, critical and major deactivated
                {"10.0", Double.valueOf("-1"),  Double.valueOf("-1"), Double.valueOf("1.0"), Severity.MINOR},
                {"7.0",  Double.valueOf("-1"),  Double.valueOf("-1"), Double.valueOf("1.0"), Severity.MINOR},
                {"6.9",  Double.valueOf("-1"),  Double.valueOf("-1"), Double.valueOf("1.0"), Severity.MINOR},
                {"4.0",  Double.valueOf("-1"),  Double.valueOf("-1"), Double.valueOf("1.0"), Severity.MINOR},
                {"3.9",  Double.valueOf("-1"),  Double.valueOf("-1"), Double.valueOf("1.0"), Severity.MINOR},
                {"1.9",  Double.valueOf("-1"),  Double.valueOf("-1"), Double.valueOf("1.0"), Severity.MINOR},
                {"0.0",  Double.valueOf("-1"),  Double.valueOf("-1"), Double.valueOf("1.0"), Severity.INFO},

                // all vulnerabilites are critical
                {"10.0", Double.valueOf("0.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.CRITICAL},
                {"7.0",  Double.valueOf("0.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.CRITICAL},
                {"6.9",  Double.valueOf("0.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.CRITICAL},
                {"4.0",  Double.valueOf("0.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.CRITICAL},
                {"3.9",  Double.valueOf("0.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.CRITICAL},
                {"1.9",  Double.valueOf("0.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.CRITICAL},
                {"0.0",  Double.valueOf("0.0"), Double.valueOf("4.0"), Double.valueOf("0.0"), Severity.CRITICAL},

                // all vulnerabilites are MAJOR, critical is deactivated
                {"10.0", Double.valueOf("-1"), Double.valueOf("0.0"), Double.valueOf("0.0"), Severity.MAJOR},
                {"7.0",  Double.valueOf("-1"), Double.valueOf("0.0"), Double.valueOf("0.0"), Severity.MAJOR},
                {"6.9",  Double.valueOf("-1"), Double.valueOf("0.0"), Double.valueOf("0.0"), Severity.MAJOR},
                {"4.0",  Double.valueOf("-1"), Double.valueOf("0.0"), Double.valueOf("0.0"), Severity.MAJOR},
                {"3.9",  Double.valueOf("-1"), Double.valueOf("0.0"), Double.valueOf("0.0"), Severity.MAJOR},
                {"1.9",  Double.valueOf("-1"), Double.valueOf("0.0"), Double.valueOf("0.0"), Severity.MAJOR},
                {"0.0",  Double.valueOf("-1"), Double.valueOf("0.0"), Double.valueOf("0.0"), Severity.MAJOR},

                // all vulnerabilites are MINOR, critical and major are deactivated
                {"10.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("0.0"), Severity.MINOR},
                {"7.0",  Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("0.0"), Severity.MINOR},
                {"6.9",  Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("0.0"), Severity.MINOR},
                {"4.0",  Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("0.0"), Severity.MINOR},
                {"3.9",  Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("0.0"), Severity.MINOR},
                {"1.9",  Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("0.0"), Severity.MINOR},
                {"0.0",  Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("0.0"), Severity.MINOR},

                // all vulnerabilities are INFO, critical, major and minor deactivated
                {"10.0", Double.valueOf("-1"),  Double.valueOf("-1"),  Double.valueOf("-1"), Severity.INFO},
                {"7.0",  Double.valueOf("-1"),  Double.valueOf("-1"),  Double.valueOf("-1"), Severity.INFO},
                {"6.9",  Double.valueOf("-1"),  Double.valueOf("-1"),  Double.valueOf("-1"), Severity.INFO},
                {"4.0",  Double.valueOf("-1"),  Double.valueOf("-1"),  Double.valueOf("-1"), Severity.INFO},
                {"3.9",  Double.valueOf("-1"),  Double.valueOf("-1"),  Double.valueOf("-1"), Severity.INFO},
                {"1.9",  Double.valueOf("-1"),  Double.valueOf("-1"),  Double.valueOf("-1"), Severity.INFO},
                {"0.0",  Double.valueOf("-1"),  Double.valueOf("-1"),  Double.valueOf("-1"), Severity.INFO}
        });
    }

    @Test
    public void testCvssToSonarQubeSeverity() {
        assertThat(DependencyCheckUtils.cvssToSonarQubeSeverity(this.cvssSeverity, this.critical, this.major, this.minor)).isEqualTo(this.expectedSeverity);
    }

}
