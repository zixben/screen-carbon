$(document).ready(function() {
	loadClimateRatedOptions();
	const pageSize = 20;
	const defaultPage = 1;
	const defaultSortValue = "avg_desc";
	const defaultSortUrl = server + "/score/getTVAvgDesc";
	const storageKeys = {
		country: "tvRatedSelectedCountry",
		genre: "tvRatedSelectedGenre",
		year: "tvRatedSelectedYear",
		sortValue: "tvRatedSortValue"
	};
	let searchDebounceTimer = null;

	function fetchVideos(url, page) {
		const offset = (page - 1) * pageSize;
		const country = window.sessionStorage.getItem(storageKeys.country) || "";
		const genre = window.sessionStorage.getItem(storageKeys.genre) || "";
		const year = window.sessionStorage.getItem(storageKeys.year) || "";
		const query = getMediaSearchQuery();

		updateMediaPagination(page, { isLoading: true });
		showLoadingMessage("#tv-shows", "Loading rated TV shows...");

		$.ajax({
			url: url,
			method: 'GET',
			data: {
				limit: pageSize + 1,
				offset: offset,
				country: country,
				genre: genre,
				year: year,
				query: query
			},
			success: function(response) {
				const videos = Array.isArray(response) ? response : [];
				const hasNextPage = videos.length > pageSize;
				const currentPageVideos = hasNextPage ? videos.slice(0, pageSize) : videos;

				if (currentPageVideos.length) {
					renderVideos(currentPageVideos);
				} else {
					if (page > defaultPage) {
						const previousPage = page - 1;
						window.sessionStorage.setItem("tvNumPage", previousPage);
						fetchVideos(url, previousPage);
						return;
					}

					showTextMessage("#tv-shows", "No results found");
				}
				updatePageNumber(page, hasNextPage);
			},
			error: function(error) {
				console.error("Error fetching videos:", error);
				showTextMessage("#tv-shows", "No results found");
				updatePageNumber(page, false);
			}
		});
	}

	function renderVideos(videos) {
		const $tvShows = $("#tv-shows").empty();
		videos.forEach(function(video) {
			const videoId = Number(video.vId);
			if (!Number.isInteger(videoId) || videoId <= 0) {
				return;
			}

			const score = Number(video.score) || 0;
			const borderColor = determineBorderColor(score);
			const iconPath = determineIconPath(score);
			const voteAveragePercentage = (score * 10).toFixed(1);
			const title = video.videoName || "";
			const posterUrl = safeTmdbStoredImageUrl(video.vImg);

			const $card = $("<div>").addClass("videoCar").on("click", function() {
				toDesc(videoId);
			}).attr("data-search-title", title);
			const $imageWrapper = $("<div>").addClass("VideoImage").css("border-color", borderColor);
			const image = createImageElement(posterUrl, "image");
			if (image) {
				$imageWrapper.append(image);
			}
			const icon = createImageElement(iconPath, "rating icon", { className: "card-icon" });
			const $rating = $("<p>");
			if (icon) {
				$rating.append($("<span>").append(icon));
			}
			$rating.append($("<span>").text(voteAveragePercentage + "%"));

			$card.append($imageWrapper).append($("<div>").append($rating).append($("<h5>").text(title)));
			$tvShows.append($card);
		});
	}

	function determineBorderColor(vote_average) {
		if (vote_average >= 8) return '#669900'; // Green
		else if (vote_average >= 6) return '#aec000'; // Light green
		else if (vote_average >= 4) return '#ff9900'; // Orange
		else if (vote_average >= 2) return '#cc0100'; // Red
		else return '#808080';  // Grey
	}

	function determineIconPath(vote_average) {
		if (vote_average >= 8) return 'assets/images/ranking_icons/ICONS_0000_Green.png';
		else if (vote_average >= 6) return 'assets/images/ranking_icons/ICONS_0001_LightGreen.png';
		else if (vote_average >= 4) return 'assets/images/ranking_icons/ICONS_0002_Orange.png';
		else if (vote_average >= 2) return 'assets/images/ranking_icons/ICONS_0003_Red.png';
		else return 'assets/images/ranking_icons/ICONS_0004_Grey.png';
	}

	// Event listener for genre selection
	$('.genre-select').change(function() {
		const selectedGenre = $(this).val();

		window.sessionStorage.setItem(storageKeys.genre, selectedGenre);
		window.sessionStorage.setItem("tvNumPage", defaultPage);
		handleDropdownChange();
	});

	// Event listener for year selection
	$('.year-select').change(function() {
		const selectedYear = $(this).val();

		window.sessionStorage.setItem(storageKeys.year, selectedYear);
		window.sessionStorage.setItem("tvNumPage", defaultPage);
		handleDropdownChange();
	});

	function handleDropdownChange() {
		const selectedOption = $('.sort-select').val() || defaultSortValue;
		let url = defaultSortUrl;

		switch (selectedOption) {
			case 'avg_desc':
				url = server + "/score/getTVAvgDesc";
				break;
			case 'avg_asc':
				url = server + "/score/getTVAvgAsc";
				break;
			case 'score_count_desc':
				url = server + "/score/getTVScoreCountDesc";
				break;
			case 'score_count_asc':
				url = server + "/score/getTVScoreCountAsc";
				break;
		}

		const page = Number(window.sessionStorage.getItem("tvNumPage")) || defaultPage;
		window.sessionStorage.setItem(storageKeys.sortValue, selectedOption);

		fetchVideos(url, page);
	}

	function updatePageNumber(page, hasNextPage) {
		updateMediaPagination(page, { hasNext: hasNextPage });
	}

	function configureSearchInput() {
		const $input = $(".moveInput");
		$input.attr("placeholder", "Search rated TV shows");
		$input.attr("autocomplete", "off");
		$input.on("input", scheduleSearchRefresh);
		$input.on("keydown", function(event) {
			if (event.key === "Enter") {
				event.preventDefault();
				refreshSearchNow();
			}
		});
	}

	function getMediaSearchQuery() {
		return String($(".moveInput").val() || "").trim();
	}

	function scheduleSearchRefresh() {
		window.sessionStorage.setItem("tvNumPage", defaultPage);
		window.clearTimeout(searchDebounceTimer);
		searchDebounceTimer = window.setTimeout(handleDropdownChange, 300);
	}

	function refreshSearchNow() {
		window.sessionStorage.setItem("tvNumPage", defaultPage);
		window.clearTimeout(searchDebounceTimer);
		handleDropdownChange();
	}

	function prePage() {
		let num = Number(window.sessionStorage.getItem("tvNumPage")) || defaultPage;
		if (num > 1) {
			num -= 1;
			window.sessionStorage.setItem("tvNumPage", num);
			handleDropdownChange();
		}
	}

	function nextPage() {
		let num = Number(window.sessionStorage.getItem("tvNumPage")) || defaultPage;
		num += 1;
		window.sessionStorage.setItem("tvNumPage", num);
		handleDropdownChange();
	}

	$('.sort-select').change(function() {
		window.sessionStorage.setItem("tvNumPage", defaultPage);
		handleDropdownChange();
	});

	$('.page-item').click(function() {
		const action = $(this).data('action');
		if (action === 'prev') {
			prePage();
		} else if (action === 'next') {
			nextPage();
		}
	});

	$('[data-clear-media-filters]').on("click", function() {
		resetMediaFilters();
	});

	function resetMediaFilters() {
		window.clearTimeout(searchDebounceTimer);
		resetEnhancedFilterSelects(".search");
		$(".moveInput").val("");
		window.sessionStorage.setItem(storageKeys.country, "");
		window.sessionStorage.setItem(storageKeys.genre, "");
		window.sessionStorage.setItem(storageKeys.year, "");
		window.sessionStorage.setItem(storageKeys.sortValue, defaultSortValue);
		window.sessionStorage.setItem("tvNumPage", defaultPage);
		handleDropdownChange();
	}

	// Event listener for country selection
	$('.country-select').change(function() {
		const selectedCountry = $(this).val();

		window.sessionStorage.setItem(storageKeys.country, selectedCountry);
		window.sessionStorage.setItem("tvNumPage", defaultPage);
		handleDropdownChange();
	});

	function initialize() {
		const country = window.sessionStorage.getItem(storageKeys.country) || "";
		const genre = window.sessionStorage.getItem(storageKeys.genre) || "";
		const year = window.sessionStorage.getItem(storageKeys.year) || "";
		const sortValue = window.sessionStorage.getItem(storageKeys.sortValue) || defaultSortValue;

		$('.sort-select').val(sortValue);
		if (!$('.sort-select').val()) {
			$('.sort-select').val(defaultSortValue);
		}
		$('.country-select').val(country);
		$('.genre-select').val(genre);
		$('.year-select').val(year);

		handleDropdownChange();
	}

	configureSearchInput();
	initialize();
	enhanceFilterSelects(".search");
});

function toDesc(id) {
	const videoId = safePositiveInteger(id);
	if (videoId !== null) {
		window.location.href = server + "/tv?id=" + videoId + "&type=tv";
	}
}

function loadClimateRatedOptions() {
	let climateRatedOptions = `
        <select class="form-select sort-select" aria-label="Default select example">
            <option value="avg_desc" selected>Sort</option>
            <option value="avg_asc">Rating average (asc)</option>
            <option value="avg_desc">Rating average (desc)</option>
            <option value="score_count_asc">Number of rates on each TV show (asc)</option>
            <option value="score_count_desc">Number of rates on each TV show (desc)</option>
        </select>
    `;
	$('#sortSelectContainer').html(climateRatedOptions);
}
