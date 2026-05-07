// CSRF token setup for AJAX requests
var csrfToken = $('input[name="_csrf"]').val();

fetchCurrentUser()
	.done(function(user) {
		$("#userName").text(user.username || "");
	})
	.fail(function() {
		redirectToLogin();
	});

$("#toAccountSettings").on("click", function() {
	window.location.href = '/user-settings';
});

$("#toMyRatings").on("click", () => { window.location.href = '/user-ratings'; });


function deleteFuntion() {
	if (!$("#deleteForm")[0].checkValidity()) {
		$("#deleteForm")[0].reportValidity();
		return;
	}

	var formDataArray = $('#deleteForm').serializeArray();


	var formDataObj = {};
	formDataArray.forEach(function(field) {
		formDataObj[field.name] = field.value;
	});

	// Convert form data object to JSON string
	var formDataJson = JSON.stringify(formDataObj);


	$.ajax({
		type: "DELETE",
		url: server + "/user/delete",
		data: formDataJson,
		contentType: "application/json;charset=UTF-8",
		headers: {
			'X-CSRF-TOKEN': csrfToken  // Add CSRF token to the request headers
		},
		success: function(response) {
			alert("Success: " + response);
			window.localStorage.removeItem("user");
			window.location.href = server + "/logout";

		},
		error: function(jqXHR) {
			console.error("Error occurred:", jqXHR.statusText);
			alert("Sorry, there was a problem with the deletion. Please try again." + jqXHR.responseText);
		}
	});

}
