		fetchCurrentUser()
			.done(function() {
				let str = "    <li onclick = 'logout()'><a href=\"#\" >Logout</a></li>\n" +
					"     <li><a href=\"/user-ratings\">User</a></li>"
				$(".sub__menu").html(str)
			})
			.fail(function() {
				let str = "      <li><a href=\"/login\">Login</a></li> <li><a href=\"/signup\">Register</a></li>"
				$(".sub__menu").html(str)
			});

		function logout() {
			window.localStorage.removeItem("user");
		}
