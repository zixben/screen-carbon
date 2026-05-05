var queryString = decodeURIComponent(window.location.search);
var params = new URLSearchParams(queryString);
var value = params.get('value');
var videoType = params.get('type'); 

function setActiveTab(button) {
	// Remove active class from all buttons
	$('.btn-group .btn').removeClass('active');

	// Add active class to the clicked button
	$(button).addClass('active');

}

function more() {
	let where = window.sessionStorage.getItem("where");
	let page = Number(window.sessionStorage.getItem("page"));
	page += 1;
	movies(where, page)
}

function fetchClimateVideo() {
	return new Promise((resolve, reject) => {
		$.ajax({
			url: server + "/score/getOrderAvg",
			method: "get",
			headers: {
				"accept": "application/json"
			},
			success: function(response) {
				resolve(response);
			},
			error: function(xhr, status, error) {
				console.error("An error occurred: " + status + ", " + error + ", " + xhr);
				reject(error);
			}
		});
	});
}

function fetchMovies(value, page, climateVideo) {
	return new Promise((resolve, reject) => {
		$.ajax({
			url: "https://api.themoviedb.org/3/search/multi?query=" + value + "&include_adult=false&language=en-US&page=" + page,
			method: "get",
			headers: {
				"Authorization": jwt,
				"accept": "application/json"
			},
			success: function(resp) {
				resolve({ resp, climateVideo });
			},
			error: function(xhr, status, error) {
				console.error("An error occurred: " + status + ", " + error + ", " + xhr);
				reject(error);
			}
		});
	});
}

function renderMovies(data) {
	const { resp, climateVideo } = data;
	let htmlEle = "";
	for (const respElement of resp.results) {
		if (respElement.media_type != window.sessionStorage.getItem("where")) {
			continue;
		}

		let title = respElement.title || respElement.name;
		let overview = String(respElement.overview).substring(0, 200) + "...";
		let score = "Not yet rated";
		let color;
		let iconPath;
		let iconClass = '';

		if (climateVideo.some(m => (m.vId === respElement.id && m.videoType === respElement.media_type))) {
			climateVideo.forEach((item) => {
				if (item.vId === respElement.id && item.videoType === respElement.media_type) {
					let climateVoteAverage = item.score;
					score = (climateVoteAverage * 10).toString().substring(0, 5) + "%";
					color = determineBorderColor(climateVoteAverage);
					iconPath = determineIconPath(climateVoteAverage);
					iconClass = 'card-icon';
				}
			});
		} else {
			iconPath = '';
		}

		let mediaType = respElement.media_type === "person" ? 3 : (respElement.media_type === "movie" ? 1 : 0);
		let poster = respElement.poster_path || respElement.profile_path;

		let scoreHTMLElement = mediaType === 3 ? '' : 
			"<div onclick='toRate(" + respElement.id + "," + mediaType + ")' class=\"item_sore\">\n" +
			"    <p><span><img class='" + iconClass + "' src='" + iconPath + "'></span>\n" +
			"    <span style='color:" + color + ";'>" + score + "</span>\n" +
			"    </p>\n" +
			"</div>\n";

		htmlEle += "<div class=\"item\">\n" +
			"    <div onclick='toDesc(" + respElement.id + "," + mediaType + ")' class=\"item_image\"><img alt='no image' src='" + imgServer + poster + "'></div>\n" +
			"    <div onclick='toDesc(" + respElement.id + "," + mediaType + ")' class=\"item_info\">\n" +
			"        <h5>" + title + "</h5>\n" +
			"        <p>" + overview + "</p>\n" +
			"    </div>\n" +
			scoreHTMLElement +
			"</div>";
	}
	$(".items").html(htmlEle);
}

function movies(where, page) {
	window.sessionStorage.setItem("where", where)
	window.sessionStorage.setItem("page", page)

	fetchClimateVideo()
		.then(climateVideo => fetchMovies(value, page, climateVideo))
		.then(renderMovies)
		.catch(error => console.error("An error occurred while fetching movies: ", error));
}

function toRate(id, type) {
	if (type == 3) {
		window.location.href = server + "/details?id=" + id;
	} else if (type == 1) {
		window.location.href = server + "/rate?id=" + id + "&type=movie";
	} else {
		window.location.href = server + "/rate?id=" + id + "&type=tv";
	}
	return false;
}

function toDesc(id, type) {
	if (type == 3) {
		window.location.href = server + "/details?id=" + id;
	} else if (type == 1) {
		window.location.href = server + "/movie?id=" + id + "&type=movie";
	} else {
		window.location.href = server + "/tv?id=" + id + "&type=tv";
	}
}

function determineBorderColor(vote_average) {
	if (vote_average >= 8) return '#669900'; // Green
	else if (vote_average >= 6) return '#aec000'; // Old Yellow '#ffcc02'; Now Light green
	else if (vote_average >= 4) return '#ff9900'; // Orange
	else if (vote_average >= 2) return '#cc0100'; // Old_Brown '#9a6601'; Now Red
	else return '#808080';  // Old_red '#cc3401'; // Now Grey
}

function determineIconPath(vote_average) {
	if (vote_average >= 8) return 'assets/images/ranking_icons/ICONS_0000_Green.png';
	else if (vote_average >= 6) return 'assets/images/ranking_icons/ICONS_0001_LightGreen.png';
	else if (vote_average >= 4) return 'assets/images/ranking_icons/ICONS_0002_Orange.png';
	else if (vote_average >= 2) return 'assets/images/ranking_icons/ICONS_0003_Red.png';
	else return 'assets/images/ranking_icons/ICONS_0004_Grey.png';
}

$(document).ready(function() {
	$(".searchValue").html(value);
	// Determine which tab to activate based on the 'type' parameter
	if (videoType === 'tv') {
		setActiveTab($('.btn-group .btn')[1]); // TV shows tab
		movies('tv', 1); // Load TV shows
	} else {
	movies('movie', 1);
	}
});
