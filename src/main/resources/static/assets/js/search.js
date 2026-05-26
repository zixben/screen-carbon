const params = new URLSearchParams(window.location.search);
const initialSearchValue = String(params.get("value") || "").trim();
const requestedType = params.get("type");
const requestedPage = Number(params.get("page"));
const searchTypes = {
	movie: {
		endpoint: "/search/movie",
		label: "Films",
		itemLabel: "Film",
		resultsLabel: "films",
		icon: "bi-film"
	},
	tv: {
		endpoint: "/search/tv",
		label: "TV shows",
		itemLabel: "TV show",
		resultsLabel: "TV shows",
		icon: "bi-tv"
	},
	person: {
		endpoint: "/search/person",
		label: "Cast & crew",
		itemLabel: "Cast & crew",
		resultsLabel: "people",
		icon: "bi-people"
	}
};
const searchState = {
	query: initialSearchValue,
	type: normalizeSearchType(requestedType),
	page: Number.isInteger(requestedPage) && requestedPage > 0 ? requestedPage : 1,
	totalPages: 1,
	requestId: 0
};
const maxSearchPage = 500;
let climateVideoCache = null;

function normalizeSearchType(type) {
	return Object.prototype.hasOwnProperty.call(searchTypes, type) ? type : "movie";
}

function setActiveTab(type) {
	$(".search-tabs__button").each(function() {
		const isActive = $(this).data("search-type") === type;
		$(this).toggleClass("active", isActive);
		$(this).attr("aria-selected", String(isActive));
	});
}

function setSearchSummary(message) {
	$("#searchResultsSummary").text(message || "");
}

function updateSearchValueDisplay() {
	const query = searchState.query || "";
	$(".searchValue").text(query || "No search term");
	$("#searchResultsInput").val(query);
}

function setSearchMessage(message, className) {
	const $items = $(".items").empty();
	$("<div>")
		.addClass(className || "search-empty")
		.text(message)
		.appendTo($items);
}

function updateSearchUrl(type, page) {
	const nextParams = new URLSearchParams();
	nextParams.set("value", searchState.query);
	nextParams.set("type", type);
	nextParams.set("page", String(page));
	window.history.replaceState(null, "", window.location.pathname + "?" + nextParams.toString());
}

function fetchClimateVideo() {
	if (climateVideoCache) {
		return Promise.resolve(climateVideoCache);
	}

	return new Promise((resolve) => {
		$.ajax({
			url: server + "/score/getOrderAvg",
			method: "get",
			headers: {
				"accept": "application/json"
			},
			success: function(response) {
				climateVideoCache = Array.isArray(response) ? response : [];
				resolve(climateVideoCache);
			},
			error: function(xhr, status, error) {
				console.error("An error occurred: " + status + ", " + error + ", " + xhr);
				climateVideoCache = [];
				resolve(climateVideoCache);
			}
		});
	});
}

function fetchSearchResults(type, page) {
	const searchParams = new URLSearchParams({
		query: searchState.query,
		include_adult: "false",
		language: "en-US",
		page: String(page)
	});

	return new Promise((resolve, reject) => {
		$.ajax({
			url: "https://api.themoviedb.org/3" + searchTypes[type].endpoint + "?" + searchParams.toString(),
			method: "get",
			headers: {
				"Authorization": jwt,
				"accept": "application/json"
			},
			success: resolve,
			error: function(xhr, status, error) {
				console.error("An error occurred: " + status + ", " + error + ", " + xhr);
				reject(error);
			}
		});
	});
}

