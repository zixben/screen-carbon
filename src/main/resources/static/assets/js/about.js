		let user = JSON.parse(window.localStorage.getItem("user"))
		if (user != null) {
			let str = "    <li onclick = 'logout()'><a href=\"#\" >Logout</a></li>\n" +
				"     <li><a href=\"User2.html\">User</a></li>"
			$(".sub__menu").html(str)
		} else {
			let str = "      <li><a href=\"login.html\">Login</a></li> <li><a href=\"signup.html\">Register</a></li>"
			$(".sub__menu").html(str)
		}

		function logout() {
			window.localStorage.clear();
		}
		
	