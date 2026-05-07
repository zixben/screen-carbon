var queryString = decodeURIComponent(window.location.search);
var params = new URLSearchParams(queryString);
var id = safePositiveInteger(params.get('id'));
var type = safeVideoType(params.get('type'));

if (id === null) {
	throw new Error("Invalid video id.");
}
if (type !== "movie" && type !== "tv") {
	throw new Error("Invalid video type.");
}

// Determine which menu item should be active and add the 'active' class
if (type === 'movie') {
	document.getElementById('movies-menu').classList.add('active');
} else if (type === 'tv') {
	document.getElementById('tv-shows-menu').classList.add('active');
}

$(document).ready(function() {
	$('.btn-dark').click(function() {
		switch (type) {
			case "movie":
				window.location.href = '/movies';
				break;
			case "tv":
				window.location.href = '/tv-shows';
				break;
			default:
				alert("Type not recognized");
		}
	});
});

loadSubmittedScore();

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
		$("#title").text(titleText || ""); // Set the title in the HTML

		fetchAndDisplayAverageScore();

		$("#overview").text(resp.overview || "");
		const posterUrl = safeTmdbImageUrl(resp.poster_path);
		setImageContent(".finish-image", posterUrl, "img");

	}
})

function loadSubmittedScore() {
	$.ajax({
		url: server + "/score/last-submission",
		method: "GET",
		success: function(response) {
			if (Number(response.vId) !== id || response.videoType !== type) {
				displayUnavailableSubmittedScore();
				return;
			}
			displaySubmittedScore(response.score);
		},
		error: function() {
			displayUnavailableSubmittedScore();
		}
	});
}

function displaySubmittedScore(rawScore) {
	const score = normalizedScore(rawScore);
	if (score === null) {
		displayUnavailableSubmittedScore();
		return;
	}

	$("#score").text(formatScore(score) + "/10");
	$("#scorePerc").text("(" + formatPercent(score) + ")");
	setRatingIcon("#submittedScoreIcon", determineIconPath(score), 30);
}

function displayUnavailableSubmittedScore() {
	$("#score").text("Unavailable");
	$("#scorePerc").text("");
	$("#submittedScoreIcon").empty();
}

function fetchAndDisplayAverageScore() {
	$.ajax({
		url: server + "/score/getScoreAvg/" + id + "/" + type,
		method: "GET",
		success: function(response) {
			const avgScore = Array.isArray(response) && response.length > 0 ? normalizedScore(response[0]) : null;
			if (avgScore === null) {
				$("#poster_path").css("border", "none");
				$("#averageScoreIcon").empty();
				$("#averageScorePerc").text("Unavailable");
				return;
			}

			$("#poster_path").css("border-color", determineBorderColor(avgScore));
			setRatingIcon("#averageScoreIcon", determineIconPath(avgScore), 30);
			$("#averageScorePerc").text(formatPercent(avgScore));
		},
		error: function(xhr, textStatus, errorThrown) {
			console.error("AJAX Error:", textStatus, errorThrown, "Response:", xhr.responseText);
		}
	});
}

function normalizedScore(rawScore) {
	const score = Number(rawScore);
	return Number.isFinite(score) && score >= 0 && score <= 10 ? score : null;
}

function formatScore(score) {
	return score.toFixed(2).replace(/\.?0+$/, "");
}

function formatPercent(score) {
	return (score * 10).toFixed(1).replace(/\.0$/, "") + "%";
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
		redirectToSearch(inputValue);
	} else if (e.key === "Enter") {
		// If the Enter key was pressed but the input is empty, show an alert
		redirectToSearch(inputValue);
	}
});
