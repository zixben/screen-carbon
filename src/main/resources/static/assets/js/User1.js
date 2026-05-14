// CSRF token setup for AJAX requests
var csrfToken = $('input[name="_csrf"]').val();
var deleteConfirmationModal = null;
var deleteConfirmationModalElement = document.getElementById("deleteConfirmationModal");

if (deleteConfirmationModalElement && typeof bootstrap !== "undefined") {
	deleteConfirmationModal = new bootstrap.Modal(deleteConfirmationModalElement);
}

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

$("#openDeleteConfirmation").on("click", function(event) {
	if (!isDeleteFormValid()) {
		event.preventDefault();
		event.stopPropagation();
	}
});

$("#confirmDeleteAccount").on("click", function() {
	deleteCurrentUser();
});

function deleteFuntion() {
	if (!isDeleteFormValid()) {
		return;
	}

	if (deleteConfirmationModal) {
		deleteConfirmationModal.show();
		return;
	}

	if (window.confirm("Delete your account permanently?")) {
		deleteCurrentUser();
	}
}

function deleteCurrentUser() {
	if (!isDeleteFormValid()) {
		if (deleteConfirmationModal) {
			deleteConfirmationModal.hide();
		}
		return;
	}

	var formDataArray = $('#deleteForm').serializeArray();


	var formDataObj = {};
	formDataArray.forEach(function(field) {
		formDataObj[field.name] = field.value;
	});

	// Convert form data object to JSON string
	var formDataJson = JSON.stringify(formDataObj);

	$("#confirmDeleteAccount").prop("disabled", true);

	$.ajax({
		type: "DELETE",
		url: server + "/user/delete",
		data: formDataJson,
		contentType: "application/json;charset=UTF-8",
		headers: {
			'X-CSRF-TOKEN': csrfToken  // Add CSRF token to the request headers
		},
		success: function(response) {
			showDeletionSuccess(response);
		},
		error: function(jqXHR) {
			console.error("Error occurred:", jqXHR.statusText);
			showDeletionError(jqXHR.responseText);
		},
		complete: function() {
			$("#confirmDeleteAccount").prop("disabled", false);
		}
	});

}

function showDeletionSuccess(response) {
	afterDeleteConfirmationCloses(function() {
		alert("Success: " + response);
		window.location.href = server + "/logout";
	});
}

function showDeletionError(responseText) {
	afterDeleteConfirmationCloses(function() {
		alert("Sorry, there was a problem with the deletion. Please try again." + responseText);
	});
}

function afterDeleteConfirmationCloses(callback) {
	if (deleteConfirmationModal && deleteConfirmationModalElement && $(deleteConfirmationModalElement).hasClass("show")) {
		$(deleteConfirmationModalElement).one("hidden.bs.modal", callback);
		deleteConfirmationModal.hide();
		return;
	}

	callback();
}

function isDeleteFormValid() {
	if (!$("#deleteForm")[0].checkValidity()) {
		$("#deleteForm")[0].reportValidity();
		return false;
	}
	return true;
}
