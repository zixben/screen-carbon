package com.lks.controller;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.lks.bean.RecoveryToken;
import com.lks.bean.User;
import com.lks.mapper.UserMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class WebController {
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public WebController(UserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/")
	public String index(Model model, HttpSession session) {

		model.addAttribute("title", "Screen Carbon Test");
		model.addAttribute("styles", "/assets/css/index.css");
		model.addAttribute("scripts", "/assets/js/index.js");

		return "index";
	}
	
	@GetMapping("/search-results")
	public String searchPage(Model model) {
		model.addAttribute("title", "Search Results");
		model.addAttribute("styles", "/assets/css/search.css");
		model.addAttribute("scripts", "/assets/js/search.js");
		return "search-page";
	}

	@GetMapping("/movies")
	public String movies(Model model, HttpSession session,
			@RequestParam(value = "toggle", required = false) Boolean toggle) {

		if (toggle != null) {
			session.setAttribute("toggleState", toggle);
		} else if (session.getAttribute("toggleState") == null) {
			session.setAttribute("toggleState", false);
		}

		Boolean toggleState = (Boolean) session.getAttribute("toggleState");

		model.addAttribute("title", "Movies");
		model.addAttribute("activePage", "movies");
		model.addAttribute("styles", "/assets/css/movies.css");

		if (toggleState) {

			model.addAttribute("scripts", "/assets/js/moviesClimateRated.js");
		} else {

			model.addAttribute("scripts", "/assets/js/moviesTMDB.js");
		}

		return "movies";
	}

	@GetMapping("/movie")
	public String movie(Model model) {
		model.addAttribute("title", "Movie");
		model.addAttribute("activePage", "movies");
		model.addAttribute("styles", "/assets/css/movie.css");
		model.addAttribute("scripts", "/assets/js/movie.js");
		return "movie";
	}

	@GetMapping("/tv-shows")
	public String tvShows(Model model, HttpSession session,
			@RequestParam(value = "toggle", required = false) Boolean toggle) {

		if (toggle != null) {
			session.setAttribute("toggleState", toggle);
		} else if (session.getAttribute("toggleState") == null) {
			session.setAttribute("toggleState", false);
		}

		Boolean toggleState = (Boolean) session.getAttribute("toggleState");

		model.addAttribute("title", "TV Shows");
		model.addAttribute("activePage", "tv-shows");
		model.addAttribute("styles", "/assets/css/movies.css");

		if (toggleState) {
			model.addAttribute("scripts", "/assets/js/tvClimateRated.js");
		} else {

			model.addAttribute("scripts", "/assets/js/tvTMDB.js");
		}

		return "tv-shows";
	}

	@GetMapping("/tv")
	public String tv(Model model) {
		model.addAttribute("title", "TV");
		model.addAttribute("activePage", "tv-shows");
		model.addAttribute("styles", "/assets/css/movie.css");
		model.addAttribute("scripts", "/assets/js/tv.js");
		return "tv";
	}

	@GetMapping("/details")
	public String details(Model model) {
		model.addAttribute("title", "Details");
		model.addAttribute("styles", "/assets/css/CelebrityDetail.css");
		model.addAttribute("scripts", "/assets/js/CelebrityDetail.js");
		return "CelebrityDetail";
	}

	@GetMapping("/signup")
	public String signup(Model model, HttpServletRequest request) {

		model.addAttribute("title", "Register");

		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		model.addAttribute("csrfToken", csrfToken);

		return "signup";
	}

	@GetMapping("/login")
	public String login(Model model, HttpServletRequest request) {

		model.addAttribute("title", "Login");

		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		model.addAttribute("csrfToken", csrfToken);

		return "login";
	}
	
	@GetMapping("/privacy-notice")
	public String privacyNotice(Model model) {

		model.addAttribute("title", "Privacy Notice");

		return "privacy-notice";
	}

	@GetMapping("/admin")
	public String adminPage(HttpSession session, Model model) {
	    User loggedInUser = (User) session.getAttribute("loggedInUser");

	    if (loggedInUser == null || !"ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
	        return "redirect:/login";
	    }

	    model.addAttribute("title", "Admin");
	    return "main";
	}
	
	@GetMapping("/1")
    public String ratingDashboard(HttpSession session) {
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null || !"ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
			
			return "redirect:/";
		}
		return "1";
    }
	@GetMapping("/2-2")
    public String userManagement(HttpSession session, Model model, HttpServletRequest request) {
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null || !"ADMIN".equalsIgnoreCase(loggedInUser.getRole())) {
			
			return "redirect:/";
		}

		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		model.addAttribute("csrfToken", csrfToken);

		return "2-2";
    }

	@GetMapping("/rate")
	public String rate(HttpSession session, Model model, HttpServletRequest request) {

		model.addAttribute("title", "Rating");
		model.addAttribute("styles", "/assets/css/rate.css");
		model.addAttribute("scripts", "/assets/js/rate.js");

		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		model.addAttribute("csrfToken", csrfToken);

		return "rate";
	}

	@GetMapping("/finish-rating")
	public String finishRating(HttpSession session, Model model) {

		model.addAttribute("title", "Finish Rating");
		model.addAttribute("styles", "/assets/css/finishRating.css");
		model.addAttribute("scripts", "/assets/js/finish-rating.js");

		return "finish-rating";
	}

	@GetMapping("/user-settings")
	public String user1(HttpSession session, Model model, HttpServletRequest request) {

		if (session.getAttribute("loggedInUser") == null) {

			return "redirect:/login";
		}

		model.addAttribute("title", "User Settings");
		model.addAttribute("styles0", "/assets/css/docs.css");
		model.addAttribute("styles", "/assets/css/User1.css");
		model.addAttribute("scripts", "/assets/js/User1.js?v=delete-confirmation-2");

		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		model.addAttribute("csrfToken", csrfToken);

		return "User1";
	}

	@GetMapping("/user-ratings")
	public String user2(HttpSession session, Model model, HttpServletRequest request) {

		if (session.getAttribute("loggedInUser") == null) {
			return "redirect:/login";
		}

		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		model.addAttribute("csrfToken", csrfToken);

		model.addAttribute("title", "User ratings");
		model.addAttribute("styles0", "/assets/css/docs.css");
		model.addAttribute("styles", "/assets/css/User2.css");
		model.addAttribute("scripts", "/assets/js/User2.js");

		return "User2";
	}

	@GetMapping("/about")
	public String about(Model model) {

		model.addAttribute("title", "About Us");
		model.addAttribute("activePage", "about");
		model.addAttribute("styles", "/assets/css/about.css");
		model.addAttribute("scripts", "/assets/js/about.js");

		return "about";
	}

	@GetMapping("/reset-password")
	public String showResetPasswordForm(@RequestParam("token") String token, Model model, HttpServletRequest request) {

		List<RecoveryToken> recoveryTokens = userMapper.findActiveRecoveryTokens();

		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		model.addAttribute("csrfToken", csrfToken);

		RecoveryToken validToken = null;

		for (RecoveryToken rt : recoveryTokens) {
			if (passwordEncoder.matches(token, rt.getTokenHash())) {
				validToken = rt;
				break;
			}
		}

		if (validToken == null || validToken.getExpiresAt().before(new java.util.Date()) || validToken.isUsed()) {
			model.addAttribute("message", "Invalid or expired token.");
			return "login";
		}

		model.addAttribute("token", token);
		model.addAttribute("title", "Reset Password");
		model.addAttribute("scripts", "/assets/js/reset-password.js");

		return "reset-password";
	}
	

	@GetMapping("/logout")
	public String logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return "redirect:/";
	}
}
