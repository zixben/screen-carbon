var queryString = decodeURIComponent(window.location.search);
var params = new URLSearchParams(queryString);
var id = params.get('id');
var score = params.get("score");
var type = params.get('type');

// Determine which menu item should be active and add the 'active' class
if (type === 'movie') {
	document.getElementById('movies-menu').classList.add('active');
} else if (type === 'tv') {
	document.getElementById('tv-shows-menu').classList.add('active');
}

$(document).ready(function() {
	$('.btn-dark').click(function() {
		switch (type) {
			case 'movie':
				window.location.href = '/movies';
				break;
			case 'tv':
				window.location.href = '/tv-shows';
				break;
			default:
				console.log('Type not recognized');
		}
	});
});

$("#score").html(String(score).substring(0, 4) + "/10")
$("#scorePerc").html("(" + String(score * 10).substring(0, 4) + "%)")

let url = "https://api.themoviedb.org/3/movie/" + id;
if (type == 'tv') {
	url = "https://api.themoviedb.org/3/tv/" + id;
}

$.ajax({
	url: url,
	method: "get",
	headers: {
		"Authorization": jwt,
		"accept": "application/json"
	},
	success: function(resp) {

		// Set the title based on the type
		var titleText = type == 'movie' ? resp.title : resp.name;
		$("#title").html(titleText); // Set the title in the HTML

		// Now fetch and display the average score for this title
		fetchAndDisplayAverageScore(titleText); // Call the function defined below

		$("#overview").html(resp.overview);
		$(".finish-image").html("<img src='" + imgServer + resp.poster_path + "' alt='img'>");

	}
})

// Function to fetch and display the average score
function fetchAndDisplayAverageScore(currentPageTitle) {
	$.ajax({
		url: server + "/score/getAvgFraction",
		method: "GET",
		success: function(response) {

			// Find the index of the current title in the avgX array
			var index = response.avgX.findIndex(title => title.trim() === currentPageTitle.trim());
			if (index !== -1) {
				// If found, get the corresponding score
				var avgScore = response.avgY[index];

				// Determine border color based on rating
				borderColor = determineBorderColor(avgScore);
				iconPath = determineIconPath(avgScore);

				// Update the HTML element with the new data
				$("#poster_path").css("border-color", borderColor);
				$(".icon-path").html("<img style=\"width: 30px; height: 30px;\" src='" + server + iconPath + "' alt='img'>");
				$("tr").eq(1).find("h3 > span").last().text(`${String(Math.round(avgScore * 10)).substring(0, 4)}%`);
			} else {
				$("#poster_path").css("border", "none");

			}
		},
		error: function(xhr, textStatus, errorThrown) {
			console.error("AJAX Error:", textStatus, errorThrown, "Response:", xhr.responseText);
		}
	});
}

// Function to determine border color based on rating
function determineBorderColor(vote_average) {
	if (vote_average >= 8) return '#669900'; // Green
	else if (vote_average >= 6) return '#aec000'; // Old Yellow '#ffcc02'; Now Light green
	else if (vote_average >= 4) return '#ff9900'; // Orange
	else if (vote_average >= 2) return '#cc0100'; // Old_Brown '#9a6601'; Now Red
	else return '#808080';  // Old_red '#cc3401'; // Now Grey
}

// Function to determine icon path based on rating
function determineIconPath(vote_average) {
	if (vote_average >= 8) return 'assets/images/ranking_icons/ICONS_0000_Green.png';
	else if (vote_average >= 6) return 'assets/images/ranking_icons/ICONS_0001_LightGreen.png';
	else if (vote_average >= 4) return 'assets/images/ranking_icons/ICONS_0002_Orange.png';
	else if (vote_average >= 2) return 'assets/images/ranking_icons/ICONS_0003_Red.png';
	else return 'assets/images/ranking_icons/ICONS_0004_Grey.png';
}

$(".moveInput").on("keyup", function(e) {
	const inputValue = $(this).val().trim();
	if (e.key === "Enter" && inputValue.length > 0) {
		window.location.href = server + "/search-results?value=" + inputValue;
	} else if (e.key === "Enter") {
		// If the Enter key was pressed but the input is empty, show an alert
		alert("The input is empty!");
	}
});