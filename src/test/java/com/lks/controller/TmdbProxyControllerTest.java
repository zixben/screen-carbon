package com.lks.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TmdbProxyControllerTest {

    @Test
    void returnsServiceUnavailableWhenTokenIsMissing() {
        TmdbProxyController controller = new TmdbProxyController("", HttpClient.newHttpClient());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tmdb/movie/123");

        ResponseEntity<?> response = controller.proxy(request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void rejectsInvalidProxyPathBeforeCallingTmdb() {
        TmdbProxyController controller = new TmdbProxyController("token", HttpClient.newHttpClient());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tmdb//evil");

        ResponseEntity<?> response = controller.proxy(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
