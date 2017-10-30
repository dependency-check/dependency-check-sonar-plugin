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
		componentKey : options.component.key,
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