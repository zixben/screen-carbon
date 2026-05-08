package com.lks.service;

import com.lks.bean.User;

public record UserLoginResult(UserServiceStatus status, String message, User user) {

	public static UserLoginResult ok(User user) {
		return new UserLoginResult(UserServiceStatus.OK, "Login successful.", user);
	}

	public static UserLoginResult badRequest(String message) {
		return new UserLoginResult(UserServiceStatus.BAD_REQUEST, message, null);
	}

	public static UserLoginResult unauthorized(String message) {
		return new UserLoginResult(UserServiceStatus.UNAUTHORIZED, message, null);
	}
}
