$(document).ready(function () {
    let isUsernameAvailable = false;
    let isEmailAvailable = false;
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

    function checkPasswordComplexity(password) {
        const requirements = [
            { regex: /.{8,}/, text: "8 characters" },
            { regex: /[A-Z]/, text: "one uppercase letter" },
            { regex: /[a-z]/, text: "one lowercase letter" },
            { regex: /[0-9]/, text: "one number" },
            { regex: /[\W_]/, text: "one special character" }
        ];
        const failingRequirements = requirements
            .filter(function (requirement) {
                return !requirement.regex.test(password);
            })
            .map(function (requirement) {
                return requirement.text;
            });

        if (failingRequirements.length > 0) {
            return {
                isValid: false,
                message: "Password must contain at least: " + failingRequirements.join(", ")
            };
        }
        return { isValid: true };
    }

    function validateEmail(email) {
        return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email);
    }

    function debounce(func, wait, immediate) {
        let timeout;
        return function () {
            const context = this;
            const args = arguments;
            clearTimeout(timeout);
            timeout = setTimeout(function () {
                timeout = null;
                if (!immediate) {
                    func.apply(context, args);
                }
            }, wait);
            if (immediate && !timeout) {
                func.apply(context, args);
            }
        };
    }

    function responseJson(response) {
        return response.json().catch(function () {
            return {};
        });
    }

    function fetchJson(url, options, fallback) {
        return fetch(url, options).then(function (response) {
            return responseJson(response).then(function (data) {
                if (!response.ok) {
                    throw new Error(data.message || fallback);
                }
                return data;
            });
        });
    }

    function fetchText(url, options) {
        return fetch(url, options).then(function (response) {
            return response.text().then(function (text) {
                if (!response.ok) {
                    const error = new Error(text || "Request failed.");
                    error.status = response.status;
                    error.responseText = text;
                    throw error;
                }
                return text;
            });
        });
    }

    $("#username").on("input", debounce(function () {
        const username = $(this).val().trim();
        if (username.length < 3) {
            showError("#username", "Username must be at least 3 characters long.");
            isUsernameAvailable = false;
            return;
        }

        fetchJson("/user/check-username?" + new URLSearchParams({ username: username }).toString(), {
            headers: {
                "X-CSRF-TOKEN": csrfToken
            }
        }, "Username is already taken.")
            .then(function () {
                isUsernameAvailable = true;
                removeError("#username");
            })
            .catch(function (error) {
                console.error("Error checking username", error.message);
                showError("#username", error.message || "Username is already taken.");
                isUsernameAvailable = false;
            });
    }, 500));

    $("#email").on("input", debounce(function () {
        const email = $(this).val().trim();
        if (!validateEmail(email)) {
            showError("#email", "Please enter a valid email address.");
            isEmailAvailable = false;
            return;
        }

        fetchJson("/user/check-email?" + new URLSearchParams({ email: email }).toString(), {
            headers: {
                "X-CSRF-TOKEN": csrfToken
            }
        }, "Email is already in use.")
            .then(function () {
                isEmailAvailable = true;
                removeError("#email");
            })
            .catch(function (error) {
                console.error("Error checking email", error.message);
                showError("#email", error.message || "Email is already in use.");
                isEmailAvailable = false;
            });
    }, 500));

    function validateField() {
        let isValid = true;
        const password = $("#password").val();
        const confirmPassword = $("#confirmPass").val();
        const email = $("#email").val().trim();

        if (!$("#fullName").val().trim().length) {
            showError("#fullName", "Full Name cannot be empty.");
            isValid = false;
        } else {
            removeError("#fullName");
        }

        if (!validateEmail(email)) {
            showError("#email", "Please enter a valid email address.");
            isValid = false;
        } else if (!isEmailAvailable) {
            showError("#email", "Email is already in use. Please choose a different one.");
            isValid = false;
        } else {
            removeError("#email");
        }

        if (!$("#username").val().trim().length) {
            showError("#username", "Username cannot be empty.");
            isValid = false;
        } else if (!isUsernameAvailable) {
            showError("#username", "Username is already taken. Please choose a different one.");
            isValid = false;
        } else {
            removeError("#username");
        }

        const passwordComplexityResult = checkPasswordComplexity(password);
        if (!passwordComplexityResult.isValid) {
            showError("#password", passwordComplexityResult.message);
            isValid = false;
        } else {
            removeError("#password");
        }

        if (password !== confirmPassword) {
            showError("#confirmPass", "Passwords do not match.");
            isValid = false;
        } else {
            removeError("#confirmPass");
        }

        return isValid && isUsernameAvailable && isEmailAvailable;
    }

    function submitRegistration(e) {
        e.preventDefault();

        if (!isUsernameAvailable) {
            alert("Please choose a different username. The current one is already taken.");
            return;
        }

        if (!isEmailAvailable) {
            alert("Please choose a different email. The current one is already in use.");
            return;
        }

        if (!validateField()) {
            return;
        }

        const formDataObj = {};
        $.each($("#signupForm").serializeArray(), function (i, field) {
            formDataObj[field.name] = field.value;
        });

        fetchText(server + "/user/save", {
            method: "POST",
            headers: {
                "Content-Type": "application/json;charset=UTF-8",
                "X-CSRF-TOKEN": csrfToken
            },
            body: JSON.stringify(formDataObj)
        })
            .then(function (resp) {
                if (resp === "success") {
                    alert("Registration successful, about to jump to the login page!");
                    window.location.href = server + "/login";
                } else {
                    alert("Sorry, registration failed!");
                }
            })
            .catch(function (error) {
                console.error("Registration error:", error.status, error.responseText || error.message);
                alert("Sorry, there was a problem with your registration. Please try again.");
            });
    }

    $(".register").on("click", submitRegistration);
    $("#signupForm").on("submit", submitRegistration);
});
