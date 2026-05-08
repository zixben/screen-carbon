package com.lks.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/tmdb")
public class TmdbProxyController {
    private static final String TMDB_API_BASE = "https://api.themoviedb.org/3";
    private static final int MAX_QUERY_STRING_LENGTH = 600;
    private static final Pattern SAFE_PATH_PATTERN = Pattern.compile("^/[A-Za-z0-9_./-]+$");
    private static final Pattern MEDIA_DETAIL_PATH = Pattern.compile("^/(movie|tv)/\\d+$");
    private static final Pattern MEDIA_CREDITS_PATH = Pattern.compile("^/(movie|tv)/\\d+/credits$");
    private static final Pattern PERSON_DETAIL_PATH = Pattern.compile("^/person/\\d+$");
    private static final Pattern PERSON_CREDITS_PATH = Pattern.compile("^/person/\\d+/combined_credits$");
    private static final Set<String> ALLOWED_FIXED_PATHS = Set.of(
            "/discover/movie",
            "/discover/tv",
            "/search/multi",
            "/trending/all/day"
    );

    private final String bearerToken;
    private final HttpClient httpClient;

    public TmdbProxyController(@Value("${app.tmdb.bearer-token:}") String bearerToken) {
        this(bearerToken, HttpClient.newHttpClient());
    }

    TmdbProxyController(String bearerToken, HttpClient httpClient) {
        this.bearerToken = normalizeBearerToken(bearerToken);
        this.httpClient = httpClient;
    }

    @GetMapping("/**")
    public ResponseEntity<?> proxy(HttpServletRequest request) {
        if (bearerToken == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "TMDB API token is not configured."));
        }

        URI upstreamUri;
        try {
            upstreamUri = buildUpstreamUri(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        HttpRequest upstreamRequest = HttpRequest.newBuilder(upstreamUri)
                .timeout(Duration.ofSeconds(10))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .GET()
                .build();

        try {
            HttpResponse<String> upstreamResponse = httpClient.send(
                    upstreamRequest,
                    HttpResponse.BodyHandlers.ofString()
            );
            return ResponseEntity.status(upstreamResponse.statusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(upstreamResponse.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "TMDB request was interrupted."));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "TMDB request failed."));
        }
    }

    URI buildUpstreamUri(HttpServletRequest request) {
        String requestPrefix = request.getContextPath() + "/tmdb";
        String requestUri = request.getRequestURI();
        if (!requestUri.startsWith(requestPrefix)) {
            throw new IllegalArgumentException("Invalid TMDB proxy path.");
        }

        String path = requestUri.substring(requestPrefix.length());
        if (path.isBlank() || path.contains("..") || path.contains("\\") || path.startsWith("//")
                || !SAFE_PATH_PATTERN.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid TMDB proxy path.");
        }
        if (!isAllowedPath(path)) {
            throw new IllegalArgumentException("TMDB proxy path is not allowed.");
        }

        String queryString = request.getQueryString();
        validateQueryString(queryString);
        return URI.create(TMDB_API_BASE + path + (queryString == null ? "" : "?" + queryString));
    }

    private boolean isAllowedPath(String path) {
        return ALLOWED_FIXED_PATHS.contains(path)
                || MEDIA_DETAIL_PATH.matcher(path).matches()
                || MEDIA_CREDITS_PATH.matcher(path).matches()
                || PERSON_DETAIL_PATH.matcher(path).matches()
                || PERSON_CREDITS_PATH.matcher(path).matches();
    }

    private void validateQueryString(String queryString) {
        if (queryString == null) {
            return;
        }
        if (queryString.length() > MAX_QUERY_STRING_LENGTH) {
            throw new IllegalArgumentException("TMDB proxy query is too long.");
        }
        for (int i = 0; i < queryString.length(); i++) {
            if (Character.isISOControl(queryString.charAt(i))) {
                throw new IllegalArgumentException("Invalid TMDB proxy query.");
            }
        }
    }

    private String normalizeBearerToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        String normalized = token.trim();
        if (normalized.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return normalized.substring("Bearer ".length()).trim();
        }
        return normalized;
    }
}
