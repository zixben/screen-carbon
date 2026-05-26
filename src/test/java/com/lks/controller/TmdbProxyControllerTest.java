package com.lks.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void buildsAllowedTypedSearchTmdbUri() {
        TmdbProxyController controller = new TmdbProxyController("token", HttpClient.newHttpClient());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tmdb/search/movie");
        request.setQueryString("query=matrix&include_adult=false&language=en-US&page=1");

        URI uri = controller.buildUpstreamUri(request);

        assertEquals("https://api.themoviedb.org/3/search/movie?query=matrix&include_adult=false&language=en-US&page=1",
                uri.toString());
    }

    @Test
    void forcesAdultFilterOnSearchTmdbUri() {
        TmdbProxyController controller = new TmdbProxyController("token", HttpClient.newHttpClient());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tmdb/search/movie");
        request.setQueryString("query=matrix&include_adult=true&language=en-US&page=1");

        URI uri = controller.buildUpstreamUri(request);

        assertEquals("https://api.themoviedb.org/3/search/movie?query=matrix&include_adult=false&language=en-US&page=1",
                uri.toString());
    }

    @Test
    void appendsAdultFilterToSearchTmdbUriWhenMissing() {
        TmdbProxyController controller = new TmdbProxyController("token", HttpClient.newHttpClient());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tmdb/search/tv");
        request.setQueryString("query=matrix&language=en-US&page=1");

        URI uri = controller.buildUpstreamUri(request);

        assertEquals("https://api.themoviedb.org/3/search/tv?query=matrix&language=en-US&page=1&include_adult=false",
                uri.toString());
    }

    @Test
    void doesNotAppendAdultFilterToDetailTmdbUri() {
        TmdbProxyController controller = new TmdbProxyController("token", HttpClient.newHttpClient());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tmdb/movie/123");
        request.setQueryString("language=en-US");

        URI uri = controller.buildUpstreamUri(request);

        assertEquals("https://api.themoviedb.org/3/movie/123?language=en-US", uri.toString());
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

    @Test
    void removesAdultItemsFromListResponses() throws Exception {
        TmdbProxyController controller = new TmdbProxyController("token", HttpClient.newHttpClient());
        String responseBody = """
                {
                  "results": [
                    {"id": 1, "title": "Safe title", "adult": false},
                    {"id": 2, "title": "Adult title", "adult": true}
                  ],
                  "total_results": 2
                }
                """;

        String sanitized = controller.sanitizeAdultContent("/search/movie", responseBody);

        JsonNode results = new ObjectMapper().readTree(sanitized).get("results");
        assertEquals(1, results.size());
        assertEquals("Safe title", results.get(0).get("title").asText());
        assertFalse(sanitized.contains("Adult title"));
    }

    @Test
    void removesAdultKnownForItemsFromPersonSearchResponses() throws Exception {
        TmdbProxyController controller = new TmdbProxyController("token", HttpClient.newHttpClient());
        String responseBody = """
                {
                  "results": [
                    {
                      "id": 1,
                      "name": "Performer",
                      "adult": false,
                      "known_for": [
                        {"id": 2, "title": "Safe title", "adult": false},
                        {"id": 3, "title": "Adult title", "adult": true}
                      ]
                    }
                  ]
                }
                """;

        String sanitized = controller.sanitizeAdultContent("/search/person", responseBody);

        JsonNode knownFor = new ObjectMapper().readTree(sanitized).get("results").get(0).get("known_for");
        assertEquals(1, knownFor.size());
        assertEquals("Safe title", knownFor.get(0).get("title").asText());
        assertFalse(sanitized.contains("Adult title"));
    }

    @Test
    void blocksAdultDetailResponses() {
        TmdbProxyController controller = new TmdbProxyController("token", HttpClient.newHttpClient());

        String sanitized = controller.sanitizeAdultContent("/movie/123", "{\"id\":123,\"adult\":true}");

        assertNull(sanitized);
    }
}
