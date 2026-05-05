$(document).ready(function() {

	let currentScript;

	// Function to dynamically load a script
	function loadScript(scriptPath, callback) {
		if (currentScript) {
			// Remove the previous script
			currentScript.remove();
		}
		// Create a new script element
		const script = document.createElement('script');
		script.src = scriptPath;
		script.onload = callback;
		script.defer = true;
		document.body.appendChild(script);
		currentScript = script;
	}

	// Function to load content based on the toggle state
	function loadContent(isChecked) {
		const contentSection = $('#movies');
		contentSection.empty(); // Clear existing content

		if (isChecked) {
			// Load content from content2.js
			loadScript('/static/assets/js/moviesClimateRated.js', loadContent2);
		} else {
			// Load content from content1.js
			loadScript('/movierating/src/main/resources/static/assets/js/moviesTMDB.js', loadContent1);
		}
	}

	// Initial load (assuming unchecked state)
	loadContent(false);

	// Toggle the filters and content when the switch is toggled
	$('#toggleSwitch').on('change', function() {
		const isChecked = $(this).is(':checked');

		// Load the appropriate content
		loadContent(isChecked);
	});
});