package com.lks.controller;

import com.lks.bean.RecoveryToken;
import com.lks.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebControllerResetPasswordTest {

	@Test
	void resetPasswordWithoutTokenRendersPageMessage() {
		UserMapper userMapper = mock(UserMapper.class);
		WebController controller = new WebController(userMapper, mock(PasswordEncoder.class));
		Model model = new ConcurrentModel();

		String view = controller.showResetPasswordForm(null, model, new MockHttpServletRequest());

		assertEquals("reset-password", view);
		assertEquals("Reset Password", model.getAttribute("title"));
		assertEquals("Invalid or expired reset link. Please request a new password recovery email.",
				model.getAttribute("message"));
		verify(userMapper, never()).findActiveRecoveryTokens();
	}

	@Test
	void resetPasswordWithValidTokenRendersForm() {
		UserMapper userMapper = mock(UserMapper.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		WebController controller = new WebController(userMapper, passwordEncoder);
		RecoveryToken recoveryToken = new RecoveryToken();
		recoveryToken.setTokenHash("hashed-token");
		recoveryToken.setExpiresAt(Timestamp.from(Instant.now().plus(Duration.ofMinutes(10))));
		when(userMapper.findActiveRecoveryTokens()).thenReturn(List.of(recoveryToken));
		when(passwordEncoder.matches("reset-token", "hashed-token")).thenReturn(true);
		Model model = new ConcurrentModel();

		String view = controller.showResetPasswordForm("reset-token", model, new MockHttpServletRequest());

		assertEquals("reset-password", view);
		assertEquals("Reset Password", model.getAttribute("title"));
		assertEquals("reset-token", model.getAttribute("token"));
		assertEquals("/assets/js/reset-password.js", model.getAttribute("scripts"));
	}
}
