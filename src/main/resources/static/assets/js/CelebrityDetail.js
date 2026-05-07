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
		$(".image").html(profileUrl ? "<img src='" + profileUrl + "' alt='image'>" : "")
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


		let castStr = "";
		for (const respElement of (resp.crew || []).slice(0, 5)) {
			const itemId = safePositiveInteger(respElement.id);
			const mediaType = respElement.media_type === "tv" ? "tv" : respElement.media_type === "movie" ? "movie" : "";
			if (itemId === null || mediaType === "") {
				continue;
			}
			const posterUrl = safeTmdbImageUrl(respElement.poster_path);
			castStr += "    <div onclick='toDesc(" + itemId + ",\"" + mediaType + "\")' class=\"knownItem flex-item\">\n" +
				"                 <div class=\"VideoImage\">\n" +
				"                         <img src='" + posterUrl + "' alt='not availble'>\n" +
				"                    </div>\n" +
				"                    <p><span><i class=\"bi bi-star-fill\"></i></span>\n" +
				"                          <span>" + escapeHtml(respElement.vote_average || "") + "</span>\n" +
				"                    </p>\n" +
				"                  <h5>" + escapeHtml(respElement.title || respElement.name || "") + "</h5>\n" +
				"    </div>"
		}
		for (const respElement of (resp.cast || []).slice(0, 5)) {
			const itemId = safePositiveInteger(respElement.id);
			const mediaType = respElement.media_type === "tv" ? "tv" : respElement.media_type === "movie" ? "movie" : "";
			if (itemId === null || mediaType === "") {
				continue;
			}
			const posterUrl = safeTmdbImageUrl(respElement.poster_path);
			castStr += "    <div onclick='toDesc(" + itemId + ",\"" + mediaType + "\")' class=\"knownItem flex-item\">\n" +
				"                 <div class=\"VideoImage\">\n" +
				"                         <img alt='not availble'  src='" + posterUrl + "' >\n" +
				"                    </div>\n" +
				"                    <p><span><i class=\"bi bi-star-fill\"></i></span>\n" +
				"                          <span>" + escapeHtml(respElement.vote_average || "") + "</span>\n" +
				"                    </p>\n" +
				"                  <h5>" + escapeHtml(respElement.title || respElement.name || "") + "</h5>\n" +
				"    </div>"
		}
		$(".known").html(castStr)
	}
})

function toDesc(id, type) {
	const itemId = safePositiveInteger(id);
	if (itemId === null) {
		return;
	}
	if (type == "tv") {
		window.location.href = server + "/tv?id=" + itemId
	} else if (type == "movie") {
		window.location.href = server + "/movie?id=" + itemId
	}
}
