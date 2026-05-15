package com.lks.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CloudflareAnalyticsPropertiesTest {

    @Test
    void requiresAccountIdAndApiTokenWhenEnabled() {
        CloudflareAnalyticsProperties properties = new CloudflareAnalyticsProperties(
                true, "account", "token", "", "", "https://example.com", 7, 300);

        assertTrue(properties.isConfigured());
    }

    @Test
    void isNotConfiguredWhenTokenIsMissing() {
        CloudflareAnalyticsProperties properties = new CloudflareAnalyticsProperties(
                true, "account", "", "", "", "https://example.com", 7, 300);

        assertFalse(properties.isConfigured());
    }

    @Test
    void normalizesBearerTokenAndHost() {
        CloudflareAnalyticsProperties properties = new CloudflareAnalyticsProperties(
                true, " account ", "Bearer token-value ", "", "https://screencarbon.gla.ac.uk/path",
                "http://localhost:8081", 7, 300);

        assertEquals("account", properties.getAccountId());
        assertEquals("token-value", properties.getApiToken());
        assertEquals("screencarbon.gla.ac.uk", properties.getRequestHost());
    }

    @Test
    void fallsBackToBaseUrlHost() {
        CloudflareAnalyticsProperties properties = new CloudflareAnalyticsProperties(
                true, "account", "token", "", "", "https://screencarbon.gla.ac.uk", 7, 300);

        assertEquals("screencarbon.gla.ac.uk", properties.getRequestHost());
    }

    @Test
    void clampsLookbackAndCacheRanges() {
        CloudflareAnalyticsProperties properties = new CloudflareAnalyticsProperties(
                true, "account", "token", "", "", "https://example.com", 90, 10);

        assertEquals(30, properties.getLookbackDays());
        assertEquals(300, properties.getCacheSeconds());
    }
}
