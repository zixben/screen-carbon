document.addEventListener("DOMContentLoaded", function () {
	const form = document.getElementById("resetPasswordForm");

	if (!form) {
		return;
	}

	const responseMessage = document.getElementById("responseMessage");
	const csrfInput = form.querySelector('input[name="_csrf"]');
	const serverBase = typeof server === "string" ? server : "";

	function setMessage(message, type) {
		responseMessage.textContent = message;
		responseMessage.classList.remove("alert-success", "alert-danger");
		responseMessage.classList.add("alert", type === "success" ? "alert-success" : "alert-danger");
	}

	function responseJson(response) {
		return response.json().catch(function () {
			return {};
		});
	}

	form.addEventListener("submit", function (event) {
		event.preventDefault();

		const token = document.getElementById("token").value;
		const newPassword = document.getElementById("newPassword").value;
		const confirmPassword = document.getElementById("confirmPassword").value;

		if (!token) {
			setMessage("Missing token. Please try again.", "error");
			return;
		}

		const headers = {
			"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
		};
		if (csrfInput && csrfInput.value) {
			headers["X-CSRF-TOKEN"] = csrfInput.value;
		}

		const body = new URLSearchParams({
			token: token,
			newPassword: newPassword,
			confirmPassword: confirmPassword
		});

		fetch(serverBase + "/user/update-password", {
			method: "POST",
			headers: headers,
			body: body.toString()
		})
			.then(function (response) {
				return responseJson(response).then(function (data) {
					if (!response.ok) {
						throw new Error(data.message || "An error occurred. Please try again.");
					}
					return data;
				});
			})
			.then(function (data) {
				setMessage(data.message || "Password updated successfully.", "success");
				setTimeout(function () {
					window.location.href = serverBase + "/login";
				}, 2000);
			})
			.catch(function (error) {
				setMessage(error.message || "An error occurred. Please try again.", "error");
			});
	});
});
