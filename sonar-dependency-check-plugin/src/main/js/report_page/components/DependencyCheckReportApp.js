/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2021 dependency-check
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
import { isBranch, isPullRequest } from "sonar-helpers";
import { getJSON } from "sonar-request";

export function findDependencyCheckReport(options) {
  var request = {
    component : options.component.key,
    metricKeys : "report"
  };

  // branch and pullRequest are internal parameters for /api/measures/component
  if (isBranch(options.branchLike)) {
    request.branch = options.branchLike.name;
  } else if (isPullRequest(options.branchLike)) {
    request.pullRequest = options.branchLike.key;
  }

  return getJSON("/api/measures/component", request).then(function(response) {
    var report = response.component.measures.find((measure) => {
      return measure.metric === "report";
    });
    if (typeof report  === "undefined") {
      return "<center><h2>No HTML-Report found. Please check property sonar.dependencyCheck.htmlReportPath</h2></center>";
    } else {
      return report.value;
    }
  });
}

export default class DependencyCheckReportApp extends React.PureComponent {
  constructor() {
    super();
    this.state = {
      loading: true,
      data: "",
      height: 0,
    };
  }


  componentDidMount() {
    // eslint-disable-next-line react/prop-types
    findDependencyCheckReport(this.props.options).then((data) => {
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
    let updateHeight = window.innerHeight - (72 + 48 + 145.5);
    this.setState({ height: updateHeight });
  }

  render() {
    // IFrame for warnings, new tab when report available.
    if(!this.state.data.includes("Dependency-Check")){
      // IFrame
      return (<div className="page dependency-check-report-container" >
                <iframe sandbox="allow-scripts allow-same-origin" height={this.state.height} srcDoc={this.state.data} style={{border: "none"}} />
              </div>);
    } else {
      // Open in new tab (avoid endless 'loading...' and present the URL by using an Event Listener)
      const newWindow = window.open('#', "_blank");
      if (newWindow) {
        newWindow.addEventListener('load', () => {
          newWindow.document.open();
          newWindow.document.write(this.state.data);
          newWindow.document.close();
          newWindow.stop();
        }, true);
      }

      return (<center><h1 style={{padding: "250px 0 0 0"}}>Dependency-Check report opened in adjacent tab.</h1></center>);
    }
}
