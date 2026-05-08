$(document).ready(function() {
    loadTMDBOptions();
	window.sessionStorage.clear();
	window.sessionStorage.setItem("Sort", "popularity.desc");
	window.sessionStorage.setItem("Year", '');
	window.sessionStorage.setItem("movieNumPage", 1);
	getMovies();

	$(".form-select").on("change", () => {
		let element = window.event.target;
		let index = element.selectedIndex;
		let value = element.options[index].value;
		let key = element.options[0].text;
		window.sessionStorage.setItem(key, value);
		window.sessionStorage.setItem("movieNumPage", 1);
		getMovies();
	})

	function prePage() {
		let num = Number(window.sessionStorage.getItem("movieNumPage"));
		window.sessionStorage.setItem("movieNumPage", num - 1);
		getMovies();
	}

	function nextPage() {
		let num = Number(window.sessionStorage.getItem("movieNumPage"));
		window.sessionStorage.setItem("movieNumPage", num + 1);
		getMovies();
	}

	function getMovies() {
		let sort = sessionStorage.getItem("Sort");
		if (sort == null) sort = "";
		let Genre = sessionStorage.getItem("Genre");
		if (Genre == null) Genre = "";
		let Country = sessionStorage.getItem("Country");
		if (Country == null) Country = "";
		let Year = sessionStorage.getItem("Year");
		if (Year == null) Year = "";

		let num = Number(window.sessionStorage.getItem("movieNumPage"));
		if (num < 1) {
			return;
		}
		$("#pageNum").text(num)


		var climateMovies = [];
		$.ajax({
			url: server + "/score/getOrderAvg",
			method: "get",
			headers: {
				"Authorization": jwt,
				"accept": "application/json"
			},
			success: function(response) {
				climateMovies = response;
			},
			error: function(xhr, status, error) {
				console.error("An error occurred: " + status + ", " + error + ", " + xhr);
			}
		});

		$.ajax({
			url: "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&page=" + num + "&sort_by=" + sort + "&with_genres=" + Genre + "&with_origin_country=" + Country + "&primary_release_year=" + Year,
			cache: false,
			method: "get",
			headers: {
				"Authorization": jwt,
				"accept": "application/json"
			},
			success: function(resp) {
				if (resp.results.length) {
					const $movies = $("#movies").empty();

					for (const respElement of resp.results) {
						appendTmdbMovieCard($movies, respElement, climateMovies);
					}
				} else {
					showTextMessage("#movies", "No results found");
				}
			}
		})
	}

	function appendTmdbMovieCard($container, respElement, climateMovies) {
		const resultId = safePositiveInteger(respElement.id);
		if (resultId === null) {
			return;
		}

		const title = respElement.title || respElement.name || "";
		const posterUrl = safeTmdbImageUrl(respElement.poster_path);
		const matchedClimateMovie = climateMovies.find(m => (m.vId === resultId && m.videoName === title));
		const score = matchedClimateMovie ? Number(matchedClimateMovie.score) : null;
		const isRated = Number.isFinite(score);
		const borderColor = isRated ? determineBorderColor(score) : "";
		const iconPath = isRated ? determineIconPath(score) : "";
		const voteAveragePercentage = isRated ? (score * 10).toFixed(1).replace(/\.0$/, "") : "";

		const $card = $("<div>").addClass("videoCar").on("click", function() {
			toDesc(resultId);
		});
		const $imageWrapper = $("<div>").addClass("VideoImage");
		if (isRated) {
			$imageWrapper.css("border-color", borderColor);
		} else {
			$imageWrapper.css("border", "none");
		}
		const image = createImageElement(posterUrl, "image");
		if (image) {
			$imageWrapper.append(image);
		}

		const $info = $("<div>");
		const $rating = $("<p>");
		if (isRated) {
			const icon = createImageElement(iconPath, "rating icon", { className: "card-icon" });
			if (icon) {
				$rating.append($("<span>").append(icon));
			}
			$rating.append($("<span>").text(voteAveragePercentage + "%"));
		} else {
			$rating.text("Not yet rated");
		}

		$info.append($rating).append($("<h5>").text(title));
		$card.append($imageWrapper).append($info);
		$container.append($card);
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

	$('.page-item').click(function() {
		const action = $(this).data('action');
		if (action === 'prev') {
			prePage();
		} else if (action === 'next') {
			nextPage();
		}
	});
});
function toDesc(id) {
	const resultId = safePositiveInteger(id);
	if (resultId !== null) {
		window.location.href = server + "/movie?id=" + resultId + "&type=movie";
	}
}
	
function loadTMDBOptions() {
    let tmdbOptions = `
        <select class="form-select sort-select" aria-label="Default select example">
            <option value="popularity.desc" selected>Sort</option>
            <option value="popularity.asc">Popularity (asc)</option>
            <option value="popularity.desc">Popularity (desc)</option>
            <option value="revenue.asc">Revenue (asc)</option>
            <option value="revenue.desc">Revenue (desc)</option>
            <option value="primary_release_date.asc">Release date (asc)</option>
            <option value="primary_release_date.desc">Release date (desc)</option>
        </select>
    `;
    $('#sortSelectContainer').html(tmdbOptions);
}
