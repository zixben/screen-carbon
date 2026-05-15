package com.lks.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lks.config.CloudflareAnalyticsProperties;
import com.lks.dto.CloudflareAnalyticsSummary;
import com.lks.dto.CloudflareAnalyticsSummary.MetricRow;
import com.lks.dto.CloudflareAnalyticsSummary.TimeBucket;

@Service
public class CloudflareAnalyticsService {
    private static final URI GRAPHQL_URI = URI.create("https://api.cloudflare.com/client/v4/graphql");
    private static final DateTimeFormatter BUCKET_FORMATTER = DateTimeFormatter.ofPattern("dd MMM HH:mm")
            .withZone(ZoneId.of("Europe/London"));
    private static final String SUMMARY_QUERY = """
            query WebAnalyticsSummary($accountTag: string, $filter: AccountRumPageloadEventsAdaptiveGroupsFilter_InputObject) {
              viewer {
                accounts(filter: { accountTag: $accountTag }) {
                  total: rumPageloadEventsAdaptiveGroups(filter: $filter, limit: 1) {
                    count
                    sum {
                      visits
                    }
                  }
                  topPaths: rumPageloadEventsAdaptiveGroups(filter: $filter, limit: 10, orderBy: [count_DESC]) {
                    count
                    sum {
                      visits
                    }
                    dimensions {
                      metric: requestPath
                    }
                  }
                  countries: rumPageloadEventsAdaptiveGroups(filter: $filter, limit: 10, orderBy: [count_DESC]) {
                    count
                    sum {
                      visits
                    }
                    dimensions {
                      metric: countryName
                    }
                  }
                  browsers: rumPageloadEventsAdaptiveGroups(filter: $filter, limit: 10, orderBy: [count_DESC]) {
                    count
                    sum {
                      visits
                    }
                    dimensions {
                      metric: userAgentBrowser
                    }
                  }
                  devices: rumPageloadEventsAdaptiveGroups(filter: $filter, limit: 10, orderBy: [count_DESC]) {
                    count
                    sum {
                      visits
                    }
                    dimensions {
                      metric: deviceType
                    }
                  }
                  series: rumPageloadEventsAdaptiveGroups(filter: $filter, limit: 200, orderBy: [datetimeHour_ASC]) {
                    count
                    sum {
                      visits
                    }
                    dimensions {
                      datetimeHour
                    }
                  }
                }
              }
            }
            """;

    private final CloudflareAnalyticsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Clock clock;
    private volatile CachedSummary cachedSummary;

