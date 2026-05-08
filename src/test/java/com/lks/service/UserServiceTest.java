package com.lks.service;

import com.lks.bean.RecoveryToken;
import com.lks.bean.User;
import com.lks.dto.PasswordResetRequest;
import com.lks.dto.UserRegistrationRequest;
import com.lks.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserServiceTest {

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Test
	void registerUserHashesPasswordAndSavesNormalizedFields() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		UserRegistrationRequest request = registrationRequest();
		when(userMapper.saveUser(any(User.class))).thenReturn(1);

		UserServiceResult result = service.registerUser(request);

		assertEquals(UserServiceStatus.OK, result.status());
		assertEquals("success", result.message());
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userMapper).saveUser(userCaptor.capture());
		User savedUser = userCaptor.getValue();
		assertEquals("newuser", savedUser.getUsername());
		assertEquals("New User", savedUser.getFullName());
		assertEquals("new@example.com", savedUser.getEmail());
		assertEquals("profile", savedUser.getDescription());
		assertNotEquals("Password!", savedUser.getPassword());
		assertTrue(passwordEncoder.matches("Password!", savedUser.getPassword()));
	}

	@Test
	void registerUserRejectsUnsupportedFieldBeforeQuerying() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		UserRegistrationRequest request = registrationRequest();
		request.addUnsupportedField("role", "ADMIN");

		UserServiceResult result = service.registerUser(request);

		assertEquals(UserServiceStatus.BAD_REQUEST, result.status());
		assertEquals("Unsupported user field: role", result.message());
		verifyNoInteractions(userMapper);
	}

	@Test
	void registerUserRejectsDuplicateEmailBeforeSaving() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		UserRegistrationRequest request = registrationRequest();
		when(userMapper.findByEmail("new@example.com")).thenReturn(new User());

		UserServiceResult result = service.registerUser(request);

		assertEquals(UserServiceStatus.BAD_REQUEST, result.status());
		assertEquals("Email is already in use.", result.message());
		verify(userMapper).findByUsername(eq("newuser"));
		verify(userMapper, never()).saveUser(any(User.class));
	}

	@Test
	void buildRecoveryLinkUsesConfiguredBaseUrlAndEncodesToken() {
		UserService service = serviceWith(mock(UserMapper.class), mock(EmailService.class), false,
				"https://example.test/app/", false);

		String link = service.buildRecoveryLink("abc+123/=");

		assertEquals("https://example.test/app/reset-password?token=abc%2B123%2F%3D", link);
	}

	@Test
	void buildRecoveryLinkFallsBackToLocalhostWhenBaseUrlIsBlank() {
		UserService service = serviceWith(mock(UserMapper.class), mock(EmailService.class), false, " ", false);

		String link = service.buildRecoveryLink("token");

		assertEquals("http://localhost:8081/reset-password?token=token", link);
	}

	@Test
	void recoverPasswordReturnsServiceUnavailableWhenEmailIsDisabledAndLinkLoggingOff() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		User user = new User();
		user.setId(42);
		when(userMapper.findByEmail("test@example.com")).thenReturn(user);

		UserServiceResult result = service.recoverPassword("test@example.com");

		assertEquals(UserServiceStatus.SERVICE_UNAVAILABLE, result.status());
		assertEquals("Password recovery email is not configured.", result.message());
		verify(userMapper, never()).insertRecoveryToken(any(Integer.class), any(String.class), any(Timestamp.class));
	}

	@Test
	void updatePasswordSkipsNotificationEmailWhenEmailIsDisabled() throws Exception {
		UserMapper userMapper = mock(UserMapper.class);
		EmailService emailService = mock(EmailService.class);
		UserService service = serviceWith(userMapper, emailService, false, "http://localhost:8081", false);
		stubValidReset(userMapper, "reset-token");

		UserServiceResult result = service.updatePassword(passwordResetRequest("reset-token", "NewPassword!",
				"NewPassword!"));

		assertEquals(UserServiceStatus.OK, result.status());
		verify(userMapper).clearAllRecoveryTokensForUser(42);
		verifyNoInteractions(emailService);
	}

	@Test
	void updatePasswordStillSucceedsWhenNotificationEmailFails() throws Exception {
		UserMapper userMapper = mock(UserMapper.class);
		EmailService emailService = mock(EmailService.class);
		UserService service = serviceWith(userMapper, emailService, true, "http://localhost:8081", false);
		stubValidReset(userMapper, "reset-token");
		doThrow(new Exception("SMTP unavailable")).when(emailService)
				.sendEmail(any(String.class), any(String.class), any(String.class));

		UserServiceResult result = service.updatePassword(passwordResetRequest("reset-token", "NewPassword!",
				"NewPassword!"));

		assertEquals(UserServiceStatus.OK, result.status());
		verify(userMapper).clearAllRecoveryTokensForUser(42);
		verify(emailService).sendEmail(any(String.class), any(String.class), any(String.class));
	}

	@Test
	void updatePasswordRejectsMalformedTokenBeforeLookup() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);

		UserServiceResult result = service.updatePassword(passwordResetRequest("x".repeat(201), "NewPassword!",
				"NewPassword!"));

		assertEquals(UserServiceStatus.BAD_REQUEST, result.status());
		assertEquals("Invalid or expired token.", result.message());
		verify(userMapper, never()).findActiveRecoveryTokens();
	}

	@Test
	void updatePasswordRejectsMissingPasswordBeforeLookup() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);

		UserServiceResult result = service.updatePassword(passwordResetRequest("reset-token", null, "NewPassword!"));

		assertEquals(UserServiceStatus.BAD_REQUEST, result.status());
		assertEquals("Password is required.", result.message());
		verify(userMapper, never()).findActiveRecoveryTokens();
	}

	private UserService serviceWith(UserMapper userMapper, EmailService emailService, boolean emailEnabled,
			String appBaseUrl, boolean logRecoveryLink) {
		return new UserService(userMapper, emailService, passwordEncoder, appBaseUrl, emailEnabled, logRecoveryLink);
	}

	private PasswordResetRequest passwordResetRequest(String token, String newPassword, String confirmPassword) {
		PasswordResetRequest request = new PasswordResetRequest();
		request.setToken(token);
		request.setNewPassword(newPassword);
		request.setConfirmPassword(confirmPassword);
		return request;
	}

	private UserRegistrationRequest registrationRequest() {
		UserRegistrationRequest request = new UserRegistrationRequest();
		request.setUsername(" newuser ");
		request.setFullName(" New User ");
		request.setEmail(" New@Example.com ");
		request.setDescription(" profile ");
		request.setPassword("Password!");
		request.setConfirmPass("Password!");
		return request;
	}

	private void stubValidReset(UserMapper userMapper, String rawToken) {
		RecoveryToken recoveryToken = new RecoveryToken();
		recoveryToken.setId(7);
		recoveryToken.setUserId(42);
		recoveryToken.setTokenHash(passwordEncoder.encode(rawToken));
		recoveryToken.setExpiresAt(Timestamp.from(Instant.now().plus(Duration.ofMinutes(10))));

		User user = new User();
		user.setId(42);
		user.setEmail("test@example.com");
		user.setPassword(passwordEncoder.encode("OldPassword!"));

		when(userMapper.findActiveRecoveryTokens()).thenReturn(List.of(recoveryToken));
		when(userMapper.findById(42)).thenReturn(user);
		when(userMapper.updateUser(any(User.class))).thenReturn(1);
	}
}
