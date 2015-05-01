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
package org.sonar.dependencycheck.ui;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class DependencyCheckWidgetTest {

    @Test
    public void test_rails_template() throws Exception {
        DependencyCheckWidget widget = new DependencyCheckWidget();
        assertThat(widget.getClass().getResource(widget.getTemplatePath()))
                .as("Template not found: " + widget.getTemplatePath())
                .isNotNull();
    }

    @Test
    public void test_metadata() throws Exception {
        DependencyCheckWidget widget = new DependencyCheckWidget();
        assertThat(widget.getId()).containsIgnoringCase("dependencycheck");
        assertThat(widget.getTitle()).contains("Known Vulnerabilities");
    }

    public static void main(String args[]) {
        double high = 4;
        double med = 21;
        double low = 1;
        double total = high + med + low;
        double vulndeps = 8;
        double totaldeps = 100; // dont think this is useful in calculation



        double nvs = (((high * 5) + (med * 3) + (low * 1)));


        //double nvs = (((high * 5) + (med * 3) + (low * 1)) * (vulndeps / totaldeps));
        //double nvs = ((high * 1) + (med * .5) + (low * .1)) / total;
        //double nvs = (5 - ((high * 1) + (med * .5) + (low * .1)) * ((vulndeps / totaldeps) / total) * 5);
        double worst_possible_score = (total * 1);

        //double risk_ratio = ((total * 5) / nvs) - 1.0;


        double risk_ratio = ( (total / nvs) );


        double test = ((high + med) / total);




        System.out.println("NVS: " + nvs);
        System.out.println("WPS: " + worst_possible_score);
        System.out.println("Risk Ratio: " + risk_ratio);

        System.out.println("Test: " + test);

    }

}
