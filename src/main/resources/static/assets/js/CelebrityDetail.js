var queryString = decodeURIComponent(window.location.search);
var params = new URLSearchParams(queryString);
var id = safePositiveInteger(params.get('id'));

if (id === null) {
	throw new Error("Invalid person id.");
}

$.ajax({
	url: "https://api.themoviedb.org/3/person/" + id,
	method: "get",
	headers: {
		"Authorization": jwt,
		"accept": "application/json"
	},
	success: function(resp) {

		$("#celebrityName").text(resp.name || "");
		$("#Birthday").text(resp.birthday || "")
		$("#Birthplace").text(resp.place_of_birth || "")
		$("#Introduction").text(String(resp.biography || "").slice(0, 600) + "...")
		const profileUrl = safeTmdbImageUrl(resp.profile_path);
		setImageContent(".image", profileUrl, "image");
	}
})


$.ajax({
	url: "https://api.themoviedb.org/3/person/" + id + "/combined_credits",
	method: "get",
	headers: {
		"Authorization": jwt,
		"accept": "application/json"
	},
	success: function(resp) {


		const $known = $(".known").empty();
		for (const respElement of (resp.crew || []).slice(0, 5)) {
			const itemId = safePositiveInteger(respElement.id);
			const mediaType = respElement.media_type === "tv" ? "tv" : respElement.media_type === "movie" ? "movie" : "";
			if (itemId === null || mediaType === "") {
				continue;
			}
			const posterUrl = safeTmdbImageUrl(respElement.poster_path);
			appendKnownItem($known, itemId, mediaType, posterUrl, respElement.vote_average, respElement.title || respElement.name || "");
		}
		for (const respElement of (resp.cast || []).slice(0, 5)) {
			const itemId = safePositiveInteger(respElement.id);
			const mediaType = respElement.media_type === "tv" ? "tv" : respElement.media_type === "movie" ? "movie" : "";
			if (itemId === null || mediaType === "") {
				continue;
			}
			const posterUrl = safeTmdbImageUrl(respElement.poster_path);
			appendKnownItem($known, itemId, mediaType, posterUrl, respElement.vote_average, respElement.title || respElement.name || "");
		}
	}
})

function appendKnownItem($container, itemId, mediaType, posterUrl, voteAverage, title) {
	const $item = $("<div>").addClass("knownItem flex-item").on("click", function() {
		toDesc(itemId, mediaType);
	});
	const $imageWrapper = $("<div>").addClass("VideoImage");
	const image = createImageElement(posterUrl, "not available");
	if (image) {
		$imageWrapper.append(image);
	}

	const $rating = $("<p>")
		.append($("<span>").append($("<i>").addClass("bi bi-star-fill")))
		.append($("<span>").text(voteAverage || ""));

	$item.append($imageWrapper)
		.append($rating)
		.append($("<h5>").text(title || ""));
	$container.append($item);
}

function toDesc(id, type) {
	const itemId = safePositiveInteger(id);
	if (itemId === null) {
		return;
	}
	if (type == "tv") {
		window.location.href = server + "/tv?id=" + itemId + "&type=tv";
	} else if (type == "movie") {
		window.location.href = server + "/movie?id=" + itemId + "&type=movie";
	}
}
