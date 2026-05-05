package com.lks.controller;

import com.lks.bean.User;
import com.lks.bean.RecoveryToken;
import com.lks.mapper.UserMapper;
import com.lks.util.ValidateCode;
import com.lks.service.EmailService;

import com.mysql.cj.util.StringUtils;
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
	public ResponseEntity<?> recoverPassword(@RequestBody User request, HttpServletRequest httpRequest) {
		String email = request.getEmail().toLowerCase();
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
		// Fetch the associated user using the valid token's user ID
		User user = userMapper.findById(validToken.getUserId());

		// Validate the token: check if it exists, is not expired, and has not been used
		if (validToken == null || validToken.getExpiresAt().before(new Date()) || validToken.isUsed()) {
			log.warn("Invalid, expired, or already used token.");
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
		return password.length() >= 8 && password.matches(".*[!@#$%^&*()].*");
	}

	private boolean isAccountLocked(User user) {
		// Convert Timestamp to Instant for comparison
		return user.getLockTime() != null && user.getLockTime().toInstant().isAfter(Instant.now());
	}

	@RequestMapping("/all")
	public List<User> getUserList() {
		List<User> users = userMapper.userList();
		return users;
	}

	@RequestMapping("/login")
	public ResponseEntity<Map<String, String>> loginUser(@RequestBody User user, HttpServletRequest req) {
		HttpSession session = req.getSession();
		String vcode = (String) session.getAttribute("vcode");

		Map<String, String> response = new HashMap<>();

		// Verify the code (case-insensitive)
		if (vcode == null || !vcode.equalsIgnoreCase(user.getCode())) {
			response.put("message", "Verification code is incorrect.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// Fetch the user by username
		User userFromDb = userMapper.findByUsername(user.getUsername());

		if (userFromDb != null && passwordEncoder.matches(user.getPassword(), userFromDb.getPassword())) {
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
	public ResponseEntity<String> deleteUser(@RequestBody User user, HttpSession session) {
		User sessionUser = (User) session.getAttribute("loggedInUser");

		// Check if the user is logged in and the request data is valid
		if (sessionUser == null || user == null || StringUtils.isEmptyOrWhitespaceOnly(user.getUsername())
				|| StringUtils.isEmptyOrWhitespaceOnly(user.getPassword())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user data or no user logged in.");
		}

		// Check if the username of the logged-in user matches the username of the user
		// to be deleted
		if (!sessionUser.getUsername().equals(user.getUsername())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: Username mismatch.");
		}

		// Authenticate the user's password before deletion
		if (!passwordEncoder.matches(user.getPassword(), sessionUser.getPassword())) {
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
	public ResponseEntity<String> saveUser(@RequestBody User user) {
		// Debugging: Log the received user object
		log.info("Received user to save: {}", user);

		try {
			// Check if the username already exists
			User existingUserByUsername = userMapper.findByUsername(user.getUsername());
			if (existingUserByUsername != null) {
				log.warn("Username already taken: {}", user.getUsername());
				return ResponseEntity.badRequest().body("Username is already taken.");
			}

			// Check if the email already exists
			User existingUserByEmail = userMapper.findByEmail(user.getEmail());
			if (existingUserByEmail != null) {
				log.warn("Email already in use: {}", user.getEmail());
				return ResponseEntity.badRequest().body("Email is already in use.");
			}

			// Hash the password before saving
			String hashedPassword = passwordEncoder.encode(user.getPassword());
			user.setPassword(hashedPassword);

			// Save the user to the database
			Integer result = userMapper.saveUser(user);
			if (result > 0) {
				log.info("User successfully saved: {}", user.getUsername());
				return ResponseEntity.ok("success");
			} else {
				log.error("Failed to save user: {}", user.getUsername());
				return ResponseEntity.badRequest().body("Registration failure");
			}
		} catch (Exception e) {
			// Log the exception
			log.error("Error saving user: ", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving user");
		}
	}

	@RequestMapping("/update")
	public String updateUser(User user) {
		// System.out.println(user);
		if (userMapper.updateUser(user) > 0) {
			return "success";
		}
		return "fail";
	}

	/**
	 * @Author
	 * @Description
	 * @Date
	 * @param req
	 * @param resp
	 * @return void
	 */
	@RequestMapping("/getCode")
	public void getCode(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		ValidateCode vCode = new ValidateCode(140, 40, 5, 50);
		HttpSession session = req.getSession();
		session.setAttribute("vcode", vCode.getCode());
		log.info("Verification Code:" + vCode.getCode());
		ServletOutputStream sos = resp.getOutputStream();
		vCode.write(sos);
	}

	/**
	 * @Author
	 * @Description
	 * @Date
	 * @return void
	 */
	@RequestMapping("/selectByUser")
	public List<User> getUserWhere(User user) throws IOException {
		List<User> listByUser = userMapper.getListByUser(user);
		return listByUser;
	}
	
	@GetMapping("/countUsers")
    public ResponseEntity<Long> getTotalUserCount() {
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
}
