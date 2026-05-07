var queryString = decodeURIComponent(window.location.search);
var params = new URLSearchParams(queryString);
var id = safePositiveInteger(params.get('id'));
var videoType = safeVideoType(params.get('type'));

if (id === null) {
	throw new Error("Invalid movie id.");
}
if (videoType !== "movie") {
	throw new Error("Invalid video type.");
}

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
				setRatingIcon("#icon-path", iconPath, 30);

				let avgScorePercentage = (avgScore * 10).toString();

				$("#currentRatingPerc").text(Math.round(avgScorePercentage) + "%");

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

						$(".rating-layout").empty().append($("<h3>").addClass("rating").text("Not yet rated"));
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


		$("#title").text(resp.title || "");
		$("#release_date").empty().append($("<span>").addClass("movieInfo").text(" " + (resp.release_date || "")));
		$("#runtime").empty().append($("<span>").addClass("movieInfo").text(" " + (resp.runtime || "")));

		const $genre = $("#genre").empty();
		(resp.genres || []).forEach((genre, index) => {

			let genreName = genre.name;
			if (genreName === "Science Fiction") {
				genreName = "Sci-Fi";
			}
			$("<div>").addClass("xinzhuang").text(genreName || "").appendTo($genre);
		})

		$("#adult").text(String(resp.adult || ""));
		$("#overview").text(resp.overview || "");


		let productionCountries = "";
		for (let i = 0; i < (resp.production_countries || []).length; i++) {
			if (i > 0) {
				productionCountries += ", ";
			}

			if (resp.production_countries[i].iso_3166_1 === 'GB') {
				productionCountries += 'UK';
			} else {
				productionCountries += `${resp.production_countries[i].iso_3166_1}`;
			}

		}

		$("#production_countries").text(productionCountries);

		$("#spoken_languages").text((resp.spoken_languages && resp.spoken_languages[0])
			? resp.spoken_languages[0].english_name
			: "");

		const posterUrl = safeTmdbImageUrl(resp.poster_path);
		setImageContent(".movieImage", posterUrl, "img");

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

		$("#cast").text(resp.cast && resp.cast[0] ? resp.cast[0].name : "");

		for (const respElement of resp.crew || []) {

			if (respElement.known_for_department == "Directing") {

				$("#director").text(respElement.name || "");
				break;
			}
		}
		for (const respElement of resp.crew || []) {
			if (respElement.known_for_department == "Writing") {

				$("#writer").text(respElement.name || "");
				break;
			}
		}
		const $actorImage = $(".actor_image").empty();
		for (const respElement of (resp.crew || []).slice(0, 5)) {
			const personId = safePositiveInteger(respElement.id);
			if (personId === null) {
				continue;
			}
			const profileUrl = safeTmdbImageUrl(respElement.profile_path);
			appendActorCard($actorImage, personId, profileUrl, respElement.name || "", respElement.job || "");
		}
		for (const respElement of (resp.cast || []).slice(0, 5)) {
			const personId = safePositiveInteger(respElement.id);
			if (personId === null) {
				continue;
			}
			const profileUrl = safeTmdbImageUrl(respElement.profile_path);
			appendActorCard($actorImage, personId, profileUrl, String(respElement.name || "").slice(0, 15), respElement.known_for_department || "");
		}
	}
})

function appendActorCard($container, personId, profileUrl, name, role) {
	const $item = $("<div>").addClass("actorImageItem").on("click", function() {
		toPersonPage(personId);
	});
	const $image = $("<div>").addClass("image");
	const image = createImageElement(profileUrl, "profile");
	if (image) {
		image.setAttribute("srcset", "");
		$image.append(image);
	}
	const $text = $("<div>").addClass("actorText")
		.append($("<h4>").text(name || ""))
		.append($("<p>").text(role || ""));

	$item.append($image).append($text);
	$container.append($item);
}

function toRatePage() {

	window.location.href = server + "/rate?id=" + id + "&" + "type=movie"


}

function toPersonPage(uId) {
	const personId = safePositiveInteger(uId);
	if (personId !== null) {
		window.location.href = server + "/details?id=" + personId
	}
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
