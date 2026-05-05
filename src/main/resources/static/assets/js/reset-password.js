$(document).ready(function() {

	// CSRF token setup for AJAX requests
	var csrfToken = $('input[name="_csrf"]').val();

	$('#resetPasswordForm').on('submit', function(e) {
		e.preventDefault(); // Prevent the default form submission

		const token = $('#token').val();
		const newPassword = $('#newPassword').val();
		const confirmPassword = $('#confirmPassword').val();

		// Check if the token is present
		if (!token) {
			$('#responseMessage').text('Missing token. Please try again.').addClass('alert alert-danger');
			return;
		}

		// Perform AJAX request to update the password
		$.ajax({
			type: 'POST',
			url: server + '/user/update-password', // Use the server variable defined in server.js
			contentType: 'application/x-www-form-urlencoded', // Send data as URL-encoded form data
			data: { token: token, newPassword: newPassword, confirmPassword: confirmPassword }, // Send the token and new password as form data
			headers: {
				'X-CSRF-TOKEN': csrfToken  // Add CSRF token to the request headers
			},
			success: function(response) {
				$('#responseMessage').text(response.message).removeClass('alert-danger').addClass('alert alert-success');
				setTimeout(() => window.location.href = server + '/login', 2000); // Redirect to login page after a short delay
			},
			error: function(xhr) {
				// Display the error message sent by the server
				const errorMessage = xhr.responseJSON ? xhr.responseJSON.message : 'An error occurred. Please try again.';
				$('#responseMessage').text(errorMessage).removeClass('alert-success').addClass('alert alert-danger');

			}
		});
	});
});