package com.lks.controller;

import com.lks.bean.User;
import com.lks.bean.RecoveryToken;
import com.lks.dto.AdminUserUpdateRequest;
import com.lks.dto.DeleteAccountRequest;
import com.lks.dto.PasswordRecoveryRequest;
import com.lks.dto.UserLoginRequest;
import com.lks.dto.UserRegistrationRequest;
import com.lks.dto.UserResponse;
import com.lks.dto.UserSearchRequest;
import com.lks.mapper.UserMapper;
import com.lks.util.ValidateCode;
import com.lks.service.EmailService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

//import javax.servlet.ServletOutputStream;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpSession;

import org.slf4j.Logger; //FK 2023
import org.slf4j.LoggerFactory; //FK 2023

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
	@Autowired
	private UserMapper userMapper;

	@Autowired
	private EmailService emailService;

	private static final Logger log = LoggerFactory.getLogger(UserController.class); // FK 2023
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // Create an instance of the password

	@PostMapping("/password-recovery")
	public ResponseEntity<?> recoverPassword(@RequestBody PasswordRecoveryRequest request) {
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

		log.info("Password recovery requested for email: {}", email);

		// Find the user by email
		User user = userMapper.findByEmail(email);
		if (user != null && !isAccountLocked(user)) {
			try {
				// Generate a secure, time-limited token
				String rawToken = generateSecureToken();
				String tokenHash = passwordEncoder.encode(rawToken);
				Timestamp expiresAt = Timestamp.from(Instant.now().plus(Duration.ofMinutes(30)));

				// Create the recovery link
				// Development Link
				// String recoveryLink = "http://localhost:8081/reset-password?token=" + rawToken;
//				String link = "http://localhost:8081/reset-password?token=" + rawToken;
//				String recoveryLink = "<a href=\"" + link + "\">Reset your password</a>";
				

				// Production Link
				// String recoveryLink =
				// "https://screencarbontest.gla.ac.uk/reset-password?token=" + rawToken;
				String link = "https://screencarbontest.gla.ac.uk/reset-password?token=" + rawToken;
				String recoveryLink = "<a href=\"" + link + "\">Reset your password</a>";

				// Try sending the recovery email
				emailService.sendEmail(email, "Password Recovery",
						"Click the link to reset your password: " + recoveryLink);

				// If email sent successfully, insert the recovery token into the database
				userMapper.insertRecoveryToken(user.getId(), tokenHash, expiresAt);
				log.info("Generated recovery token and sent email for user ID {}.", user.getId());

				// Respond with a success message
				return ResponseEntity
						.ok(Map.of("message", "A recovery link has been sent if the email is registered."));

			} catch (Exception e) {

				// Handle email sending error, don't insert the token if email fails to send
				log.error("Failed to send recovery email to {}: {}", email, e.getMessage());
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(Map.of("message", "Failed to send recovery email. Please try again later."));
			}
		} else {
			log.warn("Password recovery attempt for non-existent or locked email: {}", email);
			return ResponseEntity.ok(Map.of("message", "A recovery link has been sent if the email is registered."));
		}
	}

	private String generateSecureToken() {
		return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
	}

	@PostMapping("/update-password")
	@ResponseBody
	public ResponseEntity<Map<String, String>> updatePassword(@RequestParam("token") String token,
			@RequestParam("newPassword") String newPassword, @RequestParam("confirmPassword") String confirmPassword) throws Exception {

		log.debug("updatePassword method called with token: {}", token);

		// Retrieve all active recovery tokens that are not expired and not used
		List<RecoveryToken> recoveryTokens = userMapper.findActiveRecoveryTokens();

		RecoveryToken validToken = null;

		// Check if any stored token matches the raw token
		for (RecoveryToken rt : recoveryTokens) {
			if (passwordEncoder.matches(token, rt.getTokenHash())) {
				validToken = rt;
				break;
			}
		}
		// Validate the token: check if it exists, is not expired, and has not been used
		if (validToken == null || validToken.getExpiresAt().before(new Date()) || validToken.isUsed()) {
			log.warn("Invalid, expired, or already used token.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid or expired token."));
		}

		// Fetch the associated user using the valid token's user ID
		User user = userMapper.findById(validToken.getUserId());
		if (user == null) {
			log.warn("Recovery token references a missing user.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid or expired token."));
		}

		// Check if the new password matches the confirm password
		if (!newPassword.equals(confirmPassword)) {
			log.warn("Passwords do not match.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Passwords do not match."));
		}

		// Check if the new password is strong enough
		if (!isPasswordStrong(newPassword)) {
			log.warn("Password does not meet the required strength.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("message", "Password does not meet the required strength."));
		}

		// Check if the new password matches the old password to avoid reuse
		if (passwordEncoder.matches(newPassword, user.getPassword())) {
			log.warn("New password must not be the same as the previous password.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("message", "New password must not be the same as the previous password."));
		}

		// Update the user's password
		user.setPassword(passwordEncoder.encode(newPassword));
		if (userMapper.updateUser(user) > 0) {

			// Clear all unnecessary recovery data related to this user
			userMapper.clearAllRecoveryTokensForUser(user.getId());

			// Send a notification email after password reset
			emailService.sendEmail(user.getEmail(), "Password Changed",
					"Your password has been changed. If you did not initiate this change, please contact support immediately.");

			log.info("Password successfully updated for user ID: {}", user.getId());
			return ResponseEntity.ok(Map.of("message", "Password successfully updated. You can now log in."));
		} else {

			log.error("Failed to update password for user ID: {}", user.getId());

			// Mark the token as used after successful password update
			userMapper.markTokenAsUsed(validToken.getId());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Failed to update password. Please try again."));
		}
	}

	private boolean isPasswordStrong(String password) {

		// Implement password strength validation logic (length, special characters
		return password != null && password.length() >= 8 && password.matches(".*[!@#$%^&*()].*");
	}

	private boolean isAccountLocked(User user) {
		// Convert Timestamp to Instant for comparison
		return user.getLockTime() != null && user.getLockTime().toInstant().isAfter(Instant.now());
	}

	@GetMapping("/all")
	public ResponseEntity<?> getUserList(HttpSession session) {
		if (!isAdmin(session)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required."));
		}
		return ResponseEntity.ok(toUserResponses(userMapper.userList()));
	}

	@PostMapping("/login")
	public ResponseEntity<Map<String, String>> loginUser(@RequestBody UserLoginRequest request, HttpServletRequest req) {
		HttpSession session = req.getSession();
		String vcode = (String) session.getAttribute("vcode");

		Map<String, String> response = new HashMap<>();
		if (request == null) {
			response.put("message", "Invalid request body.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		try {
			validateNoUnsupportedFields(request.getUnsupportedFields());
		} catch (IllegalArgumentException e) {
			response.put("message", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// Verify the code (case-insensitive)
		if (vcode == null || isBlank(request.getCode()) || !vcode.equalsIgnoreCase(request.getCode().trim())) {
			response.put("message", "Verification code is incorrect.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		if (isBlank(request.getUsername()) || isBlank(request.getPassword())) {
			response.put("message", "Invalid username or password.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}

		// Fetch the user by username
		User userFromDb = userMapper.findByUsername(request.getUsername().trim());

		if (userFromDb != null && passwordEncoder.matches(request.getPassword(), userFromDb.getPassword())) {
			// Valid credentials, set user in session
			session.setAttribute("loggedInUser", userFromDb);

			//System.out.println("Session loggedInUser: " + session.getAttribute("loggedInUser"));
			response.put("username", userFromDb.getUsername());
			response.put("role", userFromDb.getRole());
			response.put("id", userFromDb.getId().toString());

			// Return a success message
			response.put("message", "Login successful.");
			return ResponseEntity.ok(response);
		}

		// Return unauthorized if the credentials are invalid
		response.put("message", "Invalid username or password.");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	}

	@DeleteMapping("/delete")
	public ResponseEntity<String> deleteUser(@RequestBody DeleteAccountRequest request, HttpSession session) {
		User sessionUser = (User) session.getAttribute("loggedInUser");
		if (request == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request body.");
		}

		try {
			validateNoUnsupportedFields(request.getUnsupportedFields());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}

		// Check if the user is logged in and the request data is valid
		if (sessionUser == null || request == null || isBlank(request.getPassword())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user data or no user logged in.");
		}

		if (!isBlank(request.getUsername()) && !sessionUser.getUsername().equals(request.getUsername().trim())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: Username mismatch.");
		}

		// Authenticate the user's password before deletion
		if (!passwordEncoder.matches(request.getPassword(), sessionUser.getPassword())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: Incorrect password.");
		}

		// Proceed with deletion
		if (userMapper.deleteUser(sessionUser.getId()) > 0) {
			session.invalidate(); // Invalidate the session after deletion
			return ResponseEntity.status(HttpStatus.OK).body("User deleted successfully.");
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Deletion failed: User not found.");
		}
	}

	@PostMapping("/save")
	public ResponseEntity<String> saveUser(@RequestBody UserRegistrationRequest request) {
		if (request == null) {
			return ResponseEntity.badRequest().body("Invalid request body.");
		}
		try {
			validateNoUnsupportedFields(request.getUnsupportedFields());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}

		String username = trimRequired(request.getUsername(), "Username");
		String fullName = trimRequired(request.getFullName(), "Full name");
		String email = normalizeEmail(request.getEmail());
		String description = trimToNull(request.getDescription());
		if (email == null) {
			return ResponseEntity.badRequest().body("Email is required.");
		}
		if (username.length() > 24 || fullName.length() > 200 || email.length() > 50
				|| (description != null && description.length() > 350)) {
			return ResponseEntity.badRequest().body("Registration data is too long.");
		}
		if (!isPasswordStrong(request.getPassword())) {
			return ResponseEntity.badRequest().body("Password does not meet the required strength.");
		}
		if (!isBlank(request.getConfirmPass()) && !request.getPassword().equals(request.getConfirmPass())) {
			return ResponseEntity.badRequest().body("Passwords do not match.");
		}

		log.info("Received registration for username: {}", username);

		try {
			// Check if the username already exists
			User existingUserByUsername = userMapper.findByUsername(username);
			if (existingUserByUsername != null) {
				log.warn("Username already taken: {}", username);
				return ResponseEntity.badRequest().body("Username is already taken.");
			}

			// Check if the email already exists
			User existingUserByEmail = userMapper.findByEmail(email);
			if (existingUserByEmail != null) {
				log.warn("Email already in use: {}", email);
				return ResponseEntity.badRequest().body("Email is already in use.");
			}

			User user = new User();
			user.setFullName(fullName);
			user.setUsername(username);
			user.setEmail(email);
			user.setDescription(description);

			// Hash the password before saving
			String hashedPassword = passwordEncoder.encode(request.getPassword());
			user.setPassword(hashedPassword);

			// Save the user to the database
			Integer result = userMapper.saveUser(user);
			if (result > 0) {
				log.info("User successfully saved: {}", username);
				return ResponseEntity.ok("success");
			} else {
				log.error("Failed to save user: {}", username);
				return ResponseEntity.badRequest().body("Registration failure");
			}
		} catch (Exception e) {
			// Log the exception
			log.error("Error saving user: ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving user");
		}
	}

	@PostMapping("/update")
	public ResponseEntity<String> updateUser(AdminUserUpdateRequest request, HttpSession session) {
		if (!isAdmin(session)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required.");
		}

		if (request == null) {
			return ResponseEntity.badRequest().body("Invalid request body.");
		}
		if (request.getId() == null) {
			return ResponseEntity.badRequest().body("User id is required.");
		}

		User user = userMapper.findById(request.getId());
		if (user == null) {
			return ResponseEntity.badRequest().body("User not found.");
		}

		String username = trimRequired(request.getUsername(), "Username");
		if (username.length() > 24) {
			return ResponseEntity.badRequest().body("Username is too long.");
		}

		User existingUserByUsername = userMapper.findByUsername(username);
		if (existingUserByUsername != null && !existingUserByUsername.getId().equals(user.getId())) {
			return ResponseEntity.badRequest().body("Username is already taken.");
		}

		user.setUsername(username);
		user.setDescription(trimToNull(request.getDescription()));

		if (!isBlank(request.getPassword())) {
			if (!isPasswordStrong(request.getPassword())) {
				return ResponseEntity.badRequest().body("Password does not meet the required strength.");
			}
			user.setPassword(passwordEncoder.encode(request.getPassword()));
		}

		if (userMapper.updateUser(user) > 0) {
			return ResponseEntity.ok("success");
		}
		return ResponseEntity.badRequest().body("fail");
	}

	/**
	 * @Author
	 * @Description
	 * @Date
	 * @param req
	 * @param resp
	 * @return void
	 */
	@GetMapping("/getCode")
	public void getCode(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		ValidateCode vCode = new ValidateCode(140, 40, 5, 50);
		HttpSession session = req.getSession();
		session.setAttribute("vcode", vCode.getCode());
		ServletOutputStream sos = resp.getOutputStream();
		vCode.write(sos);
	}

	/**
	 * @Author
	 * @Description
	 * @Date
	 * @return void
	 */
	@GetMapping("/selectByUser")
	public ResponseEntity<?> getUserWhere(UserSearchRequest request, HttpSession session) throws IOException {
		if (!isAdmin(session)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access required."));
		}
		User user = new User();
		user.setUsername(trimToNull(request.getUsername()));
		user.setFullName(trimToNull(request.getFullName()));
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

		// Check if the username exists
		User existingUser = userMapper.findByUsername(username);
		if (existingUser != null) {

			// Username already exists
			return ResponseEntity.badRequest().body(new HashMap<String, String>() {

				private static final long serialVersionUID = 1L;

				{
					put("message", "Username is already taken!");
				}
			});
		}
		// Username is available
		return ResponseEntity.ok(new HashMap<String, String>() {
			/**
			* 
			*/
			private static final long serialVersionUID = 1L;

			{
				put("message", "Username is available.");
			}
		});
	}

	@GetMapping("/check-email")
	public ResponseEntity<?> checkEmailAvailability(@RequestParam("email") String email) {
		// Check if the email exists
		User existingUser = userMapper.findByEmail(email);
		if (existingUser != null) {

			// Email already exists
			return ResponseEntity.badRequest().body(new HashMap<String, String>() {

				private static final long serialVersionUID = 1L;

				{
					put("message", "Email is already in use!");
				}
			});
		}
		// Email is available
		return ResponseEntity.ok(new HashMap<String, String>() {

			private static final long serialVersionUID = 1L;

			{
				put("message", "Email is available.");
			}
		});
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

	private String trimRequired(String value, String fieldName) {
		String trimmed = trimToNull(value);
		if (trimmed == null) {
			throw new IllegalArgumentException(fieldName + " is required.");
		}
		return trimmed;
	}

	private String trimToNull(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return value.trim();
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
