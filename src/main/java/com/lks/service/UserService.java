package com.lks.service;

import com.lks.bean.RecoveryToken;
import com.lks.bean.User;
import com.lks.dto.PasswordResetRequest;
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

	private boolean isValidRecoveryToken(String token) {
		return token != null && token.length() <= 200 && !containsControlCharacter(token);
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
