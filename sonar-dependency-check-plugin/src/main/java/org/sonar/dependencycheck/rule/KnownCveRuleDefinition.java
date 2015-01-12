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
package org.sonar.dependencycheck.rule;

import org.sonar.api.BatchExtension;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.dependencycheck.DependencyCheckPlugin;

public class KnownCveRuleDefinition implements RulesDefinition, BatchExtension {


    @Override
    public void define(Context context) {
        NewRepository repo = context.createRepository(DependencyCheckPlugin.REPOSITORY_KEY, DependencyCheckPlugin.LANGUAGE_KEY);
        repo.setName("Known CVEs");

        NewRule rule = repo.createRule(DependencyCheckPlugin.RULE_KEY);
        rule.addTags("cwe-937", "cve", "security", "vulnerability");
        rule.setName("Using Components with Known Vulnerabilities");
        rule.setHtmlDescription("OWASP Top 10 2013-A9: <a href=\"https://www.owasp.org/index.php/Top_10_2013-A9-Using_Components_with_Known_Vulnerabilities\">Using Components with Known Vulnerabilities</a>");
        rule.setSeverity(Severity.MAJOR);

        repo.done();
    }

}