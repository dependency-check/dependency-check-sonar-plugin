/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2019 dependency-check
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
package org.sonar.dependencycheck;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.dependencycheck.base.DependencyCheckMetrics;
import org.sonar.dependencycheck.base.DependencyCheckUtils;
import org.sonar.dependencycheck.parser.JsonReportParserHelper;
import org.sonar.dependencycheck.parser.ReportParserException;
import org.sonar.dependencycheck.parser.XMLReportParserHelper;
import org.sonar.dependencycheck.parser.element.Analysis;
import org.sonar.dependencycheck.reason.DependencyReasonSearcher;
import org.sonar.dependencycheck.report.HtmlReportFile;
import org.sonar.dependencycheck.report.JsonReportFile;
import org.sonar.dependencycheck.report.XmlReportFile;

public class DependencyCheckSensor implements ProjectSensor {

    private static final Logger LOGGER = Loggers.get(DependencyCheckSensor.class);
    private static final String SENSOR_NAME = "Dependency-Check";
    private static final String XSD = "https://jeremylong.github.io/DependencyCheck/dependency-check.2.3.xsd";

    private final FileSystem fileSystem;
    private final PathResolver pathResolver;

    public DependencyCheckSensor(FileSystem fileSystem, PathResolver pathResolver) {
        this.fileSystem = fileSystem;
        this.pathResolver = pathResolver;
    }

    private Optional<Analysis> parseAnalysis(SensorContext context) {
        LOGGER.info("Using JSON-Reportparser");
        try {
            JsonReportFile report = JsonReportFile.getJsonReport(context.config(), fileSystem, pathResolver);
            return Optional.of(JsonReportParserHelper.parse(report.getInputStream()));
        } catch (FileNotFoundException e) {
            LOGGER.info("JSON-Analysis skipped/aborted due to missing report file");
            LOGGER.debug(e.getMessage(), e);
        }catch (ReportParserException e) {
            LOGGER.warn("JSON-Analysis aborted");
            LOGGER.debug(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.warn("JSON-Analysis aborted due to: IO Errors", e);
        }
        LOGGER.info("Using XML-Reportparser");
        XmlReportFile report;
        try {
            report = XmlReportFile.getXmlReport(context.config(), fileSystem, pathResolver);
            return Optional.of(XMLReportParserHelper.parse(report.getInputStream()));
        } catch (FileNotFoundException e) {
            LOGGER.info("XML-Analysis skipped/aborted due to missing report file");
            LOGGER.debug(e.getMessage(), e);
        } catch (ReportParserException e) {
            LOGGER.warn("XML-Analysis aborted due to: Mandatory elements are missing. Plugin is compatible to {}", XSD);
            LOGGER.debug(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.warn("XML-Analysis aborted due to: IO Errors", e);
        }
        return Optional.empty();
    }

    private void uploadHTMLReport (SensorContext context){
        try {
            HtmlReportFile htmlReportFile = HtmlReportFile.getHtmlReport(context.config(), fileSystem, pathResolver);
            String htmlReport = htmlReportFile.getReportContent();
            if (htmlReport != null) {
                LOGGER.info("Upload Dependency-Check HTML-Report");
                context.<String>newMeasure().forMetric(DependencyCheckMetrics.REPORT).on(context.project()).withValue(htmlReport).save();
            }
        } catch (FileNotFoundException e) {
            LOGGER.info(e.getMessage());
            LOGGER.debug(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return SENSOR_NAME;
    }

    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor.name(SENSOR_NAME);
    }

    @Override
    public void execute(SensorContext sensorContext) {
        Profiler profiler = Profiler.create(LOGGER);
        profiler.startInfo("Process Dependency-Check report");
        if (DependencyCheckUtils.skipPlugin(sensorContext.config()).booleanValue()) {
            LOGGER.info("Dependency-Check skipped");
        } else {
            Optional<Analysis> analysis = parseAnalysis(sensorContext);
            if (analysis.isPresent()) {
                DependencyReasonSearcher dependencyReasonSearcher = new DependencyReasonSearcher(sensorContext);
                dependencyReasonSearcher.addDependenciesToInputComponents(analysis.get(), sensorContext);
            }
            uploadHTMLReport(sensorContext);
        }
        profiler.stopInfo();
    }
}
