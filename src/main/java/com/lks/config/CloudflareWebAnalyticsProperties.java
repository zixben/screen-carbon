package com.lks.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudflareWebAnalyticsProperties {
    private final boolean enabled;
    private final String token;
    private final boolean spa;

    public CloudflareWebAnalyticsProperties(
            @Value("${app.cloudflare.web-analytics.enabled:false}") boolean enabled,
            @Value("${app.cloudflare.web-analytics.token:}") String token,
            @Value("${app.cloudflare.web-analytics.spa:false}") boolean spa) {
        this.enabled = enabled;
        this.token = token;
        this.spa = spa;
    }

    public boolean isEnabled() {
        return enabled && normalizedToken() != null;
    }

    public String getBeaconConfigJson() {
        String normalizedToken = normalizedToken();
        if (normalizedToken == null) {
            return "";
        }
        return "{\"token\":\"" + escapeJson(normalizedToken) + "\",\"spa\":" + spa + "}";
    }

    private String normalizedToken() {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return token.trim();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
