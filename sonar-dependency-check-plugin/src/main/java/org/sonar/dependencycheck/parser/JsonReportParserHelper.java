/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2025 dependency-check
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
package org.sonar.dependencycheck.parser;

import java.io.IOException;
import java.io.InputStream;

import org.sonar.dependencycheck.parser.element.Analysis;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonReportParserHelper {

    private JsonReportParserHelper() {
        // do nothing
    }

    public static Analysis parse(InputStream inputStream) throws ReportParserException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(inputStream, Analysis.class);
        } catch (JsonParseException e) {
            throw new ReportParserException("Could not parse JSON-Report", e);
        } catch (JsonMappingException e) {
            throw new ReportParserException("Problem with JSON-Report-Mapping", e);
        } catch (IOException e) {
            throw new ReportParserException("IO Problem with JSON-Report", e);
        }
    }
}