function loadSearch(type, page) {
	const normalizedType = normalizeSearchType(type);
	const normalizedPage = Math.min(maxSearchPage, Math.max(1, Number(page) || 1));
	const query = searchState.query;
	const requestId = searchState.requestId + 1;
	searchState.requestId = requestId;
	searchState.type = normalizedType;
	searchState.page = normalizedPage;

	updateSearchValueDisplay();
	setActiveTab(normalizedType);
	setSearchSummary("Loading " + searchTypes[normalizedType].label.toLowerCase() + "...");
	setSearchMessage("Loading results...", "search-loading");
	updateMediaPagination(normalizedPage, {
		totalPages: searchState.totalPages,
		isLoading: true
	});
	updateSearchUrl(normalizedType, normalizedPage);

	if (!query) {
		searchState.page = 1;
		setSearchSummary("");
		setSearchMessage("No search term provided.", "search-empty");
		updateSearchUrl(normalizedType, 1);
		updateMediaPagination(1, { totalPages: 1 });
		return;
	}

	Promise.all([
		fetchSearchResults(normalizedType, normalizedPage),
		normalizedType === "person" ? Promise.resolve([]) : fetchClimateVideo()
	])
		.then(function(results) {
			if (requestId !== searchState.requestId) {
				return;
			}

			const response = results[0] || {};
			const climateVideo = results[1] || [];
			const totalPages = Math.min(maxSearchPage, Math.max(1, Number(response.total_pages) || 1));
			const totalResults = Math.max(0, Number(response.total_results) || 0);

			if (normalizedPage > totalPages) {
				loadSearch(normalizedType, totalPages);
				return;
			}

			searchState.totalPages = totalPages;
			searchState.page = normalizedPage;
			setSearchSummary(formatSearchSummary(totalResults, normalizedType));
			renderResults(filterSafeTmdbResults(response.results), normalizedType, climateVideo);
			updateMediaPagination(normalizedPage, { totalPages: totalPages });
		})
		.catch(function(error) {
			if (requestId !== searchState.requestId) {
				return;
			}

			console.error("An error occurred while fetching search results: ", error);
			setSearchSummary("Search unavailable");
			setSearchMessage("Search results could not be loaded.", "search-empty");
			updateMediaPagination(normalizedPage, { hasNext: false });
		});
}

function formatSearchSummary(totalResults, type) {
	if (totalResults === 1) {
		return "1 result";
	}

	return totalResults.toLocaleString() + " " + searchTypes[type].resultsLabel;
}

function renderResults(results, type, climateVideo) {
	const $items = $(".items").empty();
	let renderedAny = false;

	for (const result of results) {
		const card = createResultCard(result, type, climateVideo);
		if (card) {
			$items.append(card);
			renderedAny = true;
		}
	}

	if (!renderedAny) {
		setSearchMessage("No results found.", "search-empty");
	}
}

function createResultCard(result, type, climateVideo) {
	if (!isSafeTmdbMedia(result)) {
		return null;
	}

	const resultId = safePositiveInteger(result.id);
	if (resultId === null) {
		return null;
	}

	const title = result.title || result.name || "";
	const posterUrl = safeTmdbImageUrl(result.poster_path || result.profile_path);
	const metaItems = getResultMeta(result, type);
	const overview = getResultOverview(result, type);
	const mediaTypeCode = type === "person" ? 3 : (type === "movie" ? 1 : 0);

	const $card = $("<article>").addClass("search-result-card");
	const detailsUrl = getDetailsUrl(resultId, type);
	const $poster = $("<a>")
		.attr("href", detailsUrl)
		.addClass("search-result-card__poster")
		.attr("aria-label", "View " + title);
	const placeholderOptions = {
		placeholderLabel: type === "person" ? "No photo" : "No poster",
		placeholderIcon: type === "person" ? "bi bi-person" : "bi " + searchTypes[type].icon,
		placeholderClassName: "media-placeholder--search media-placeholder--compact"
	};
	const posterImage = createImageElement(posterUrl, title || "Search result image", placeholderOptions);
	if (posterImage) {
		$poster.append(posterImage);
	} else {
		$poster.append(createImagePlaceholder(placeholderOptions.placeholderLabel, placeholderOptions));
	}

	const $body = $("<a>")
		.attr("href", detailsUrl)
		.addClass("search-result-card__body")
		.attr("aria-label", "View " + title);
	$body.append($("<h3>").addClass("search-result-card__title").text(title));
	$body.append($("<div>").addClass("search-result-card__meta").text(metaItems.join(" / ")));
	$body.append($("<p>").addClass("search-result-card__overview").text(overview));

	const $action = $("<div>").addClass("search-result-card__action");
	if (mediaTypeCode === 3) {
		$action.append(
			$("<a>")
				.attr("href", detailsUrl)
				.addClass("search-result-card__details")
				.text("View details")
		);
	} else {
		$action.append(createRatingButton(resultId, type, climateVideo));
	}

	$card.append($poster).append($body).append($action);
	return $card;
}

