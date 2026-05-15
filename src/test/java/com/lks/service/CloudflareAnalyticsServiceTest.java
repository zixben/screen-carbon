package com.lks.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lks.config.CloudflareAnalyticsProperties;
import com.lks.dto.CloudflareAnalyticsSummary;

class CloudflareAnalyticsServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void returnsUnavailableWhenReportingIsDisabled() {
        CloudflareAnalyticsService service = new CloudflareAnalyticsService(
                properties(false),
                objectMapper,
                HttpClient.newHttpClient(),
                clock);

        CloudflareAnalyticsSummary summary = service.getSummary();

        assertFalse(summary.isAvailable());
        assertEquals("Cloudflare analytics reporting is disabled.", summary.getMessage());
    }

    @Test
    void buildsPayloadWithHostAndSiteTagFilters() throws Exception {
        CloudflareAnalyticsService service = new CloudflareAnalyticsService(
                properties(true),
                objectMapper,
                HttpClient.newHttpClient(),
                clock);

        String body = service.buildRequestBody(
                Instant.parse("2026-05-14T12:00:00Z"),
                Instant.parse("2026-05-15T12:00:00Z"));
        JsonNode root = objectMapper.readTree(body);
        JsonNode filters = root.path("variables").path("filter").path("AND");

        assertEquals("account-id", root.path("variables").path("accountTag").asText());
        assertTrue(body.contains("rumPageloadEventsAdaptiveGroups"));
        assertEquals("screencarbon.gla.ac.uk", filters.get(2).path("requestHost").asText());
        assertEquals("site-tag", filters.get(3).path("siteTag").asText());
    }

    @Test
    void parsesCloudflareAnalyticsResponse() throws Exception {
        CloudflareAnalyticsService service = new CloudflareAnalyticsService(
                properties(true),
                objectMapper,
                HttpClient.newHttpClient(),
                clock);

        String body = """
                {
                  "data": {
                    "viewer": {
                      "accounts": [
                        {
                          "total": [
                            { "count": 42, "sum": { "visits": 13 } }
                          ],
                          "topPaths": [
                            { "count": 25, "sum": { "visits": 8 }, "dimensions": { "metric": "/" } }
                          ],
                          "countries": [
                            { "count": 20, "sum": { "visits": 7 }, "dimensions": { "metric": "GB" } }
                          ],
                          "browsers": [
                            { "count": 16, "sum": { "visits": 6 }, "dimensions": { "metric": "Chrome" } }
                          ],
                          "devices": [
                            { "count": 12, "sum": { "visits": 5 }, "dimensions": { "metric": "desktop" } }
                          ],
                          "series": [
                            {
                              "count": 9,
                              "sum": { "visits": 3 },
                              "dimensions": { "datetimeHour": "2026-05-15T10:00:00Z" }
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
                """;

        CloudflareAnalyticsSummary summary = service.parseResponse(
                200,
                body,
                Instant.parse("2026-05-14T12:00:00Z"),
                Instant.parse("2026-05-15T12:00:00Z"));

        assertTrue(summary.isAvailable());
        assertEquals(42, summary.getPageViews());
        assertEquals(13, summary.getVisits());
        assertEquals("/", summary.getTopPaths().get(0).getLabel());
        assertEquals("GB", summary.getCountries().get(0).getLabel());
        assertEquals("Chrome", summary.getBrowsers().get(0).getLabel());
        assertEquals("desktop", summary.getDevices().get(0).getLabel());
        assertEquals(9, summary.getSeries().get(0).getPageViews());
    }

    @Test
    void surfacesCloudflareGraphqlErrors() throws Exception {
        CloudflareAnalyticsService service = new CloudflareAnalyticsService(
                properties(true),
                objectMapper,
                HttpClient.newHttpClient(),
                clock);

        CloudflareAnalyticsSummary summary = service.parseResponse(
                200,
                "{\"errors\":[{\"message\":\"bad query\"}]}",
                Instant.parse("2026-05-14T12:00:00Z"),
                Instant.parse("2026-05-15T12:00:00Z"));

        assertFalse(summary.isAvailable());
        assertTrue(summary.getMessage().contains("bad query"));
    }

    private CloudflareAnalyticsProperties properties(boolean enabled) {
        return new CloudflareAnalyticsProperties(
                enabled,
                "account-id",
                "Bearer api-token",
                "site-tag",
                "screencarbon.gla.ac.uk",
                "https://fallback.example",
                7,
                300);
    }
}
