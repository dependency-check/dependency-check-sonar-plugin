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
window.registerExtension('dependencycheck/report_page', function (options) {
  var container = options.el;

  function parameters() {
    var params = 'component=' + encodeURIComponent(options.component.key);
    var query = new URLSearchParams(window.location.search);
    if (query.get('branch')) {
      params += '&branch=' + encodeURIComponent(query.get('branch'));
    }
    if (query.get('pullRequest')) {
      params += '&pullRequest=' + encodeURIComponent(query.get('pullRequest'));
    }
    return params;
  }

  function showMessage(text) {
    var message = document.createElement('div');
    message.style.padding = '20px';
    message.textContent = text;
    container.appendChild(message);
  }

  // The page is served at "<webContext>/project/extension/dependencycheck/report_page", so the
  // web service must be addressed from the web context root, not with a relative "../" - that
  // resolves to "<webContext>/project/extension/api/dependencycheck/show", which is not a real
  // route. SonarQube's single-page app answers unknown routes with HTTP 200 and its HTML shell,
  // so a wrong URL here would silently look like success (see the X-Dependency-Check-Report
  // check below).
  function webContextBase() {
    if (typeof window.baseUrl === 'string') {
      return window.baseUrl;
    }
    var marker = '/project/extension/';
    var index = window.location.pathname.indexOf(marker);
    return index === -1 ? '' : window.location.pathname.substring(0, index);
  }

  var url = webContextBase() + '/api/dependencycheck/show?' + parameters();

  function probe(method) {
    return window.fetch(url, { credentials: 'same-origin', method: method });
  }

  // The status is checked with HEAD, so the report itself is downloaded once - when the user opens
  // it in a new tab - and not twice. Should the web service engine ever refuse HEAD on this action,
  // fall back to GET rather than leave the page empty. A proxy in front of SonarQube may block HEAD
  // with a 403 or a 404 of its own; those are retried with GET too, but only when the answer does
  // not carry the plugin's own marker header - otherwise a real "no report published" answer from
  // this very action would be fetched a second time for nothing.
  function methodRejected(response) {
    if (response.status === 400 || response.status === 405 || response.status === 501) {
      return true;
    }
    return (
      (response.status === 403 || response.status === 404) &&
      !response.headers.get('X-Dependency-Check-Report')
    );
  }

  probe('HEAD')
    .then(function (response) {
      if (methodRejected(response)) {
        return probe('GET');
      }
      return response;
    })
    .then(function (response) {
      if (response.status === 404) {
        showMessage(
          'No Dependency-Check report has been published for this project. ' +
            'Configure a report store under Administration > Configuration > Dependency-Check.'
        );
        return;
      }
      if (!response.ok) {
        showMessage('The Dependency-Check report could not be loaded (HTTP ' + response.status + ').');
        return;
      }
      // A 2xx status alone does not prove this is the plugin's response: SonarQube's single-page
      // app answers an unknown route with HTTP 200 and its HTML shell too. Require the marker
      // header the web service sets in DependencyCheckReportWebService#writeReport before
      // treating this as a real report.
      if (!response.headers.get('X-Dependency-Check-Report')) {
        showMessage(
          'The Dependency-Check web service did not answer. The SonarQube base URL may be ' +
            'misconfigured.'
        );
        return;
      }
      // No iframe here. SonarQube 26.9 sends a page-wide Content-Security-Policy header with an
      // empty `frame-src` directive, which forbids embedding ANY frame, including one pointed at
      // this plugin's own report_upload/show action. That header is SonarQube's own response
      // header, not something this plugin controls, so there is no CSP we could add to allow it -
      // the only way to show the report is to navigate to it as a top-level document. The report
      // response itself still sets its own `Content-Security-Policy: sandbox allow-scripts; ...`
      // (see DependencyCheckReportWebService#writeReport), which keeps it in an opaque origin
      // without access to the SonarQube session even when opened this way. Do not reintroduce an
      // iframe; the browser will block it again.
      var intro = document.createElement('div');
      intro.style.padding = '20px';
      intro.textContent =
        "SonarQube's content security policy does not allow embedding the report on this page, " +
        'so it opens in a new tab instead.';
      container.appendChild(intro);

      var link = document.createElement('a');
      link.setAttribute('href', url);
      link.setAttribute('target', '_blank');
      link.setAttribute('rel', 'noopener noreferrer');
      link.textContent = 'Open the Dependency-Check report';
      link.style.margin = '0 20px';
      container.appendChild(link);
    })
    .catch(function () {
      showMessage('The Dependency-Check report could not be loaded.');
    });

  return function () {
    container.innerHTML = '';
  };
});
