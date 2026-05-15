package com.lks.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudflareAnalyticsProperties {
    private static final int DEFAULT_LOOKBACK_DAYS = 7;
    private static final int DEFAULT_CACHE_SECONDS = 300;

    private final boolean enabled;
    private final String accountId;
    private final String apiToken;
    private final String siteTag;
    private final String requestHost;
    private final String appBaseUrl;
    private final int lookbackDays;
    private final int cacheSeconds;

    public CloudflareAnalyticsProperties(
            @Value("${app.cloudflare.analytics.enabled:false}") boolean enabled,
            @Value("${app.cloudflare.analytics.account-id:}") String accountId,
            @Value("${app.cloudflare.analytics.api-token:}") String apiToken,
            @Value("${app.cloudflare.analytics.site-tag:}") String siteTag,
            @Value("${app.cloudflare.analytics.request-host:}") String requestHost,
            @Value("${app.base-url:}") String appBaseUrl,
            @Value("${app.cloudflare.analytics.lookback-days:7}") int lookbackDays,
            @Value("${app.cloudflare.analytics.cache-seconds:300}") int cacheSeconds) {
        this.enabled = enabled;
        this.accountId = accountId;
        this.apiToken = apiToken;
        this.siteTag = siteTag;
        this.requestHost = requestHost;
        this.appBaseUrl = appBaseUrl;
        this.lookbackDays = lookbackDays;
        this.cacheSeconds = cacheSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConfigured() {
        return enabled && normalizedAccountId() != null && normalizedApiToken() != null;
    }

    public String getAccountId() {
        return normalizedAccountId();
    }

    public String getApiToken() {
        return normalizedApiToken();
    }

    public String getSiteTag() {
        return normalize(siteTag);
    }

    public String getRequestHost() {
        String configuredHost = normalizeHost(requestHost);
        if (configuredHost != null) {
            return configuredHost;
        }
        return hostFromBaseUrl();
    }

    public int getLookbackDays() {
        if (lookbackDays < 1) {
            return DEFAULT_LOOKBACK_DAYS;
        }
        return Math.min(lookbackDays, 30);
    }

    public int getCacheSeconds() {
        if (cacheSeconds < 30) {
            return DEFAULT_CACHE_SECONDS;
        }
        return Math.min(cacheSeconds, 3600);
    }

    private String normalizedAccountId() {
        return normalize(accountId);
    }

    private String normalizedApiToken() {
        String normalized = normalize(apiToken);
        if (normalized == null) {
            return null;
        }
        if (normalized.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return normalize(normalized.substring("Bearer ".length()));
        }
        return normalized;
    }

    private String hostFromBaseUrl() {
        String normalizedBaseUrl = normalize(appBaseUrl);
        if (normalizedBaseUrl == null) {
            return null;
        }

        try {
            URI uri = URI.create(normalizedBaseUrl);
            return normalizeHost(uri.getHost());
        } catch (IllegalArgumentException e) {
            return normalizeHost(normalizedBaseUrl);
        }
    }

    private String normalizeHost(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            try {
                return normalizeHost(URI.create(normalized).getHost());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        int slashIndex = normalized.indexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(0, slashIndex);
        }
        int colonIndex = normalized.indexOf(':');
        if (colonIndex >= 0) {
            normalized = normalized.substring(0, colonIndex);
        }
        return normalize(normalized);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
