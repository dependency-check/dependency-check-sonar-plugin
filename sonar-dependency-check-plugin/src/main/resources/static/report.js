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
window.registerExtension("dependencycheck/report", function(options) {
	var isDisplayed = true;
	
	if (!document.querySelector("style#dependency-check-report")) {
		var style = document.createElement("style");
		style.id = "dependency-check-report";
		// WebKit hack :(
		style.appendChild(document.createTextNode(""));
		document.head.appendChild(style);
		style.sheet.insertRule(".dependency-check-report-content {flex: 1 1 auto;}", 0);
		style.sheet.insertRule(".dependency-check-report-container {display: flex; flex-direction: column;}", 0);
	}
	
	window.SonarRequest.getJSON("/api/measures/component", {
		component : options.component.key,
		metricKeys : "report"
	}).then(function(response) {
		if (isDisplayed) {
			var htmlString = response.component.measures.filter(measure => measure.metric === "report")[0].value;
			var currentEl = options.el;
			while (currentEl.id !== "container") {
				currentEl.classList.add("dependency-check-report-content");
				currentEl.classList.add("dependency-check-report-container");
				currentEl = currentEl.parentElement;
			}
			currentEl.classList.add("dependency-check-report-container");
			
			var reportFrame = document.createElement("iframe");
			reportFrame.sandbox.value = "allow-scripts allow-same-origin";
			reportFrame.style.border = "none";
			reportFrame.style.flex= "1 1 auto";
			reportFrame.srcdoc = htmlString;
			options.el.append(reportFrame);
		}
	});

	return function() {
		options.el.textContent = "";
		var isDisplayed = false;
		var currentEl = options.el;
		while (currentEl.id !== "container") {
			currentEl.classList.remove("dependency-check-report-content");
			currentEl.classList.remove("dependency-check-report-container");
			currentEl = currentEl.parentElement;
		}
		currentEl.classList.remove("dependency-check-report-container");
	};
});
