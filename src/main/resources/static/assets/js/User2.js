// CSRF token setup for AJAX requests
var csrfToken = $('input[name="_csrf"]').val();

$("#toAccountSettings").on("click", function() {
	window.location.href = '/user-settings';
});

$("#toMyRatings").on("click", () => { window.location.href = '/user-ratings'; });

fetchCurrentUser()
	.done(function(user) {
		$("#userName").text(user.username || "");
		loadUserRatings(user.id);
	})
	.fail(function() {
		redirectToLogin();
	});

function loadUserRatings(userId) {
	const safeUserId = safePositiveInteger(userId);
	if (safeUserId === null) {
		redirectToLogin();
		return;
	}

	$.ajax({
		url: server + "/score/getScoreList/" + safeUserId,
		contentType: "application/json;charset=UTF-8",
		headers: {
			'X-CSRF-TOKEN': csrfToken
		},
		success: function(resp) {
			const $menuDesc = $(".menuDesc").empty();

			for (const respElement of resp) {
				const videoId = Number(respElement.vId);
				const videoType = respElement.videoType === "tv" ? "tv" : "movie";
				if (!Number.isInteger(videoId) || videoId <= 0) {
					continue;
				}

				const imageUrl = safeTmdbStoredImageUrl(respElement.vImg);
				const videoName = String(respElement.videoName || "").substring(0, 20);
				const score = String(respElement.score || "").substring(0, 4);

				const $rating = $("<div>").addClass("ratings").on("click", function() {
					toDesc2(videoId, videoType);
				});
				const image = createImageElement(imageUrl, "img");
				if (image) {
					$rating.append(image);
				}
				const $ratingInfo = $("<div>").addClass("ratingInfo")
					.append($("<h4>").attr("id", "title").text(videoName))
					.append($("<h5>").text("Your rating"))
					.append($("<button>").attr("type", "button").addClass("btn btn-info").text(score + "/10"));

				$rating.append($ratingInfo);
				$menuDesc.append($rating);
			}
		}
	});
}


function toDesc2(vId, vType) {
	const videoId = safePositiveInteger(vId);
	const videoType = safeVideoType(vType);
	if (videoId === null || !videoType) {
		return;
	}

	window.location.href = server + `/${videoType}?id=${videoId}&type=${videoType}`;
}
