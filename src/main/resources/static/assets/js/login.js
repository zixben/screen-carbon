document.addEventListener("DOMContentLoaded", function () {
    const csrfInput = document.querySelector('input[name="_csrf"]');
    const csrfToken = csrfInput ? csrfInput.value : "";
    const loginForm = document.getElementById("loginRequestForm");
    const passwordRecoveryForm = document.getElementById("passwordRecoveryForm");
    const loginPanel = document.getElementById("loginForm");
    const passwordRecoveryPanel = document.getElementById("passwordRecoveryPanel");
    const captchaImage = document.getElementById("imgpw");
    const forgotPasswordLink = document.getElementById("forgotPasswordLink");
    const backToLoginLink = document.getElementById("backToLoginLink");
    const responseMessage = document.getElementById("responseMessage");
    const serverBase = typeof server === "string" ? server : "";

    function field(selector) {
        return document.querySelector(selector);
    }

    function removeGroupErrors(formGroup) {
        if (!formGroup) {
            return;
        }
        Array.from(formGroup.children).forEach(function (child) {
            if (child.classList.contains("auth-field-error")) {
                child.remove();
            }
        });
    }

    function showError(selector, message) {
        const element = field(selector);
        const formGroup = element ? element.closest(".form-group") : null;

        if (!formGroup) {
            return;
        }

        removeGroupErrors(formGroup);

        const error = document.createElement("div");
        error.className = "auth-field-error";
        error.textContent = message;
        formGroup.appendChild(error);
    }

    function removeError(selector) {
        const element = field(selector);
        removeGroupErrors(element ? element.closest(".form-group") : null);
    }

    function validateLoginForm() {
        let isValid = true;
        const username = field("#username").value.trim();
        const password = field("#password").value.trim();
        const code = field("#code").value.trim();

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

    function formToJson(form) {
        const formDataObj = {};
        new FormData(form).forEach(function (value, key) {
            formDataObj[key] = value;
        });
        return JSON.stringify(formDataObj);
    }

    function responseJson(response) {
        return response.json().catch(function () {
            return {};
        });
    }

    function setRecoveryMessage(message, type) {
        responseMessage.textContent = message;
        responseMessage.classList.remove("d-none", "alert-success", "alert-danger");
        responseMessage.classList.add("alert", type === "success" ? "alert-success" : "alert-danger");
    }

    function fetchJson(url, options) {
        return fetch(url, options).then(function (response) {
            return responseJson(response).then(function (data) {
                if (!response.ok) {
                    throw new Error(data.message || "An unexpected error occurred.");
                }
                return data;
            });
        });
    }

    function submitLogin(event) {
        event.preventDefault();

        if (!validateLoginForm()) {
            return;
        }

        fetchJson(serverBase + "/user/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json;charset=UTF-8",
                "X-CSRF-TOKEN": csrfToken
            },
            body: formToJson(loginForm)
        })
            .then(function (resp) {
                alert(resp.message);
                window.location.href = resp.role === "ADMIN" ? serverBase + "/admin" : serverBase + "/";
            })
            .catch(function (error) {
                alert(error.message || "An unexpected error occurred.");
            });
    }

    function refreshCaptcha() {
        captchaImage.src = serverBase + "/user/getCode?" + new Date().getMilliseconds();
    }

    loginForm.addEventListener("submit", submitLogin);
    document.querySelector(".login").addEventListener("click", submitLogin);
    captchaImage.addEventListener("click", refreshCaptcha);
    captchaImage.src = serverBase + "/user/getCode";

    forgotPasswordLink.addEventListener("click", function (event) {
        event.preventDefault();
        loginPanel.classList.add("d-none");
        passwordRecoveryPanel.classList.remove("d-none");
    });

    backToLoginLink.addEventListener("click", function (event) {
        event.preventDefault();
        passwordRecoveryPanel.classList.add("d-none");
        loginPanel.classList.remove("d-none");
    });

    passwordRecoveryForm.addEventListener("submit", function (event) {
        event.preventDefault();

        fetchJson(serverBase + "/user/password-recovery", {
            method: "POST",
            headers: {
                "Content-Type": "application/json;charset=UTF-8",
                "X-CSRF-TOKEN": csrfToken
            },
            body: JSON.stringify({ email: passwordRecoveryForm.querySelector("#email").value })
        })
            .then(function (response) {
                setRecoveryMessage(response.message, "success");
            })
            .catch(function (error) {
                setRecoveryMessage(error.message || "An error occurred. Please try again.", "error");
            });
    });
});
