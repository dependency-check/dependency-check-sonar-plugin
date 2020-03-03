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
    data: []
  };

  componentDidMount() {
    findDependencyCheckReport(this.props.project).then(data => {
      this.setState({
        loading: false,
        data
      });
    });
  }

  render() {
    if (this.state.loading) {
      return <div className="page page-limited"><DeferredSpinner /></div>;
    }

    return (<div><iframe srcdoc={this.state.data} style={{border: 'none'}} height="600px" width="100%"/></div>);
  }
}
export function findDependencyCheckReport(project) {
  return getJSON("/api/measures/component", {
      component : project.key,
      metricKeys : "report"
    }).then(function(response) {
    var report = response.component.measures.filter(measure => measure.metric === "report")[0];
    if (report !== undefined) {
      return report.value
    } else {
      return "<center><h2>No HTML-Report found. Please check property sonar.dependencyCheck.htmlReportPath</h2></center>"
    }
  });
}
