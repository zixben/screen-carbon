$(document).ready(function () {
    const csrfToken = $('input[name="_csrf"]').first().val();

    function showError(selector, message) {
        $(selector).next(".error").remove();
        $(selector).after(`<div class="error" style="color:red;">${message}</div>`);
    }

    function removeError(selector) {
        $(selector).next(".error").remove();
    }

    function validateLoginForm() {
        let isValid = true;
        const username = $("#username").val().trim();
        const password = $("#password").val().trim();
        const code = $("#code").val().trim();

        if (username.length === 0) {
            showError("#username", "Please enter your username.");
            isValid = false;
        } else {
            removeError("#username");
        }

        if (password.length === 0) {
            showError("#password", "Please enter your password.");
            isValid = false;
        } else {
            removeError("#password");
        }

        if (code.length === 0) {
            showError("#code", "Please enter the code.");
            isValid = false;
        } else {
            removeError("#code");
        }

        return isValid;
    }

    function formToJson($form) {
        const formDataObj = {};
        $.each($form.serializeArray(), function (i, field) {
            formDataObj[field.name] = field.value;
        });
        return JSON.stringify(formDataObj);
    }

    function submitLogin(e) {
        e.preventDefault();

        if (!validateLoginForm()) {
            return;
        }

        $.ajax({
            type: "POST",
            url: server + "/user/login",
            data: formToJson($("#loginRequestForm")),
            contentType: "application/json;charset=UTF-8",
            headers: {
                "X-CSRF-TOKEN": csrfToken
            },
            success: function (resp) {
                alert(resp.message);
                window.location.href = resp.role === "ADMIN" ? server + "/admin" : server + "/";
            },
            error: function (jqXHR) {
                const response = jqXHR.responseJSON;
                alert(response && response.message ? response.message : "An unexpected error occurred.");
            }
        });
    }

    $(".login").on("click", submitLogin);
    $("#loginRequestForm").on("submit", submitLogin);

    $("#imgpw").on("click", function () {
        const date = new Date().getMilliseconds();
        $("#imgpw").attr("src", server + "/user/getCode?" + date);
    });
    $("#imgpw").attr("src", server + "/user/getCode");

    $("#forgotPasswordLink").on("click", function (e) {
        e.preventDefault();
        $("#loginForm").addClass("d-none");
        $("#passwordRecoveryPanel").removeClass("d-none");
    });

    $("#backToLoginLink").on("click", function (e) {
        e.preventDefault();
        $("#passwordRecoveryPanel").addClass("d-none");
        $("#loginForm").removeClass("d-none");
    });

    $("#passwordRecoveryForm").on("submit", function (e) {
        e.preventDefault();

        $.ajax({
            type: "POST",
            url: server + "/user/password-recovery",
            contentType: "application/json;charset=UTF-8",
            data: JSON.stringify({ email: $("#email").val() }),
            headers: {
                "X-CSRF-TOKEN": csrfToken
            },
            success: function (response) {
                $("#responseMessage")
                    .removeClass("d-none alert-danger")
                    .addClass("alert alert-success")
                    .text(response.message);
            },
            error: function (error) {
                const errorMessage = error.responseJSON && error.responseJSON.message
                    ? error.responseJSON.message
                    : "An error occurred. Please try again.";
                $("#responseMessage")
                    .removeClass("d-none alert-success")
                    .addClass("alert alert-danger")
                    .text(errorMessage);
            }
        });
    });
});
