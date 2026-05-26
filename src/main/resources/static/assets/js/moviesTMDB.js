$(document).ready(function() {
    loadTMDBOptions();
	window.sessionStorage.clear();
	window.sessionStorage.setItem("Sort", "popularity.desc");
	window.sessionStorage.setItem("Year", '');
	window.sessionStorage.setItem("movieNumPage", 1);
	getMovies();

	$(".form-select").on("change", function(e) {
		let element = e.currentTarget;
		let index = element.selectedIndex;
		let value = element.options[index].value;
		let key = element.options[0].text;
		window.sessionStorage.setItem(key, value);
		window.sessionStorage.setItem("movieNumPage", 1);
		window.sessionStorage.removeItem("movieTotalPages");
		getMovies();
	})

	function prePage() {
		let num = Number(window.sessionStorage.getItem("movieNumPage"));
		if (num <= 1) {
			window.sessionStorage.setItem("movieNumPage", 1);
			updateMediaPagination(1);
			return;
		}
		window.sessionStorage.setItem("movieNumPage", num - 1);
		getMovies();
	}

	function nextPage() {
		let num = Number(window.sessionStorage.getItem("movieNumPage"));
		const totalPages = Number(window.sessionStorage.getItem("movieTotalPages"));
		if (Number.isInteger(totalPages) && totalPages > 0 && num >= totalPages) {
			updateMediaPagination(num, { totalPages: totalPages });
			return;
		}
		window.sessionStorage.setItem("movieNumPage", num + 1);
		getMovies();
	}

	function getMovies() {
		showLoadingMessage("#movies", "Loading films...");

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
			num = 1;
			window.sessionStorage.setItem("movieNumPage", num);
		}
		const storedTotalPages = Number(window.sessionStorage.getItem("movieTotalPages"));
		if (Number.isInteger(storedTotalPages) && storedTotalPages > 0 && num > storedTotalPages) {
			num = storedTotalPages;
			window.sessionStorage.setItem("movieNumPage", num);
		}
		updateMediaPagination(num, { totalPages: storedTotalPages, isLoading: true });


		$.ajax({
			url: server + "/score/getOrderAvg",
			method: "get",
			headers: {
				"Authorization": jwt,
				"accept": "application/json"
			},
			success: function(response) {
				loadTmdbMovies(Array.isArray(response) ? response : []);
			},
			error: function(xhr, status, error) {
				console.error("An error occurred: " + status + ", " + error + ", " + xhr);
				loadTmdbMovies([]);
			}
		});
	}

	function loadTmdbMovies(climateMovies) {
		let sort = sessionStorage.getItem("Sort");
		if (sort == null) sort = "";
		let Genre = sessionStorage.getItem("Genre");
		if (Genre == null) Genre = "";
		let Country = sessionStorage.getItem("Country");
		if (Country == null) Country = "";
		let Year = sessionStorage.getItem("Year");
		if (Year == null) Year = "";

		let num = Number(window.sessionStorage.getItem("movieNumPage"));
		$.ajax({
			url: "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&page=" + num + "&sort_by=" + sort + "&with_genres=" + Genre + "&with_origin_country=" + Country + "&primary_release_year=" + Year,
			cache: false,
			method: "get",
			headers: {
				"Authorization": jwt,
				"accept": "application/json"
			},
			success: function(resp) {
				const results = filterSafeTmdbResults(resp.results);
				const totalPages = Math.max(1, Number(resp.total_pages) || 1);
				window.sessionStorage.setItem("movieTotalPages", totalPages);

				if (num > totalPages) {
					window.sessionStorage.setItem("movieNumPage", totalPages);
					updateMediaPagination(totalPages, { totalPages: totalPages });
					getMovies();
					return;
				}

				updateMediaPagination(num, { totalPages: totalPages });

				if (results.length) {
					const $movies = $("#movies").empty();

					for (const respElement of results) {
						appendTmdbMovieCard($movies, respElement, climateMovies);
					}
				} else {
					showTextMessage("#movies", "No results found");
				}
			},
			error: function() {
				updateMediaPagination(num, { hasNext: false });
				showTextMessage("#movies", "No results found");
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
		const matchedClimateMovie = climateMovies.find(m => (Number(m.vId) === resultId && m.videoName === title));
		const score = matchedClimateMovie ? Number(matchedClimateMovie.score) : null;
		const isRated = Number.isFinite(score);
		const borderColor = isRated ? determineBorderColor(score) : "";
		const iconPath = isRated ? determineIconPath(score) : "";
		const voteAveragePercentage = isRated ? (score * 10).toFixed(1).replace(/\.0$/, "") : "";

		const $card = $("<div>").addClass("videoCar").on("click", function() {
			toDesc(resultId);
		}).attr("data-search-title", title);
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

	$('.page-item').click(function() {
		const action = $(this).data('action');
		if (action === 'prev') {
			prePage();
		} else if (action === 'next') {
			nextPage();
		}
	});

	enhanceFilterSelects(".search");
	initializeMediaPageSearch({
		containerSelector: "#movies",
		videoType: "movie",
		allResultsLabel: "films",
		placeholder: "Filter visible films"
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
