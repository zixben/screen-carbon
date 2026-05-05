var queryString = decodeURIComponent(window.location.search);
var params = new URLSearchParams(queryString);
var id = params.get('id');
var videoType = params.get('type');

$.ajax({
	url: server + "/score/getScoreAvg/" + id + "/" + videoType,
	method: "get",
	headers: {
		"accept": "application/json"
	},
	success: function(response) {
		response.forEach((avgScore) => {

			let vote_average = avgScore;
			if (vote_average !== null) {


				// Determine border color based on rating
				borderColor = determineBorderColor(vote_average);
				iconPath = determineIconPath(vote_average);

				$("#poster_path").css("border-color", borderColor);
				$("#icon-path").html("<img style=\"width: 30px; height: 30px;\" src='" + server + iconPath + "' alt='img'>");

				let avgScorePercentage = (avgScore * 10).toString();

				$("#currentRatingPerc").html(Math.round(avgScorePercentage) + "%");

			} else {

				$("#poster_path").css("border", "none");

				$.ajax({
					url: "https://api.themoviedb.org/3/movie/" + id,
					method: "get",
					headers: {
						"Authorization": jwt,
						"accept": "application/json"
					},
					success: function(resp) {
						scoreTMDB = resp.vote_average;

						$(".rating-layout").html("<h3 class='rating'>Not yet rated</h3>");
					},
					error: function(xhr, status, error) {
						console.error("An error occurred: " + status + ", " + error + ", " + xhr);
					}
				});
			}
		});
	},
	error: function(xhr, status, error) {
		console.error("An error occurred: " + status + ", " + error + ", " + xhr);
	}
});

$.ajax({
	url: "https://api.themoviedb.org/3/movie/" + id,
	cache: false,
	method: "get",
	headers: {
		"Authorization": jwt,
		"accept": "application/json"
	},
	success: function(resp) {


		$("#title").html(resp.title);
		$("#release_date").html("<span class='movieInfo'> " + resp.release_date + "</span>");
		$("#runtime").html("<span class='movieInfo'> " + resp.runtime + "</span>");

		let genres = "";
		(resp.genres).forEach((genre, index) => {

			let genreName = genre.name;
			if (genreName === "Science Fiction") {
				genreName = "Sci-Fi";
			}
			genres += `<div class="xinzhuang">${genreName}</div>`
		})
		$("#genre").html(genres);

		$("#adult").html(resp.adult);
		$("#overview").html(resp.overview);


		let productionCountries = "";
		for (let i = 0; i < resp.production_countries.length; i++) {
			if (i > 0) {
				productionCountries += ", ";
			}

			if (resp.production_countries[i].iso_3166_1 === 'GB') {
				productionCountries += 'UK';
			} else {
				productionCountries += `${resp.production_countries[i].iso_3166_1}`;
			}

		}

		$("#production_countries").html(productionCountries);

		$("#spoken_languages").html(resp.spoken_languages[0].english_name);

		$(".movieImage").html("<img src='" + imgServer + resp.poster_path + "' alt='img'>");

	}
})

$.ajax({
	url: "https://api.themoviedb.org/3/movie/" + id + "/credits?language=en-US",
	method: "get",
	headers: {
		"Authorization": jwt,
		"accept": "application/json"
	},
	success: function(resp) {

		$("#cast").html(resp.cast[0].name)

		for (const respElement of resp.crew) {

			if (respElement.known_for_department == "Directing") {

				$("#director").html(respElement.name);
				break;
			}
		}
		for (const respElement of resp.crew) {
			if (respElement.known_for_department == "Writing") {

				$("#writer").html(respElement.name);
				break;
			}
		}
		let castStr = "";
		for (const respElement of resp.crew.slice(0, 5)) {
			castStr += "   <div onclick=\"toPersonPage(" + respElement.id + ")\"  class='actorImageItem'>\n" +
				"          <div class=\"image\">\n" +
				"            <img src='" + imgServer + respElement.profile_path + "' alt=\"profile\" srcset=\"\">\n" +
				"          </div>\n" +
				"          <div class=\"actorText\">\n" +
				"            <h4>" + respElement.name + "</h4>\n" +
				"            <p>" + respElement.job + "</p>\n" +
				"          </div>\n" +
				"        </div>"
		}
		for (const respElement of resp.cast.slice(0, 5)) {
			castStr += "   <div onclick=\"toPersonPage(" + respElement.id + ")\" class='actorImageItem'>\n" +
				"          <div class=\"image\">\n" +
				"            <img src='" + imgServer + respElement.profile_path + "' alt=\"profile\" srcset=\"\">\n" +
				"          </div>\n" +
				"          <div class=\"actorText\">\n" +
				"            <h4>" + respElement.name.slice(0, 15) + "</h4>\n" +
				"            <p>" + respElement.known_for_department + "</p>\n" +
				"          </div>\n" +
				"        </div>"
		}
		$(".actor_image").html(castStr)
	}
})

function toRatePage() {

	window.location.href = server + "/rate?id=" + id + "&" + "type=movie"


}

function toPersonPage(uId) {
	window.location.href = server + "/details?id=" + uId
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
