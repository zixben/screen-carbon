document.addEventListener("DOMContentLoaded", function () {
    let isUsernameAvailable = false;
    let isEmailAvailable = false;
    const signupForm = document.getElementById("signupForm");
    const fullNameInput = document.getElementById("fullName");
    const usernameInput = document.getElementById("username");
    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    const confirmPasswordInput = document.getElementById("confirmPass");
    const csrfInput = document.querySelector('input[name="_csrf"]');
    const csrfToken = csrfInput ? csrfInput.value : "";
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

    function debounce(func, wait) {
        let timeout;
        return function (event) {
            const context = this;
            clearTimeout(timeout);
            timeout = setTimeout(function () {
                func.call(context, event);
            }, wait);
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

    usernameInput.addEventListener("input", debounce(function () {
        const username = usernameInput.value.trim();
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

    emailInput.addEventListener("input", debounce(function () {
        const email = emailInput.value.trim();
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
        const password = passwordInput.value;
        const confirmPassword = confirmPasswordInput.value;
        const email = emailInput.value.trim();

        if (!fullNameInput.value.trim().length) {
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

        if (!usernameInput.value.trim().length) {
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

    function formToJson(form) {
        const formDataObj = {};
        new FormData(form).forEach(function (value, key) {
            formDataObj[key] = value;
        });
        return JSON.stringify(formDataObj);
    }

    function submitRegistration(event) {
        event.preventDefault();

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

        fetchText(serverBase + "/user/save", {
            method: "POST",
            headers: {
                "Content-Type": "application/json;charset=UTF-8",
                "X-CSRF-TOKEN": csrfToken
            },
            body: formToJson(signupForm)
        })
            .then(function (resp) {
                if (resp === "success") {
                    alert("Registration successful, about to jump to the login page!");
                    window.location.href = serverBase + "/login";
                } else {
                    alert("Sorry, registration failed!");
                }
            })
            .catch(function (error) {
                console.error("Registration error:", error.status, error.responseText || error.message);
                alert("Sorry, there was a problem with your registration. Please try again.");
            });
    }

    document.querySelector(".register").addEventListener("click", submitRegistration);
    signupForm.addEventListener("submit", submitRegistration);
});
