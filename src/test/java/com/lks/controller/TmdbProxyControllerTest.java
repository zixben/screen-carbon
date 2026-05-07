package com.lks.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void buildsAllowedTmdbUri() {
        TmdbProxyController controller = new TmdbProxyController("token", HttpClient.newHttpClient());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tmdb/discover/movie");
        request.setQueryString("include_adult=false&language=en-US&page=1");

        URI uri = controller.buildUpstreamUri(request);

        assertEquals("https://api.themoviedb.org/3/discover/movie?include_adult=false&language=en-US&page=1",
                uri.toString());
    }

    @Test
    void rejectsDisallowedTmdbPath() {
        TmdbProxyController controller = new TmdbProxyController("token", HttpClient.newHttpClient());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tmdb/authentication/token/new");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.buildUpstreamUri(request));

        assertEquals("TMDB proxy path is not allowed.", exception.getMessage());
    }

    @Test
    void rejectsOverlongTmdbQuery() {
        TmdbProxyController controller = new TmdbProxyController("token", HttpClient.newHttpClient());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tmdb/search/multi");
        request.setQueryString("query=" + "a".repeat(601));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.buildUpstreamUri(request));

        assertEquals("TMDB proxy query is too long.", exception.getMessage());
    }
}