    @Autowired
    public CloudflareAnalyticsService(CloudflareAnalyticsProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newHttpClient(), Clock.systemUTC());
    }

    CloudflareAnalyticsService(CloudflareAnalyticsProperties properties, ObjectMapper objectMapper,
            HttpClient httpClient, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.clock = clock;
    }

    public CloudflareAnalyticsSummary getSummary() {
        if (!properties.isEnabled()) {
            return CloudflareAnalyticsSummary.unavailable("Cloudflare analytics reporting is disabled.");
        }
        if (!properties.isConfigured()) {
            return CloudflareAnalyticsSummary.unavailable(
                    "Cloudflare analytics reporting is not configured. Add an account ID and API token.");
        }

        Instant now = Instant.now(clock);
        CachedSummary cached = cachedSummary;
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return cached.summary();
        }

        Instant from = now.minusSeconds(properties.getLookbackDays() * 24L * 60L * 60L);
        try {
            HttpRequest request = HttpRequest.newBuilder(GRAPHQL_URI)
                    .timeout(java.time.Duration.ofSeconds(12))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiToken())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(from, now)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            CloudflareAnalyticsSummary summary = parseResponse(response.statusCode(), response.body(), from, now);
            cachedSummary = new CachedSummary(summary, now.plusSeconds(properties.getCacheSeconds()));
            return summary;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CloudflareAnalyticsSummary.unavailable("Cloudflare analytics request was interrupted.");
        } catch (IOException e) {
            return CloudflareAnalyticsSummary.unavailable("Cloudflare analytics request failed.");
        }
    }

    String buildRequestBody(Instant from, Instant to) throws JsonProcessingException {
        List<Map<String, Object>> filterParts = new ArrayList<>();
        filterParts.add(Map.of("datetime_geq", from.toString(), "datetime_leq", to.toString()));
        filterParts.add(Map.of("bot", 0));

        String requestHost = properties.getRequestHost();
        if (requestHost != null) {
            filterParts.add(Map.of("requestHost", requestHost));
        }
        String siteTag = properties.getSiteTag();
        if (siteTag != null) {
            filterParts.add(Map.of("siteTag", siteTag));
        }

        Map<String, Object> payload = Map.of(
                "query", SUMMARY_QUERY,
                "variables", Map.of(
                        "accountTag", properties.getAccountId(),
                        "filter", Map.of("AND", filterParts)));
        return objectMapper.writeValueAsString(payload);
    }

    CloudflareAnalyticsSummary parseResponse(int statusCode, String body, Instant from, Instant to)
            throws JsonProcessingException {
        if (statusCode < 200 || statusCode >= 300) {
            return CloudflareAnalyticsSummary.unavailable("Cloudflare analytics returned HTTP " + statusCode + ".");
        }

        JsonNode root = objectMapper.readTree(body);
        JsonNode errors = root.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            String message = errors.get(0).path("message").asText("Unknown Cloudflare analytics error.");
            return CloudflareAnalyticsSummary.unavailable("Cloudflare analytics request failed: " + message);
        }

        JsonNode account = root.path("data").path("viewer").path("accounts").path(0);
        if (account.isMissingNode() || account.isNull()) {
            return CloudflareAnalyticsSummary.unavailable("Cloudflare analytics returned no account data.");
        }

        JsonNode total = account.path("total").path(0);
        long pageViews = total.path("count").asLong(0);
        long visits = total.path("sum").path("visits").asLong(0);

        return new CloudflareAnalyticsSummary(
                true,
                true,
                "Cloudflare analytics loaded successfully.",
                nullToEmpty(properties.getRequestHost()),
                properties.getLookbackDays(),
                from,
                to,
                Instant.now(clock),
                pageViews,
                visits,
                readMetricRows(account.path("topPaths"), "Unknown path"),
                readMetricRows(account.path("countries"), "Unknown country"),
                readMetricRows(account.path("browsers"), "Unknown browser"),
                readMetricRows(account.path("devices"), "Unknown device"),
                readTimeBuckets(account.path("series")));
    }

    private List<MetricRow> readMetricRows(JsonNode groups, String fallbackLabel) {
        List<MetricRow> rows = new ArrayList<>();
        if (!groups.isArray()) {
            return rows;
        }
        for (JsonNode group : groups) {
            String label = group.path("dimensions").path("metric").asText("");
            if (label == null || label.isBlank()) {
                label = fallbackLabel;
            }
            rows.add(new MetricRow(label, group.path("count").asLong(0),
                    group.path("sum").path("visits").asLong(0)));
        }
        return rows;
    }

    private List<TimeBucket> readTimeBuckets(JsonNode groups) {
        List<TimeBucket> buckets = new ArrayList<>();
        if (!groups.isArray()) {
            return buckets;
        }
        for (JsonNode group : groups) {
            String rawLabel = group.path("dimensions").path("datetimeHour").asText("");
            String label = formatBucketLabel(rawLabel);
            buckets.add(new TimeBucket(label, group.path("count").asLong(0),
                    group.path("sum").path("visits").asLong(0)));
        }
        return buckets;
    }

    private String formatBucketLabel(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        try {
            return BUCKET_FORMATTER.format(Instant.parse(value));
        } catch (RuntimeException e) {
            return value;
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record CachedSummary(CloudflareAnalyticsSummary summary, Instant expiresAt) {
    }
}
