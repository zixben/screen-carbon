$(document).ready(function () {
    $('#passwordRecoveryForm').on('submit', function (e) {
        e.preventDefault();
        const email = $('#email').val();

        $.ajax({
            type: 'POST',
            url: server + '/user/password-recovery',
            contentType: 'application/json',
            data: JSON.stringify({ email: email }),
            success: function (response) {
                // Display the success message coming from the back-end
                $('#responseMessage')
                    .removeClass('d-none alert-danger')
                    .addClass('alert alert-success')
                    .text(response.message); // Assuming the server returns { "message": "..." }
            },
            error: function (error) {
                // Display the error message coming from the back-end
                const errorMessage = error.responseJSON && error.responseJSON.message
                    ? error.responseJSON.message
                    : 'An error occurred. Please try again.'; // Fallback message

                $('#responseMessage')
                    .removeClass('d-none alert-success')
                    .addClass('alert alert-danger')
                    .text(errorMessage);
            }
        });
    });
});
