var queryString = decodeURIComponent(window.location.search);
var params = new URLSearchParams(queryString);
var id = params.get('id');
$.ajax({
	url: "https://api.themoviedb.org/3/person/" + id,
	method: "get",
	headers: {
		"Authorization": jwt,
		"accept": "application/json"
	},
	success: function(resp) {

		$("#celebrityName").html(resp.name);
		$("#Birthday").html(resp.birthday)
		$("#Birthplace").html(resp.place_of_birth)
		$("#Introduction").html(resp.biography.slice(0, 600) + "...")
		$(".image").html("<img src='" + imgServer + resp.profile_path + "' alt='image'>")
	}
})


$.ajax({
	url: " https://api.themoviedb.org/3/person/" + id + "/combined_credits",
	method: "get",
	headers: {
		"Authorization": jwt,
		"accept": "application/json"
	},
	success: function(resp) {


		let castStr = "";
		for (const respElement of resp.crew.slice(0, 5)) {
			castStr += "    <div onclick='toDesc(" + respElement.id + ",\"" + respElement.media_type + "\")' class=\"knownItem flex-item\">\n" +
				"                 <div class=\"VideoImage\">\n" +
				"                         <img src='" + imgServer + respElement.poster_path + "' alt='not availble'>\n" +
				"                    </div>\n" +
				"                    <p><span><i class=\"bi bi-star-fill\"></i></span>\n" +
				"                          <span>" + respElement.vote_average + "</span>\n" +
				"                    </p>\n" +
				"                  <h5>" + respElement.title + "</h5>\n" +
				"    </div>"
		}
		for (const respElement of resp.cast.slice(0, 5)) {
			castStr += "    <div onclick='toDesc(" + respElement.id + ",\"" + respElement.media_type + "\")' class=\"knownItem flex-item\">\n" +
				"                 <div class=\"VideoImage\">\n" +
				"                         <img alt='not availble'  src='" + imgServer + respElement.poster_path + "' >\n" +
				"                    </div>\n" +
				"                    <p><span><i class=\"bi bi-star-fill\"></i></span>\n" +
				"                          <span>" + respElement.vote_average + "</span>\n" +
				"                    </p>\n" +
				"                  <h5>" + respElement.title + "</h5>\n" +
				"    </div>"
		}
		$(".known").html(castStr)
	}
})

function toDesc(id, type) {
	if (type == "tv") {
		window.location.href = server + "/tv?id=" + id
	} else if (type == "movie") {
		window.location.href = server + "/movie?id=" + id
	}
}