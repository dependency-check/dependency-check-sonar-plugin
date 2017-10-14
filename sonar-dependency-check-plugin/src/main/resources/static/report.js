window.registerExtension("dependencycheck/report", function(options) {
	var isDisplayed = true;
	
	window.SonarRequest.getJSON("/api/measures/component", {
		componentKey : options.component.key,
		metricKeys : "report"
	}).then(function(response) {
		if (isDisplayed) {
			var htmlString = response.component.measures.filter(measure => measure.metric === "report")[0].value;
			var currentEl = options.el;
			while (currentEl.id !== "container") {
				currentEl.style.display = "flex";
				currentEl.style.flex = "1 1 auto";
				currentEl.style.flexDirection = "column";
				currentEl = currentEl.parentElement;
			}
			currentEl.style.display = "flex";
			currentEl.style.flexDirection = "column";
			
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
	};
});