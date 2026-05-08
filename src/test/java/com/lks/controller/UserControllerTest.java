package com.lks.controller;

import com.lks.bean.RecoveryToken;
import com.lks.bean.User;
import com.lks.dto.AdminUserUpdateRequest;
import com.lks.dto.PasswordResetRequest;
import com.lks.dto.UserLoginRequest;
import com.lks.dto.UserResponse;
import com.lks.dto.UserSearchRequest;
import com.lks.exception.RateLimitExceededException;
import com.lks.mapper.UserMapper;
import com.lks.service.EmailService;
import com.lks.service.RequestRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserControllerTest {

	@Test
	void buildRecoveryLinkUsesConfiguredBaseUrlAndEncodesToken() {
		UserController controller = controllerWith(mock(UserMapper.class), mock(EmailService.class), false);
		ReflectionTestUtils.setField(controller, "appBaseUrl", "https://example.test/app/");

		String link = controller.buildRecoveryLink("abc+123/=");

		assertEquals("https://example.test/app/reset-password?token=abc%2B123%2F%3D", link);
	}

	@Test
	void buildRecoveryLinkFallsBackToLocalhostWhenBaseUrlIsBlank() {
		UserController controller = controllerWith(mock(UserMapper.class), mock(EmailService.class), false);
		ReflectionTestUtils.setField(controller, "appBaseUrl", " ");

		String link = controller.buildRecoveryLink("token");

		assertEquals("http://localhost:8081/reset-password?token=token", link);
	}

	@Test
	void updatePasswordSkipsNotificationEmailWhenEmailIsDisabled() throws Exception {
		UserMapper userMapper = mock(UserMapper.class);
		EmailService emailService = mock(EmailService.class);
		UserController controller = controllerWith(userMapper, emailService, false);
		stubValidReset(userMapper, "reset-token");

		ResponseEntity<Map<String, String>> response =
				controller.updatePassword(passwordResetRequest("reset-token", "NewPassword!", "NewPassword!"));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(userMapper).clearAllRecoveryTokensForUser(42);
		verifyNoInteractions(emailService);
	}

	@Test
	void updatePasswordStillSucceedsWhenNotificationEmailFails() throws Exception {
		UserMapper userMapper = mock(UserMapper.class);
		EmailService emailService = mock(EmailService.class);
		UserController controller = controllerWith(userMapper, emailService, true);
		stubValidReset(userMapper, "reset-token");
		doThrow(new Exception("SMTP unavailable")).when(emailService)
				.sendEmail(any(String.class), any(String.class), any(String.class));

		ResponseEntity<Map<String, String>> response =
				controller.updatePassword(passwordResetRequest("reset-token", "NewPassword!", "NewPassword!"));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(userMapper).clearAllRecoveryTokensForUser(42);
		verify(emailService).sendEmail(any(String.class), any(String.class), any(String.class));
	}

	@Test
	void updatePasswordRejectsMalformedTokenBeforeLookup() throws Exception {
		UserMapper userMapper = mock(UserMapper.class);
		UserController controller = controllerWith(userMapper, mock(EmailService.class), false);

		ResponseEntity<Map<String, String>> response =
				controller.updatePassword(passwordResetRequest("x".repeat(201), "NewPassword!", "NewPassword!"));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Invalid or expired token.", response.getBody().get("message"));
		verify(userMapper, never()).findActiveRecoveryTokens();
	}

	@Test
	void updatePasswordRejectsMissingPasswordBeforeLookup() throws Exception {
		UserMapper userMapper = mock(UserMapper.class);
		UserController controller = controllerWith(userMapper, mock(EmailService.class), false);

		ResponseEntity<Map<String, String>> response =
				controller.updatePassword(passwordResetRequest("reset-token", null, "NewPassword!"));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Password is required.", response.getBody().get("message"));
		verify(userMapper, never()).findActiveRecoveryTokens();
	}

	@Test
	void currentUserReturnsSessionBackedUserResponse() {
		UserController controller = controllerWith(mock(UserMapper.class), mock(EmailService.class), false);
		MockHttpSession session = new MockHttpSession();
		User user = new User();
		user.setId(42);
		user.setUsername("session-user");
		user.setFullName("Session User");
		user.setEmail("session@example.com");
		user.setRole("USER");
		session.setAttribute("loggedInUser", user);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(session);

		ResponseEntity<?> response = controller.currentUser(request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		UserResponse body = (UserResponse) response.getBody();
		assertEquals(42, body.id());
		assertEquals("session-user", body.username());
		assertEquals("USER", body.role());
	}

	@Test
	void currentUserRejectsMissingSessionUser() {
		UserController controller = controllerWith(mock(UserMapper.class), mock(EmailService.class), false);
		MockHttpServletRequest request = new MockHttpServletRequest();

		ResponseEntity<?> response = controller.currentUser(request);

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	void loginUserRateLimitsRepeatedAttemptsFromSameClient() {
		UserMapper userMapper = mock(UserMapper.class);
		UserController controller = controllerWith(userMapper, mock(EmailService.class), false);
		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		httpRequest.setRemoteAddr("203.0.113.10");
		httpRequest.getSession().setAttribute("vcode", "abcde");
		UserLoginRequest request = new UserLoginRequest();
		request.setUsername("limited-user");
		request.setPassword("wrong-password");
		request.setCode("abcde");

		for (int i = 0; i < 10; i++) {
			ResponseEntity<Map<String, String>> response = controller.loginUser(request, httpRequest);
			assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		}

		RateLimitExceededException exception = assertThrows(RateLimitExceededException.class,
				() -> controller.loginUser(request, httpRequest));
		assertEquals("Too many login attempts. Please try again later.", exception.getMessage());
	}

	@Test
	void updateUserRejectsHtmlDescriptionBeforePersisting() {
		UserMapper userMapper = mock(UserMapper.class);
		UserController controller = controllerWith(userMapper, mock(EmailService.class), false);
		User user = new User();
		user.setId(42);
		user.setUsername("olduser");
		user.setPassword("existing-hash");
		when(userMapper.findById(42)).thenReturn(user);

		AdminUserUpdateRequest request = new AdminUserUpdateRequest();
		request.setId(42);
		request.setUsername("newuser");
		request.setDescription("<script>alert(1)</script>");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> controller.updateUser(request, adminSession()));

		assertEquals("Description contains invalid characters.", exception.getMessage());
		verify(userMapper, never()).updateUser(any(User.class));
	}

	@Test
	void getUserWhereRejectsOverlongSearchTermBeforeQuerying() {
		UserMapper userMapper = mock(UserMapper.class);
		UserController controller = controllerWith(userMapper, mock(EmailService.class), false);
		UserSearchRequest request = new UserSearchRequest();
		request.setUsername("a".repeat(51));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> controller.getUserWhere(request, adminSession()));

		assertEquals("Username is too long.", exception.getMessage());
		verifyNoInteractions(userMapper);
	}

	private UserController controllerWith(UserMapper userMapper, EmailService emailService, boolean emailEnabled) {
		UserController controller = new UserController(userMapper, emailService, new RequestRateLimiter(),
				new BCryptPasswordEncoder());
		ReflectionTestUtils.setField(controller, "emailEnabled", emailEnabled);
		return controller;
	}

	private MockHttpSession adminSession() {
		MockHttpSession session = new MockHttpSession();
		User admin = new User();
		admin.setRole("ADMIN");
		session.setAttribute("loggedInUser", admin);
		return session;
	}

	private PasswordResetRequest passwordResetRequest(String token, String newPassword, String confirmPassword) {
		PasswordResetRequest request = new PasswordResetRequest();
		request.setToken(token);
		request.setNewPassword(newPassword);
		request.setConfirmPassword(confirmPassword);
		return request;
	}

	private void stubValidReset(UserMapper userMapper, String rawToken) {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		RecoveryToken recoveryToken = new RecoveryToken();
		recoveryToken.setId(7);
		recoveryToken.setUserId(42);
		recoveryToken.setTokenHash(encoder.encode(rawToken));
		recoveryToken.setExpiresAt(Timestamp.from(Instant.now().plus(Duration.ofMinutes(10))));

		User user = new User();
		user.setId(42);
		user.setEmail("test@example.com");
		user.setPassword(encoder.encode("OldPassword!"));

		when(userMapper.findActiveRecoveryTokens()).thenReturn(List.of(recoveryToken));
		when(userMapper.findById(42)).thenReturn(user);
		when(userMapper.updateUser(any(User.class))).thenReturn(1);
	}
}
