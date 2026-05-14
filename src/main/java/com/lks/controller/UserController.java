package com.lks.controller;

import com.lks.bean.User;
import com.lks.dto.AdminUserUpdateRequest;
import com.lks.dto.DeleteAccountRequest;
import com.lks.dto.PasswordRecoveryRequest;
import com.lks.dto.PasswordResetRequest;
import com.lks.dto.UserLoginRequest;
import com.lks.dto.UserRegistrationRequest;
import com.lks.dto.UserResponse;
import com.lks.dto.UserSearchRequest;
import com.lks.exception.RateLimitExceededException;
import com.lks.mapper.UserMapper;
import com.lks.util.ValidateCode;
import com.lks.service.RequestRateLimiter;
import com.lks.service.UserLoginResult;
import com.lks.service.UserService;
import com.lks.service.UserServiceResult;
import com.lks.service.UserServiceStatus;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
	private final UserMapper userMapper;
	private final UserService userService;
	private final RequestRateLimiter requestRateLimiter;

	private static final int MAX_USER_SEARCH_TERM_LENGTH = 50;
	private static final int MAX_LOGIN_ATTEMPTS_PER_WINDOW = 10;
	private static final int MAX_SIGNUP_ATTEMPTS_PER_WINDOW = 5;
	private static final int MAX_PASSWORD_RECOVERY_ATTEMPTS_PER_CLIENT_WINDOW = 5;
	private static final int MAX_CAPTCHA_REQUESTS_PER_WINDOW = 30;
	private static final Duration LOGIN_RATE_LIMIT_WINDOW = Duration.ofMinutes(5);
	private static final Duration SIGNUP_RATE_LIMIT_WINDOW = Duration.ofHours(1);
	private static final Duration PASSWORD_RECOVERY_RATE_LIMIT_WINDOW = Duration.ofHours(1);
	private static final Duration CAPTCHA_RATE_LIMIT_WINDOW = Duration.ofMinutes(10);

	public UserController(UserMapper userMapper, UserService userService, RequestRateLimiter requestRateLimiter) {
		this.userMapper = userMapper;
		this.userService = userService;
		this.requestRateLimiter = requestRateLimiter;
	}

	@PostMapping("/password-recovery")
	public ResponseEntity<?> recoverPassword(@RequestBody PasswordRecoveryRequest request, HttpServletRequest httpRequest) {
		if (request == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "Invalid request body."));
		}
		try {
			validateNoUnsupportedFields(request.getUnsupportedFields());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}

		String email = normalizeEmail(request.getEmail());
		if (email == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
		}

		enforceClientRateLimit(httpRequest, "password-recovery", MAX_PASSWORD_RECOVERY_ATTEMPTS_PER_CLIENT_WINDOW,
				PASSWORD_RECOVERY_RATE_LIMIT_WINDOW, "Too many password recovery requests. Please try again later.");

		UserServiceResult result = userService.recoverPassword(email);
		return serviceResponse(result);
	}

	@PostMapping("/update-password")
	@ResponseBody
	public ResponseEntity<Map<String, String>> updatePassword(@ModelAttribute PasswordResetRequest request) {
		return serviceResponse(userService.updatePassword(request));
	}

	@GetMapping("/all")
	public ResponseEntity<?> getUserList(HttpSession session) {
		if (!isAdmin(session)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required."));
		}
		return ResponseEntity.ok(toUserResponses(userMapper.userList()));
	}

	@GetMapping("/me")
	public ResponseEntity<?> currentUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Login required."));
		}
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Login required."));
		}
		return ResponseEntity.ok(UserResponse.from(loggedInUser));
	}

	@PostMapping("/login")
	public ResponseEntity<Map<String, String>> loginUser(@RequestBody UserLoginRequest request, HttpServletRequest req) {
		HttpSession session = req.getSession();
		String vcode = (String) session.getAttribute("vcode");

		enforceClientRateLimit(req, "login", MAX_LOGIN_ATTEMPTS_PER_WINDOW, LOGIN_RATE_LIMIT_WINDOW,
				"Too many login attempts. Please try again later.");

		UserLoginResult result = userService.loginUser(request, vcode);
		Map<String, String> response = new HashMap<>();
		response.put("message", result.message());
		if (result.status() == UserServiceStatus.OK) {
			User user = result.user();
			session.setAttribute("loggedInUser", user);
			response.put("username", user.getUsername());
			response.put("role", user.getRole());
			response.put("id", user.getId().toString());
		}
		return ResponseEntity.status(toHttpStatus(result.status())).body(response);
	}

	@DeleteMapping("/delete")
	public ResponseEntity<String> deleteUser(@RequestBody DeleteAccountRequest request, HttpSession session) {
		User sessionUser = (User) session.getAttribute("loggedInUser");
		UserServiceResult result = userService.deleteCurrentUser(request, sessionUser);
		if (result.status() == UserServiceStatus.OK) {
			session.invalidate();
		}
		return serviceStringResponse(result);
	}

	@DeleteMapping("/admin/{id}")
	public ResponseEntity<String> deleteUserAsAdmin(@PathVariable Integer id, HttpSession session) {
		if (!isAdmin(session)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required.");
		}
		User adminUser = (User) session.getAttribute("loggedInUser");
		return serviceStringResponse(userService.deleteUserAsAdmin(id, adminUser));
	}

	@PostMapping("/save")
	public ResponseEntity<String> saveUser(@RequestBody UserRegistrationRequest request, HttpServletRequest httpRequest) {
		enforceClientRateLimit(httpRequest, "signup", MAX_SIGNUP_ATTEMPTS_PER_WINDOW, SIGNUP_RATE_LIMIT_WINDOW,
				"Too many signup attempts. Please try again later.");
		return serviceStringResponse(userService.registerUser(request));
	}

	@PostMapping("/admin/create")
	public ResponseEntity<String> createUserAsAdmin(@RequestBody UserRegistrationRequest request, HttpSession session) {
		if (!isAdmin(session)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required.");
		}
		return serviceStringResponse(userService.registerUser(request));
	}

	@PostMapping("/update")
	public ResponseEntity<String> updateUser(AdminUserUpdateRequest request, HttpSession session) {
		if (!isAdmin(session)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required.");
		}
		return serviceStringResponse(userService.updateUserAsAdmin(request));
	}

	@GetMapping("/getCode")
	public void getCode(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		enforceClientRateLimit(req, "captcha", MAX_CAPTCHA_REQUESTS_PER_WINDOW, CAPTCHA_RATE_LIMIT_WINDOW,
				"Too many verification code requests. Please try again later.");
		ValidateCode vCode = new ValidateCode(140, 40, 5, 50);
		HttpSession session = req.getSession();
		session.setAttribute("vcode", vCode.getCode());
		ServletOutputStream sos = resp.getOutputStream();
		vCode.write(sos);
	}

	@GetMapping("/selectByUser")
	public ResponseEntity<?> getUserWhere(UserSearchRequest request, HttpSession session) throws IOException {
		if (!isAdmin(session)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required."));
		}
		User user = new User();
		user.setUsername(validateUserSearchTerm(request.getUsername(), "Username"));
		user.setFullName(validateUserSearchTerm(request.getFullName(), "Full name"));
		return ResponseEntity.ok(toUserResponses(userMapper.getListByUser(user)));
	}
	
	@GetMapping("/countUsers")
    public ResponseEntity<?> getTotalUserCount(HttpSession session) {
		if (!isAdmin(session)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required."));
		}
        long count = userMapper.getTotalUserCount();
        return ResponseEntity.ok(count);
    }

	@GetMapping("/check-username")
	public ResponseEntity<?> checkUsernameAvailability(@RequestParam("username") String username) {

		User existingUser = userMapper.findByUsername(username);
		if (existingUser != null) {

			return ResponseEntity.badRequest().body(new HashMap<String, String>() {

				private static final long serialVersionUID = 1L;

				{
					put("message", "Username is already taken!");
				}
			});
		}
		return ResponseEntity.ok(new HashMap<String, String>() {
			private static final long serialVersionUID = 1L;

			{
				put("message", "Username is available.");
			}
		});
	}

	@GetMapping("/check-email")
	public ResponseEntity<?> checkEmailAvailability(@RequestParam("email") String email) {
		User existingUser = userMapper.findByEmail(email);
		if (existingUser != null) {

			return ResponseEntity.badRequest().body(new HashMap<String, String>() {

				private static final long serialVersionUID = 1L;

				{
					put("message", "Email is already in use!");
				}
			});
		}
		return ResponseEntity.ok(new HashMap<String, String>() {

			private static final long serialVersionUID = 1L;

			{
				put("message", "Email is available.");
			}
		});
	}

	private void enforceClientRateLimit(HttpServletRequest request, String action, int maxRequests, Duration window,
			String message) {
		String key = "user:" + action + ":" + clientAddress(request);
		if (!requestRateLimiter.tryAcquire(key, maxRequests, window)) {
			throw new RateLimitExceededException(message);
		}
	}

	private ResponseEntity<Map<String, String>> serviceResponse(UserServiceResult result) {
		return ResponseEntity.status(toHttpStatus(result.status())).body(Map.of("message", result.message()));
	}

	private ResponseEntity<String> serviceStringResponse(UserServiceResult result) {
		return ResponseEntity.status(toHttpStatus(result.status())).body(result.message());
	}

	private HttpStatus toHttpStatus(UserServiceStatus status) {
		return switch (status) {
			case OK -> HttpStatus.OK;
			case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
			case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
			case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
			case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
		};
	}

	private String clientAddress(HttpServletRequest request) {
		if (request == null || isBlank(request.getRemoteAddr())) {
			return "unknown";
		}
		return request.getRemoteAddr();
	}

	private void validateNoUnsupportedFields(Map<String, Object> unsupportedFields) {
		if (!unsupportedFields.isEmpty()) {
			String fieldName = unsupportedFields.keySet().iterator().next();
			throw new IllegalArgumentException("Unsupported user field: " + fieldName);
		}
	}

	private List<UserResponse> toUserResponses(List<User> users) {
		return users.stream()
				.map(UserResponse::from)
				.toList();
	}

	private String validateUserSearchTerm(String value, String fieldName) {
		return validateUserText(value, fieldName, MAX_USER_SEARCH_TERM_LENGTH, false);
	}

	private String validateUserText(String value, String fieldName, int maxLength, boolean required) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			if (required) {
				throw new IllegalArgumentException(fieldName + " is required.");
			}
			return null;
		}
		if (trimmed.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " is too long.");
		}
		if (containsHtmlBoundary(trimmed) || containsControlCharacter(trimmed)) {
			throw new IllegalArgumentException(fieldName + " contains invalid characters.");
		}
		return trimmed;
	}

	private String trimToNull(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return value.trim();
	}

	private boolean containsHtmlBoundary(String value) {
		return value.indexOf('<') >= 0 || value.indexOf('>') >= 0;
	}

	private boolean containsControlCharacter(String value) {
		for (int i = 0; i < value.length(); i++) {
			if (Character.isISOControl(value.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	private String normalizeEmail(String email) {
		String normalized = trimToNull(email);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.toLowerCase(Locale.ROOT);
		if (!normalized.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
			return null;
		}
		return normalized;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private boolean isAdmin(HttpSession session) {
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		return loggedInUser != null && "ADMIN".equalsIgnoreCase(loggedInUser.getRole());
	}
}