function createRatingButton(resultId, type, climateVideo) {
	const climateRating = climateVideo.find(function(item) {
		return Number(item.vId) === resultId && item.videoType === type;
	});
	const score = climateRating ? Number(climateRating.score) : null;
	const isRated = Number.isFinite(score);
	const $button = $("<a>")
		.attr("href", getRateUrl(resultId, type))
		.addClass("search-result-card__rating")
		.attr("aria-label", "Rate " + searchTypes[type].itemLabel.toLowerCase());

	if (isRated) {
		const scoreText = (score * 10).toFixed(1).replace(/\.0$/, "") + "%";
		const icon = createImageElement(determineIconPath(score), "rating icon", { className: "card-icon" });
		if (icon) {
			$button.append(icon);
		}
		$button.append(
			$("<span>").addClass("search-result-card__rating-value")
				.css("color", determineBorderColor(score))
				.text(scoreText)
		);
		$button.append($("<span>").addClass("search-result-card__rating-label").text("Rate again"));
	} else {
		$button.append($("<span>").addClass("search-result-card__rating-value").text("Rate"));
		$button.append($("<span>").addClass("search-result-card__rating-label").text("Not yet rated"));
	}

	return $button;
}

function getDetailsUrl(id, type) {
	if (type === "movie") {
		return server + "/movie?id=" + id + "&type=movie";
	}
	if (type === "tv") {
		return server + "/tv?id=" + id + "&type=tv";
	}
	return server + "/details?id=" + id;
}

function getRateUrl(id, type) {
	if (type === "movie") {
		return server + "/rate?id=" + id + "&type=movie";
	}
	if (type === "tv") {
		return server + "/rate?id=" + id + "&type=tv";
	}
	return server + "/details?id=" + id;
}

function getResultMeta(result, type) {
	const metaItems = [searchTypes[type].itemLabel];
	const year = getResultYear(result, type);
	if (year) {
		metaItems.push(year);
	}
	if (type === "person" && result.known_for_department) {
		metaItems.push(result.known_for_department);
	}
	return metaItems;
}

function getResultYear(result, type) {
	const date = type === "movie" ? result.release_date : result.first_air_date;
	if (typeof date !== "string" || date.length < 4) {
		return "";
	}
	return date.substring(0, 4);
}

function getResultOverview(result, type) {
	if (type === "person") {
		const knownFor = filterSafeTmdbResults(result.known_for)
			.map(function(item) {
				return item.title || item.name;
			})
			.filter(Boolean);
		return knownFor.length ? "Known for " + knownFor.slice(0, 3).join(", ") + "." : "No profile details available.";
	}

	return result.overview || "No overview available.";
}

function toRate(id, type) {
	const resultId = safePositiveInteger(id);
	if (resultId === null) {
		return false;
	}
	if (type === "movie") {
		window.location.href = server + "/rate?id=" + resultId + "&type=movie";
	} else if (type === "tv") {
		window.location.href = server + "/rate?id=" + resultId + "&type=tv";
	} else {
		window.location.href = server + "/details?id=" + resultId;
	}
	return false;
}

function toDesc(id, type) {
	const resultId = safePositiveInteger(id);
	if (resultId === null) {
		return;
	}
	if (type === "movie") {
		window.location.href = server + "/movie?id=" + resultId + "&type=movie";
	} else if (type === "tv") {
		window.location.href = server + "/tv?id=" + resultId + "&type=tv";
	} else {
		window.location.href = server + "/details?id=" + resultId;
	}
}

function determineBorderColor(vote_average) {
	if (vote_average >= 8) return '#669900';
	else if (vote_average >= 6) return '#aec000';
	else if (vote_average >= 4) return '#ff9900';
	else if (vote_average >= 2) return '#cc0100';
	else return '#808080';
}

function determineIconPath(vote_average) {
	if (vote_average >= 8) return 'assets/images/ranking_icons/ICONS_0000_Green.png';
	else if (vote_average >= 6) return 'assets/images/ranking_icons/ICONS_0001_LightGreen.png';
	else if (vote_average >= 4) return 'assets/images/ranking_icons/ICONS_0002_Orange.png';
	else if (vote_average >= 2) return 'assets/images/ranking_icons/ICONS_0003_Red.png';
	else return 'assets/images/ranking_icons/ICONS_0004_Grey.png';
}

$(document).ready(function() {
	updateSearchValueDisplay();

	$("#searchResultsForm").on("submit", function(event) {
		event.preventDefault();
		const nextQuery = String($("#searchResultsInput").val() || "").trim();
		searchState.query = nextQuery;
		searchState.totalPages = 1;
		loadSearch(searchState.type, 1);
	});

	$(".search-tabs__button").on("click", function() {
		loadSearch($(this).data("search-type"), 1);
	});

	$(".page-item").on("click", function() {
		if (this.disabled) {
			return;
		}

		const action = $(this).data("action");
		if (action === "prev") {
			loadSearch(searchState.type, searchState.page - 1);
		} else if (action === "next") {
			loadSearch(searchState.type, searchState.page + 1);
		}
	});

	loadSearch(searchState.type, searchState.page);
});
