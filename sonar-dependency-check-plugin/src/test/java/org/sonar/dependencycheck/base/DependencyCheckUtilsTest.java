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
	private final Double blocker;
	private final Double critical;
	private final Double major;
	private final Severity expectedSeverity;

	public DependencyCheckUtilsTest(String cvssSeverity, Double blocker, Double critical, Double major, Severity expectedSeverity) {
		this.cvssSeverity = cvssSeverity;
		this.blocker = blocker;
		this.critical = critical;
		this.major = major;
		this.expectedSeverity = expectedSeverity;
	}

	@Parameterized.Parameters
	public static Collection<Object[]> severities() {
		return Arrays.asList(new Object[][] {
			// defaults
			{ "10.0", Double.valueOf("9.0"), Double.valueOf("7.0"), Double.valueOf("4.0"), Severity.BLOCKER },
			{ "9.0", Double.valueOf("9.0"), Double.valueOf("7.0"), Double.valueOf("4.0"), Severity.BLOCKER },
			{ "7.0", Double.valueOf("9.0"), Double.valueOf("7.0"), Double.valueOf("4.0"), Severity.CRITICAL },
			{ "6.9", Double.valueOf("9.0"), Double.valueOf("7.0"), Double.valueOf("4.0"), Severity.MAJOR },
			{ "4.0", Double.valueOf("9.0"), Double.valueOf("7.0"), Double.valueOf("4.0"), Severity.MAJOR },
			{ "3.9", Double.valueOf("9.0"), Double.valueOf("7.0"), Double.valueOf("4.0"), Severity.MINOR },
			{ "0.0", Double.valueOf("9.0"), Double.valueOf("7.0"), Double.valueOf("4.0"), Severity.MINOR },

			// custom
			{ "10.0", Double.valueOf("9.0"), Double.valueOf("5.0"), Double.valueOf("2.0"), Severity.BLOCKER },
			{ "9.0", Double.valueOf("9.0"), Double.valueOf("5.0"), Double.valueOf("2.0"), Severity.BLOCKER },
			{ "7.0", Double.valueOf("9.0"), Double.valueOf("5.0"), Double.valueOf("2.0"), Severity.CRITICAL },
			{ "6.9", Double.valueOf("9.0"), Double.valueOf("5.0"), Double.valueOf("2.0"), Severity.CRITICAL },
			{ "4.0", Double.valueOf("9.0"), Double.valueOf("5.0"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "3.9", Double.valueOf("9.0"), Double.valueOf("5.0"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "1.9", Double.valueOf("9.0"), Double.valueOf("5.0"), Double.valueOf("2.0"), Severity.MINOR },
			{ "0.0", Double.valueOf("9.0"), Double.valueOf("5.0"), Double.valueOf("2.0"), Severity.MINOR },

			// custom, blocker deactivated
			{ "10.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "9.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "7.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "6.9", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "4.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "3.9", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "1.9", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MINOR },
			{ "0.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MINOR },

			// custom, critical deactivated
			{ "10.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "9.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "7.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "6.9", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "4.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "3.9", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MAJOR },
			{ "1.9", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MINOR },
			{ "0.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("2.0"), Severity.MINOR },

			// custom, critical and major deactivated
			{ "10.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("-1"), Severity.MINOR },
			{ "9.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("-1"), Severity.MINOR },
			{ "7.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("-1"), Severity.MINOR },
			{ "6.9", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("-1"), Severity.MINOR },
			{ "4.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("-1"), Severity.MINOR },
			{ "3.9", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("-1"), Severity.MINOR },
			{ "1.9", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("-1"), Severity.MINOR },
			{ "0.0", Double.valueOf("-1"), Double.valueOf("-1"), Double.valueOf("-1"), Severity.MINOR } });
	}

	@Test
	public void testCvssToSonarQubeSeverity() {
		assertThat(DependencyCheckUtils.cvssToSonarQubeSeverity(this.cvssSeverity, this.blocker, this.critical, this.major))
			.isEqualTo(this.expectedSeverity);
	}

}
