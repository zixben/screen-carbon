package com.lks.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
    private static final Set<String> ADULT_FILTERED_QUERY_PATHS = Set.of(
            "/discover/movie",
            "/discover/tv",
            "/search/movie",
            "/search/multi",
            "/search/person",
            "/search/tv"
    );
    private static final Set<String> ADULT_FILTERED_RESULT_PATHS = Set.of(
            "/discover/movie",
            "/discover/tv",
            "/search/movie",
            "/search/multi",
            "/search/person",
            "/search/tv",
            "/trending/all/day"
    );
    private static final Set<String> ALLOWED_FIXED_PATHS = Set.of(
            "/discover/movie",
            "/discover/tv",
            "/search/movie",
            "/search/multi",
            "/search/person",
            "/search/tv",
            "/trending/all/day"
    );

    private final String bearerToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public TmdbProxyController(@Value("${app.tmdb.bearer-token:}") String bearerToken) {
        this(bearerToken, HttpClient.newHttpClient(), new ObjectMapper());
    }

    TmdbProxyController(String bearerToken, HttpClient httpClient) {
        this(bearerToken, httpClient, new ObjectMapper());
    }

    TmdbProxyController(String bearerToken, HttpClient httpClient, ObjectMapper objectMapper) {
        this.bearerToken = normalizeBearerToken(bearerToken);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/**")
    public ResponseEntity<?> proxy(HttpServletRequest request) {
        if (bearerToken == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "TMDB API token is not configured."));
        }

        URI upstreamUri;
        String path;
        try {
            path = extractValidatedPath(request);
            upstreamUri = buildUpstreamUri(request, path);
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
            String responseBody = sanitizeAdultContent(path, upstreamResponse.body());
            if (responseBody == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("message", "TMDB media is unavailable."));
            }
            return ResponseEntity.status(upstreamResponse.statusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
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
        return buildUpstreamUri(request, extractValidatedPath(request));
    }

    private URI buildUpstreamUri(HttpServletRequest request, String path) {
        String queryString = request.getQueryString();
        validateQueryString(queryString);
        queryString = forceAdultFilter(path, queryString);
        validateQueryString(queryString);
        return URI.create(TMDB_API_BASE + path + (queryString == null ? "" : "?" + queryString));
    }

    private String extractValidatedPath(HttpServletRequest request) {
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

        return path;
    }

    private boolean isAllowedPath(String path) {
        return ALLOWED_FIXED_PATHS.contains(path)
                || MEDIA_DETAIL_PATH.matcher(path).matches()
                || MEDIA_CREDITS_PATH.matcher(path).matches()
                || PERSON_DETAIL_PATH.matcher(path).matches()
                || PERSON_CREDITS_PATH.matcher(path).matches();
    }

    private String forceAdultFilter(String path, String queryString) {
        if (!ADULT_FILTERED_QUERY_PATHS.contains(path)) {
            return queryString;
        }
        if (queryString == null || queryString.isBlank()) {
            return "include_adult=false";
        }

        String[] pairs = queryString.split("&", -1);
        List<String> safePairs = new ArrayList<>();
        boolean includedAdultFilter = false;
        for (String pair : pairs) {
            String key = pair;
            int separatorIndex = pair.indexOf('=');
            if (separatorIndex >= 0) {
                key = pair.substring(0, separatorIndex);
            }

            if (isIncludeAdultParameter(key)) {
                if (!includedAdultFilter) {
                    safePairs.add("include_adult=false");
                    includedAdultFilter = true;
                }
            } else {
                safePairs.add(pair);
            }
        }
        if (!includedAdultFilter) {
            safePairs.add("include_adult=false");
        }
        return String.join("&", safePairs);
    }

    private boolean isIncludeAdultParameter(String key) {
        try {
            return "include_adult".equals(URLDecoder.decode(key, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid TMDB proxy query.");
        }
    }

    String sanitizeAdultContent(String path, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return responseBody;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (isAdultDetailPath(path) && isAdultNode(root)) {
                return null;
            }
            if (ADULT_FILTERED_RESULT_PATHS.contains(path) && root instanceof ObjectNode objectNode) {
                filterAdultItems(objectNode, "results");
            } else if (PERSON_CREDITS_PATH.matcher(path).matches() && root instanceof ObjectNode objectNode) {
                filterAdultItems(objectNode, "cast");
                filterAdultItems(objectNode, "crew");
            }
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return responseBody;
        }
    }

    private boolean isAdultDetailPath(String path) {
        return MEDIA_DETAIL_PATH.matcher(path).matches() || PERSON_DETAIL_PATH.matcher(path).matches();
    }

    private void filterAdultItems(ObjectNode objectNode, String fieldName) {
        JsonNode items = objectNode.get(fieldName);
        if (!(items instanceof ArrayNode arrayNode)) {
            return;
        }

        ArrayNode safeItems = objectMapper.createArrayNode();
        for (JsonNode item : arrayNode) {
            JsonNode safeItem = filterAdultItem(item);
            if (safeItem != null) {
                safeItems.add(safeItem);
            }
        }
        objectNode.set(fieldName, safeItems);
    }

    private JsonNode filterAdultItem(JsonNode item) {
        if (item == null || isAdultNode(item)) {
            return null;
        }
        if (item instanceof ObjectNode objectNode) {
            filterAdultItems(objectNode, "known_for");
        }
        return item;
    }

    private boolean isAdultNode(JsonNode node) {
        return node != null && node.path("adult").asBoolean(false);
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
