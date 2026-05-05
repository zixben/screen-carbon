// CSRF token setup for AJAX requests
var csrfToken = $('input[name="_csrf"]').val();

let user = JSON.parse(window.localStorage.getItem("user"))

// Existing scripts can remain below or above this script
console.log(user ? user.username : "No user logged in");

$("#userName").html(user.username);

$("#toAccountSettings").on("click", function() {
	window.location.href = '/user-settings';
});

$("#toMyRatings").on("click", () => { window.location.href = '/user-ratings'; });

$.ajax({
	url: server + "/score/getScoreList/" + user.id,
	contentType: "application/json;charset=UTF-8",
	headers: {
		'X-CSRF-TOKEN': csrfToken  // Add CSRF token to the request headers
	},
	success: function(resp) {
		console.log(resp);

		let htmlStr = "";


		for (const respElement of resp) {

			htmlStr += " <div   onclick='toDesc2(" + respElement.vId + ", \"" + respElement.videoType + "\")' class=\"ratings\">\n" +
				"                    <img src=\"" + respElement.vImg + "\" alt=\"img\">\n" +
				"                    <div class=\"ratingInfo\">\n" +
				"                        <h4 id=\"title\">" + respElement.videoName.substring(0, 20) + "</h4>\n" +
				"                        <h5>Your rating</h5>\n" +
				"                        <button type=\"button\" class=\"btn btn-info\">" + respElement.score.substring(0, 4) + "/10</button>\n" +
				"                    </div>\n" +
				"                </div>"
		}
		$(".menuDesc").html(htmlStr);
	}
});


function toDesc2(vId, vType) {

	let url = "https://api.themoviedb.org/3/movie/" + vId
	if (vType == 'tv') {
		url = "https://api.themoviedb.org/3/tv/" + vId
	}

	$.ajax({
		url: url,
		method: "get",
		headers: {
			"Authorization": jwt,
			"accept": "application/json"
		},
		success: function(resp) {
			console.log(resp);
			console.log(server + `/${vType}?id=${vId}&type=${vType}`);

			window.location.href = server + `/${vType}?id=${resp.id}&type=${vType}`;



		},
		error: function(xhr, status, error) {
			console.error("An error occurred: " + status + ", " + error + ", " + xhr);
		}
	})

}