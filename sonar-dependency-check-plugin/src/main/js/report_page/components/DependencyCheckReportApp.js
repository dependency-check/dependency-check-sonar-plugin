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
import React from "react";
// SonarComponents (referenced as sonar-components here, see the Webpack config)
// exposes React components exposed by SonarQube.
import { DeferredSpinner } from "sonar-components";
import { getJSON } from "sonar-request";

export default class DependencyCheckReportApp extends React.PureComponent {
  state = {
    loading: true,
    data: [],
    height: 0,
  };

  componentDidMount() {
    findDependencyCheckReport(this.props.options).then(data => {
      this.setState({
        loading: false,
        data
      });
    });
    /**
    * Add event listener
    */
    this.updateDimensions();
    window.addEventListener("resize", this.updateDimensions.bind(this));
  }

  /**
   * Remove event listener
   */
  componentWillUnmount() {
    window.removeEventListener("resize", this.updateDimensions.bind(this));
  }

  updateDimensions() {
    // 72px SonarQube common pane
    // 72px SonarQube project pane
    // 145,5 SonarQube footer
    let update_height = window.innerHeight - (72 + 48 + 145.5);
    this.setState({ height: update_height });
  }

  render() {
    if (this.state.loading) {
      return <div className="page page-limited"><DeferredSpinner /></div>;
    }

    return (<div className="page dependency-check-report-container" >
              <iframe classsandbox="allow-scripts allow-same-origin" height={this.state.height} srcdoc={this.state.data} style={{border: 'none'}} />
            </div>);
  }
}
export function findDependencyCheckReport(options) {
  return getJSON("/api/measures/component", {
      component : options.component.key,
      metricKeys : "report"
    }).then(function(response) {
    var report = response.component.measures.find(measure => measure.metric === "report");
    if (report !== undefined) {
      return report.value
    } else {
      return "<center><h2>No HTML-Report found. Please check property sonar.dependencyCheck.htmlReportPath</h2></center>"
    }
  });
}
