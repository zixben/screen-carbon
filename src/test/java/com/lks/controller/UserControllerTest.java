package com.lks.controller;

import com.lks.bean.RecoveryToken;
import com.lks.bean.User;
import com.lks.dto.AdminUserUpdateRequest;
import com.lks.dto.UserSearchRequest;
import com.lks.mapper.UserMapper;
import com.lks.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
		UserController controller = new UserController();
		ReflectionTestUtils.setField(controller, "appBaseUrl", "https://example.test/app/");

		String link = controller.buildRecoveryLink("abc+123/=");

		assertEquals("https://example.test/app/reset-password?token=abc%2B123%2F%3D", link);
	}

	@Test
	void buildRecoveryLinkFallsBackToLocalhostWhenBaseUrlIsBlank() {
		UserController controller = new UserController();
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
				controller.updatePassword("reset-token", "NewPassword!", "NewPassword!");

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
				controller.updatePassword("reset-token", "NewPassword!", "NewPassword!");

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(userMapper).clearAllRecoveryTokensForUser(42);
		verify(emailService).sendEmail(any(String.class), any(String.class), any(String.class));
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
		UserController controller = new UserController();
		ReflectionTestUtils.setField(controller, "userMapper", userMapper);
		ReflectionTestUtils.setField(controller, "emailService", emailService);
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
