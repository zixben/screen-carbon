package com.lks.controller;

import com.lks.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {
	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	@Test
	void handleInvalidRequestParameterReturnsSafeBadRequestMessage() {
		ResponseEntity<Map<String, String>> response = handler.handleInvalidRequestParameter();

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Invalid request parameter.", response.getBody().get("message"));
	}

	@Test
	void handleRateLimitExceededReturnsTooManyRequests() {
		ResponseEntity<Map<String, String>> response =
				handler.handleRateLimitExceeded(new RateLimitExceededException("Too many requests."));

		assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
		assertEquals("Too many requests.", response.getBody().get("message"));
	}
}
