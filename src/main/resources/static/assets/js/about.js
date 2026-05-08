		fetchCurrentUser()
			.done(function() {
				const $menu = $(".sub__menu").empty();
				$menu.append($("<li>").on("click", logout).append($("<a>").attr("href", "#").text("Logout")));
				$menu.append($("<li>").append($("<a>").attr("href", "/user-ratings").text("User")));
			})
			.fail(function() {
				const $menu = $(".sub__menu").empty();
				$menu.append($("<li>").append($("<a>").attr("href", "/login").text("Login")));
				$menu.append($("<li>").append($("<a>").attr("href", "/signup").text("Register")));
			});

		function logout() {
			window.location.href = server + "/logout";
		}
