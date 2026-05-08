package com.lks.service;

public record UserServiceResult(UserServiceStatus status, String message) {

	public static UserServiceResult ok(String message) {
		return new UserServiceResult(UserServiceStatus.OK, message);
	}

	public static UserServiceResult badRequest(String message) {
		return new UserServiceResult(UserServiceStatus.BAD_REQUEST, message);
	}

	public static UserServiceResult serviceUnavailable(String message) {
		return new UserServiceResult(UserServiceStatus.SERVICE_UNAVAILABLE, message);
	}

	public static UserServiceResult internalError(String message) {
		return new UserServiceResult(UserServiceStatus.INTERNAL_ERROR, message);
	}
}
