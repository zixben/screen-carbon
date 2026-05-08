package com.lks.service;

import com.lks.bean.RecoveryToken;
import com.lks.bean.User;
import com.lks.dto.DeleteAccountRequest;
import com.lks.dto.PasswordResetRequest;
import com.lks.dto.UserLoginRequest;
import com.lks.dto.UserRegistrationRequest;
import com.lks.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {
	private static final String GENERIC_RECOVERY_MESSAGE =
			"A recovery link has been sent if the email is registered.";
	private static final int MAX_USERNAME_LENGTH = 24;
	private static final int MAX_FULL_NAME_LENGTH = 200;
	private static final int MAX_EMAIL_LENGTH = 50;
	private static final int MAX_DESCRIPTION_LENGTH = 350;
	private static final int MAX_RECOVERY_ATTEMPTS_PER_WINDOW = 3;
	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserMapper userMapper;
	private final EmailService emailService;
	private final PasswordEncoder passwordEncoder;
	private final String appBaseUrl;
	private final boolean emailEnabled;
	private final boolean logRecoveryLink;

	public UserService(UserMapper userMapper, EmailService emailService, PasswordEncoder passwordEncoder,
			@Value("${app.base-url:http://localhost:8081}") String appBaseUrl,
			@Value("${app.email.enabled:true}") boolean emailEnabled,
			@Value("${app.password-recovery.log-link:false}") boolean logRecoveryLink) {
		this.userMapper = userMapper;
		this.emailService = emailService;
		this.passwordEncoder = passwordEncoder;
		this.appBaseUrl = appBaseUrl;
		this.emailEnabled = emailEnabled;
		this.logRecoveryLink = logRecoveryLink;
	}

	public UserServiceResult registerUser(UserRegistrationRequest request) {
		if (request == null) {
			return UserServiceResult.badRequest("Invalid request body.");
		}
		try {
			validateNoUnsupportedFields(request.getUnsupportedFields());
			String username = validateUserText(request.getUsername(), "Username", MAX_USERNAME_LENGTH, true);
			String fullName = validateUserText(request.getFullName(), "Full name", MAX_FULL_NAME_LENGTH, true);
			String email = normalizeEmail(request.getEmail());
			String description = validateUserText(request.getDescription(), "Description", MAX_DESCRIPTION_LENGTH, false);
			if (email == null) {
				return UserServiceResult.badRequest("Email is required.");
			}
			if (email.length() > MAX_EMAIL_LENGTH) {
				return UserServiceResult.badRequest("Registration data is too long.");
			}
			if (!isPasswordStrong(request.getPassword())) {
				return UserServiceResult.badRequest("Password does not meet the required strength.");
			}
			if (!isBlank(request.getConfirmPass()) && !request.getPassword().equals(request.getConfirmPass())) {
				return UserServiceResult.badRequest("Passwords do not match.");
			}

			log.info("Received registration for username: {}", username);

			User existingUserByUsername = userMapper.findByUsername(username);
			if (existingUserByUsername != null) {
				log.warn("Username already taken: {}", username);
				return UserServiceResult.badRequest("Username is already taken.");
			}

			User existingUserByEmail = userMapper.findByEmail(email);
			if (existingUserByEmail != null) {
				log.warn("Email already in use: {}", email);
				return UserServiceResult.badRequest("Email is already in use.");
			}

			User user = new User();
			user.setFullName(fullName);
			user.setUsername(username);
			user.setEmail(email);
			user.setDescription(description);
			user.setPassword(passwordEncoder.encode(request.getPassword()));

			Integer result = userMapper.saveUser(user);
			if (result > 0) {
				log.info("User successfully saved: {}", username);
				return UserServiceResult.ok("success");
			}
			log.error("Failed to save user: {}", username);
			return UserServiceResult.badRequest("Registration failure");
		} catch (IllegalArgumentException e) {
			return UserServiceResult.badRequest(e.getMessage());
		} catch (Exception e) {
			log.error("Error saving user: ", e);
			return UserServiceResult.internalError("Error saving user");
		}
	}

	public UserLoginResult loginUser(UserLoginRequest request, String verificationCode) {
		if (request == null) {
			return UserLoginResult.badRequest("Invalid request body.");
		}
		try {
			validateNoUnsupportedFields(request.getUnsupportedFields());
		} catch (IllegalArgumentException e) {
			return UserLoginResult.badRequest(e.getMessage());
		}

		if (verificationCode == null || isBlank(request.getCode())
				|| !verificationCode.equalsIgnoreCase(request.getCode().trim())) {
			return UserLoginResult.badRequest("Verification code is incorrect.");
		}
		if (isBlank(request.getUsername()) || isBlank(request.getPassword())) {
			return UserLoginResult.unauthorized("Invalid username or password.");
		}

		User userFromDb = userMapper.findByUsername(request.getUsername().trim());
		if (userFromDb != null && passwordEncoder.matches(request.getPassword(), userFromDb.getPassword())) {
			return UserLoginResult.ok(userFromDb);
		}

		return UserLoginResult.unauthorized("Invalid username or password.");
	}

	public UserServiceResult deleteCurrentUser(DeleteAccountRequest request, User sessionUser) {
		if (request == null) {
			return UserServiceResult.badRequest("Invalid request body.");
		}
		try {
			validateNoUnsupportedFields(request.getUnsupportedFields());
		} catch (IllegalArgumentException e) {
			return UserServiceResult.badRequest(e.getMessage());
		}

		if (sessionUser == null || isBlank(request.getPassword())) {
			return UserServiceResult.badRequest("Invalid user data or no user logged in.");
		}

		if (!isBlank(request.getUsername())
				&& (isBlank(sessionUser.getUsername()) || !sessionUser.getUsername().equals(request.getUsername().trim()))) {
			return UserServiceResult.unauthorized("Authentication failed: Username mismatch.");
		}

		if (isBlank(sessionUser.getPassword())) {
			return UserServiceResult.unauthorized("Authentication failed: Incorrect password.");
		}
		if (!passwordEncoder.matches(request.getPassword(), sessionUser.getPassword())) {
			return UserServiceResult.unauthorized("Authentication failed: Incorrect password.");
		}

		if (userMapper.deleteUser(sessionUser.getId()) > 0) {
			return UserServiceResult.ok("User deleted successfully.");
		}
		return UserServiceResult.badRequest("Deletion failed: User not found.");
	}

	public UserServiceResult recoverPassword(String submittedEmail) {
		String email = normalizeEmail(submittedEmail);
		if (email == null) {
			return UserServiceResult.badRequest("Email is required.");
		}

		log.info("Password recovery requested.");

		User user = userMapper.findByEmail(email);
		if (user == null || isAccountLocked(user)) {
			log.warn("Password recovery attempt for non-existent or locked account.");
			return UserServiceResult.ok(GENERIC_RECOVERY_MESSAGE);
		}

		if (userMapper.countRecentRecoveryAttempts(user.getId()) >= MAX_RECOVERY_ATTEMPTS_PER_WINDOW) {
			log.warn("Password recovery request throttled for user ID {}.", user.getId());
			return UserServiceResult.ok(GENERIC_RECOVERY_MESSAGE);
		}

		try {
			String rawToken = generateSecureToken();
			String tokenHash = passwordEncoder.encode(rawToken);
			Timestamp expiresAt = Timestamp.from(Instant.now().plus(Duration.ofMinutes(30)));
			String link = buildRecoveryLink(rawToken);
			String recoveryLink = "<a href=\"" + link + "\">Reset your password</a>";

			if (emailEnabled) {
				emailService.sendEmail(email, "Password Recovery",
						"Click the link to reset your password: " + recoveryLink);
			} else if (logRecoveryLink) {
				log.warn("Password recovery email is disabled. Local recovery link for user ID {}: {}", user.getId(),
						link);
			} else {
				log.error("Password recovery email is disabled and recovery link logging is off.");
				return UserServiceResult.serviceUnavailable("Password recovery email is not configured.");
			}

			userMapper.insertRecoveryToken(user.getId(), tokenHash, expiresAt);
			log.info("Generated recovery token and sent email for user ID {}.", user.getId());
			return UserServiceResult.ok(GENERIC_RECOVERY_MESSAGE);
		} catch (Exception e) {
			log.error("Failed to send recovery email for user ID {}: {}", user.getId(), e.getMessage());
			return UserServiceResult.internalError("Failed to send recovery email. Please try again later.");
		}
	}

	public UserServiceResult updatePassword(PasswordResetRequest request) {
		if (request == null) {
			return UserServiceResult.badRequest("Invalid request body.");
		}

		String token = trimToNull(request.getToken());
		String newPassword = request.getNewPassword();
		String confirmPassword = request.getConfirmPassword();
		if (!isValidRecoveryToken(token)) {
			log.warn("Password reset rejected because the recovery token was missing or malformed.");
			return invalidRecoveryTokenResult();
		}
		if (newPassword == null || confirmPassword == null) {
			return UserServiceResult.badRequest("Password is required.");
		}

		RecoveryToken validToken = findMatchingActiveRecoveryToken(token);
		if (validToken == null || validToken.getExpiresAt().before(new Date()) || validToken.isUsed()) {
			log.warn("Invalid, expired, or already used token.");
			return invalidRecoveryTokenResult();
		}

		User user = userMapper.findById(validToken.getUserId());
		if (user == null) {
			log.warn("Recovery token references a missing user.");
			return invalidRecoveryTokenResult();
		}

		if (!newPassword.equals(confirmPassword)) {
			log.warn("Passwords do not match.");
			return UserServiceResult.badRequest("Passwords do not match.");
		}

		if (!isPasswordStrong(newPassword)) {
			log.warn("Password does not meet the required strength.");
			return UserServiceResult.badRequest("Password does not meet the required strength.");
		}

		if (passwordEncoder.matches(newPassword, user.getPassword())) {
			log.warn("New password must not be the same as the previous password.");
			return UserServiceResult.badRequest("New password must not be the same as the previous password.");
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		if (userMapper.updateUser(user) > 0) {
			userMapper.clearAllRecoveryTokensForUser(user.getId());
			sendPasswordChangedNotification(user);
			log.info("Password successfully updated for user ID: {}", user.getId());
			return UserServiceResult.ok("Password successfully updated. You can now log in.");
		}

		log.error("Failed to update password for user ID: {}", user.getId());
		userMapper.markTokenAsUsed(validToken.getId());
		return UserServiceResult.internalError("Failed to update password. Please try again.");
	}

	String buildRecoveryLink(String rawToken) {
		String baseUrl = trimToNull(appBaseUrl);
		if (baseUrl == null) {
			baseUrl = "http://localhost:8081";
		}
		baseUrl = baseUrl.replaceAll("/+$", "");
		return baseUrl + "/reset-password?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
	}

	public boolean isPasswordStrong(String password) {
		return password != null && password.length() >= 8 && password.matches(".*[!@#$%^&*()].*");
	}

	private void validateNoUnsupportedFields(java.util.Map<String, Object> unsupportedFields) {
		if (!unsupportedFields.isEmpty()) {
			String fieldName = unsupportedFields.keySet().iterator().next();
			throw new IllegalArgumentException("Unsupported user field: " + fieldName);
		}
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

	private RecoveryToken findMatchingActiveRecoveryToken(String token) {
		List<RecoveryToken> recoveryTokens = userMapper.findActiveRecoveryTokens();
		for (RecoveryToken rt : recoveryTokens) {
			if (passwordEncoder.matches(token, rt.getTokenHash())) {
				return rt;
			}
		}
		return null;
	}

	private UserServiceResult invalidRecoveryTokenResult() {
		return UserServiceResult.badRequest("Invalid or expired token.");
	}

	private void sendPasswordChangedNotification(User user) {
		if (!emailEnabled) {
			log.info("Password change notification email is disabled for user ID {}.", user.getId());
			return;
		}

		try {
			emailService.sendEmail(user.getEmail(), "Password Changed",
					"Your password has been changed. If you did not initiate this change, please contact support immediately.");
		} catch (Exception e) {
			log.warn("Password was changed for user ID {}, but notification email failed: {}", user.getId(),
					e.getMessage());
		}
	}

	private boolean isAccountLocked(User user) {
		return user.getLockTime() != null && user.getLockTime().toInstant().isAfter(Instant.now());
	}

	private String generateSecureToken() {
		return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
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

	private String trimToNull(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return value.trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private boolean isValidRecoveryToken(String token) {
		return token != null && token.length() <= 200 && !containsControlCharacter(token);
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
}
