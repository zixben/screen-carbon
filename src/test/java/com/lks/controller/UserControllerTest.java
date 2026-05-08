package com.lks.controller;

import com.lks.bean.User;
import com.lks.dto.AdminUserUpdateRequest;
import com.lks.dto.PasswordResetRequest;
import com.lks.dto.UserLoginRequest;
import com.lks.dto.UserRegistrationRequest;
import com.lks.dto.UserResponse;
import com.lks.dto.UserSearchRequest;
import com.lks.exception.RateLimitExceededException;
import com.lks.mapper.UserMapper;
import com.lks.service.RequestRateLimiter;
import com.lks.service.UserService;
import com.lks.service.UserServiceResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserControllerTest {

	@Test
	void updatePasswordMapsServiceResult() {
		UserService userService = mock(UserService.class);
		UserController controller = controllerWith(mock(UserMapper.class), userService);
		PasswordResetRequest request = passwordResetRequest("reset-token", "NewPassword!", "NewPassword!");
		when(userService.updatePassword(request)).thenReturn(UserServiceResult.badRequest("Invalid or expired token."));

		ResponseEntity<Map<String, String>> response = controller.updatePassword(request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Invalid or expired token.", response.getBody().get("message"));
		verify(userService).updatePassword(request);
	}

	@Test
	void saveUserMapsServiceResult() {
		UserService userService = mock(UserService.class);
		UserController controller = controllerWith(mock(UserMapper.class), userService);
		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		httpRequest.setRemoteAddr("203.0.113.11");
		UserRegistrationRequest request = new UserRegistrationRequest();
		when(userService.registerUser(request)).thenReturn(UserServiceResult.badRequest("Email is required."));

		ResponseEntity<String> response = controller.saveUser(request, httpRequest);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Email is required.", response.getBody());
		verify(userService).registerUser(request);
	}

	@Test
	void createUserAsAdminMapsServiceResult() {
		UserService userService = mock(UserService.class);
		UserController controller = controllerWith(mock(UserMapper.class), userService);
		UserRegistrationRequest request = new UserRegistrationRequest();
		when(userService.registerUser(request)).thenReturn(UserServiceResult.ok("success"));

		ResponseEntity<String> response = controller.createUserAsAdmin(request, adminSession());

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("success", response.getBody());
		verify(userService).registerUser(request);
	}

	@Test
	void createUserAsAdminRejectsNonAdminBeforeService() {
		UserService userService = mock(UserService.class);
		UserController controller = controllerWith(mock(UserMapper.class), userService);
		UserRegistrationRequest request = new UserRegistrationRequest();

		ResponseEntity<String> response = controller.createUserAsAdmin(request, new MockHttpSession());

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertEquals("Admin access required.", response.getBody());
		verifyNoInteractions(userService);
	}

	@Test
	void currentUserReturnsSessionBackedUserResponse() {
		UserController controller = controllerWith(mock(UserMapper.class), mock(UserService.class));
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
		UserController controller = controllerWith(mock(UserMapper.class), mock(UserService.class));
		MockHttpServletRequest request = new MockHttpServletRequest();

		ResponseEntity<?> response = controller.currentUser(request);

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	void loginUserRateLimitsRepeatedAttemptsFromSameClient() {
		UserMapper userMapper = mock(UserMapper.class);
		UserController controller = controllerWith(userMapper, mock(UserService.class));
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
		UserController controller = controllerWith(userMapper, mock(UserService.class));
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
		UserController controller = controllerWith(userMapper, mock(UserService.class));
		UserSearchRequest request = new UserSearchRequest();
		request.setUsername("a".repeat(51));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> controller.getUserWhere(request, adminSession()));

		assertEquals("Username is too long.", exception.getMessage());
		verifyNoInteractions(userMapper);
	}

	private UserController controllerWith(UserMapper userMapper, UserService userService) {
		return new UserController(userMapper, userService, new RequestRateLimiter(), new BCryptPasswordEncoder());
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

}
