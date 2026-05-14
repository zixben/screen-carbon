package com.lks.service;

import com.lks.bean.RecoveryToken;
import com.lks.bean.User;
import com.lks.dto.AdminUserUpdateRequest;
import com.lks.dto.DeleteAccountRequest;
import com.lks.dto.PasswordResetRequest;
import com.lks.dto.UserLoginRequest;
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
	void loginUserAuthenticatesWithVerificationCodeAndPasswordHash() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		User user = new User();
		user.setId(42);
		user.setUsername("newuser");
		user.setPassword(passwordEncoder.encode("Password!"));
		when(userMapper.findByUsername("newuser")).thenReturn(user);

		UserLoginResult result = service.loginUser(loginRequest(" newuser ", "Password!", "abcde"), "abcde");

		assertEquals(UserServiceStatus.OK, result.status());
		assertEquals("Login successful.", result.message());
		assertEquals(user, result.user());
	}

	@Test
	void loginUserRejectsBadVerificationCodeBeforeLookup() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);

		UserLoginResult result = service.loginUser(loginRequest("newuser", "Password!", "wrong"), "abcde");

		assertEquals(UserServiceStatus.BAD_REQUEST, result.status());
		assertEquals("Verification code is incorrect.", result.message());
		verify(userMapper, never()).findByUsername(any(String.class));
	}

	@Test
	void loginUserRejectsInvalidCredentials() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		User user = new User();
		user.setPassword(passwordEncoder.encode("Password!"));
		when(userMapper.findByUsername("newuser")).thenReturn(user);

		UserLoginResult result = service.loginUser(loginRequest("newuser", "wrong-password", "abcde"), "abcde");

		assertEquals(UserServiceStatus.UNAUTHORIZED, result.status());
		assertEquals("Invalid username or password.", result.message());
	}

	@Test
	void deleteCurrentUserDeletesAfterPasswordMatch() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		User user = sessionUser();
		when(userMapper.deleteUser(42)).thenReturn(1);

		UserServiceResult result = service.deleteCurrentUser(deleteAccountRequest(" session-user ", "Password!"), user);

		assertEquals(UserServiceStatus.OK, result.status());
		assertEquals("User deleted successfully.", result.message());
		verify(userMapper).deleteUser(42);
	}

	@Test
	void deleteCurrentUserAcceptsMatchingOptionalEmail() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		DeleteAccountRequest request = deleteAccountRequest("session-user", "Password!");
		request.setEmail(" Session@Example.com ");
		when(userMapper.deleteUser(42)).thenReturn(1);

		UserServiceResult result = service.deleteCurrentUser(request, sessionUser());

		assertEquals(UserServiceStatus.OK, result.status());
		assertEquals("User deleted successfully.", result.message());
		verify(userMapper).deleteUser(42);
	}

	@Test
	void deleteCurrentUserRejectsUsernameMismatchBeforeDeleting() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);

		UserServiceResult result = service.deleteCurrentUser(deleteAccountRequest("other-user", "Password!"),
				sessionUser());

		assertEquals(UserServiceStatus.UNAUTHORIZED, result.status());
		assertEquals("Authentication failed: Username mismatch.", result.message());
		verify(userMapper, never()).deleteUser(any(Integer.class));
	}

	@Test
	void deleteCurrentUserRejectsEmailMismatchBeforeDeleting() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		DeleteAccountRequest request = deleteAccountRequest("session-user", "Password!");
		request.setEmail("other@example.com");

		UserServiceResult result = service.deleteCurrentUser(request, sessionUser());

		assertEquals(UserServiceStatus.UNAUTHORIZED, result.status());
		assertEquals("Authentication failed: Email mismatch.", result.message());
		verify(userMapper, never()).deleteUser(any(Integer.class));
	}

	@Test
	void deleteCurrentUserRejectsUnsupportedFieldBeforeDeleting() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		DeleteAccountRequest request = deleteAccountRequest("session-user", "Password!");
		request.addUnsupportedField("id", 42);

		UserServiceResult result = service.deleteCurrentUser(request, sessionUser());

		assertEquals(UserServiceStatus.BAD_REQUEST, result.status());
		assertEquals("Unsupported user field: id", result.message());
		verifyNoInteractions(userMapper);
	}

	@Test
	void deleteUserAsAdminDeletesExistingUser() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		when(userMapper.findById(77)).thenReturn(user(77, "target-user", "Password!"));
		when(userMapper.deleteUser(77)).thenReturn(1);

		UserServiceResult result = service.deleteUserAsAdmin(77, adminUser());

		assertEquals(UserServiceStatus.OK, result.status());
		assertEquals("success", result.message());
		verify(userMapper).deleteUser(77);
	}

	@Test
	void deleteUserAsAdminRejectsSelfDeletionBeforeDeleting() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);

		UserServiceResult result = service.deleteUserAsAdmin(1, adminUser());

		assertEquals(UserServiceStatus.BAD_REQUEST, result.status());
		assertEquals("Admin users cannot delete their own account from user management.", result.message());
		verifyNoInteractions(userMapper);
	}

	@Test
	void deleteUserAsAdminRejectsMissingUserBeforeDeleting() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		when(userMapper.findById(77)).thenReturn(null);

		UserServiceResult result = service.deleteUserAsAdmin(77, adminUser());

		assertEquals(UserServiceStatus.BAD_REQUEST, result.status());
		assertEquals("User not found.", result.message());
		verify(userMapper, never()).deleteUser(any(Integer.class));
	}

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
	void updateUserAsAdminRejectsHtmlDescriptionBeforePersisting() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		User user = user(42, "olduser", "OldPassword!");
		when(userMapper.findById(42)).thenReturn(user);

		UserServiceResult result = service.updateUserAsAdmin(adminUpdateRequest(42, "newuser", null,
				"<script>alert(1)</script>"));

		assertEquals(UserServiceStatus.BAD_REQUEST, result.status());
		assertEquals("Description contains invalid characters.", result.message());
		verify(userMapper, never()).updateUser(any(User.class));
	}

	@Test
	void updateUserAsAdminRejectsDuplicateUsernameBeforePersisting() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		User user = user(42, "olduser", "OldPassword!");
		User duplicate = user(99, "newuser", "OtherPassword!");
		when(userMapper.findById(42)).thenReturn(user);
		when(userMapper.findByUsername("newuser")).thenReturn(duplicate);

		UserServiceResult result = service.updateUserAsAdmin(adminUpdateRequest(42, "newuser", null, "profile"));

		assertEquals(UserServiceStatus.BAD_REQUEST, result.status());
		assertEquals("Username is already taken.", result.message());
		verify(userMapper, never()).updateUser(any(User.class));
	}

	@Test
	void updateUserAsAdminHashesPasswordAndUpdatesUser() {
		UserMapper userMapper = mock(UserMapper.class);
		UserService service = serviceWith(userMapper, mock(EmailService.class), false, "http://localhost:8081", false);
		User user = user(42, "olduser", "OldPassword!");
		when(userMapper.findById(42)).thenReturn(user);
		when(userMapper.updateUser(any(User.class))).thenReturn(1);

		UserServiceResult result = service.updateUserAsAdmin(adminUpdateRequest(42, "newuser", "NewPassword!",
				"profile"));

		assertEquals(UserServiceStatus.OK, result.status());
		assertEquals("success", result.message());
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userMapper).updateUser(userCaptor.capture());
		User updatedUser = userCaptor.getValue();
		assertEquals("newuser", updatedUser.getUsername());
		assertEquals("profile", updatedUser.getDescription());
		assertNotEquals("NewPassword!", updatedUser.getPassword());
		assertTrue(passwordEncoder.matches("NewPassword!", updatedUser.getPassword()));
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

	private UserLoginRequest loginRequest(String username, String password, String code) {
		UserLoginRequest request = new UserLoginRequest();
		request.setUsername(username);
		request.setPassword(password);
		request.setCode(code);
		return request;
	}

	private DeleteAccountRequest deleteAccountRequest(String username, String password) {
		DeleteAccountRequest request = new DeleteAccountRequest();
		request.setUsername(username);
		request.setPassword(password);
		return request;
	}

	private AdminUserUpdateRequest adminUpdateRequest(Integer id, String username, String password,
			String description) {
		AdminUserUpdateRequest request = new AdminUserUpdateRequest();
		request.setId(id);
		request.setUsername(username);
		request.setPassword(password);
		request.setDescription(description);
		return request;
	}

	private User sessionUser() {
		User user = new User();
		user.setId(42);
		user.setUsername("session-user");
		user.setEmail("session@example.com");
		user.setPassword(passwordEncoder.encode("Password!"));
		return user;
	}

	private User adminUser() {
		User user = new User();
		user.setId(1);
		user.setUsername("admin-user");
		user.setRole("ADMIN");
		return user;
	}

	private User user(Integer id, String username, String password) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(password));
		return user;
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
