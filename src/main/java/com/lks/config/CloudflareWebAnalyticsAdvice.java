package com.lks.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CloudflareWebAnalyticsAdvice {
    private final CloudflareWebAnalyticsProperties properties;

    public CloudflareWebAnalyticsAdvice(CloudflareWebAnalyticsProperties properties) {
        this.properties = properties;
    }

    @ModelAttribute("cloudflareWebAnalytics")
    public CloudflareWebAnalyticsProperties cloudflareWebAnalytics() {
        return properties;
    }
}
