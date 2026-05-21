$(document).ready(function () {
    const csrfToken = $('input[name="_csrf"]').first().val();

    function showError(selector, message) {
        const $formGroup = $(selector).closest(".form-group");
        $formGroup.children(".auth-field-error").remove();
        $("<div>", {
            "class": "auth-field-error",
            text: message
        }).appendTo($formGroup);
    }

    function removeError(selector) {
        $(selector).closest(".form-group").children(".auth-field-error").remove();
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

    function responseJson(response) {
        return response.json().catch(function () {
            return {};
        });
    }

    function setRecoveryMessage(message, type) {
        $("#responseMessage")
            .removeClass("d-none alert-success alert-danger")
            .addClass("alert " + (type === "success" ? "alert-success" : "alert-danger"))
            .text(message);
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

        fetch(server + "/user/password-recovery", {
            method: "POST",
            headers: {
                "Content-Type": "application/json;charset=UTF-8",
                "X-CSRF-TOKEN": csrfToken
            },
            body: JSON.stringify({ email: $("#email").val() })
        })
            .then(function (response) {
                return responseJson(response).then(function (data) {
                    if (!response.ok) {
                        throw new Error(data.message || "An error occurred. Please try again.");
                    }
                    return data;
                });
            })
            .then(function (response) {
                setRecoveryMessage(response.message, "success");
            })
            .catch(function (error) {
                setRecoveryMessage(error.message || "An error occurred. Please try again.", "error");
            });
    });
});
