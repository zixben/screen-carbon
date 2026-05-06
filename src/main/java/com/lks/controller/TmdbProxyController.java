package com.lks.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/tmdb")
public class TmdbProxyController {
    private static final String TMDB_API_BASE = "https://api.themoviedb.org/3";

    private final String bearerToken;
    private final HttpClient httpClient;

    @Autowired
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

    private URI buildUpstreamUri(HttpServletRequest request) {
        String requestPrefix = request.getContextPath() + "/tmdb";
        String requestUri = request.getRequestURI();
        if (!requestUri.startsWith(requestPrefix)) {
            throw new IllegalArgumentException("Invalid TMDB proxy path.");
        }

        String path = requestUri.substring(requestPrefix.length());
        if (path.isBlank() || path.contains("..") || path.contains("\\") || path.startsWith("//")) {
            throw new IllegalArgumentException("Invalid TMDB proxy path.");
        }

        String queryString = request.getQueryString();
        return URI.create(TMDB_API_BASE + path + (queryString == null ? "" : "?" + queryString));
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
