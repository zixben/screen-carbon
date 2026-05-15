package com.lks.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.lks.dto.CloudflareAnalyticsSummary;
import com.lks.dto.CloudflareAnalyticsSummary.MetricRow;
import com.lks.dto.CloudflareAnalyticsSummary.TimeBucket;

class CloudflareAnalyticsTemplateTest {

    @Test
    void rendersTrafficAnalyticsSummary() {
        Context context = new Context();
        context.setVariable("cloudflareWebAnalytics",
                new CloudflareWebAnalyticsProperties(false, "", false));
        context.setVariable("summary", new CloudflareAnalyticsSummary(
                true,
                true,
                "Cloudflare analytics loaded successfully.",
                "screencarbon.gla.ac.uk",
                7,
                Instant.parse("2026-05-14T12:00:00Z"),
                Instant.parse("2026-05-15T12:00:00Z"),
                Instant.parse("2026-05-15T12:05:00Z"),
                42,
                13,
                List.of(new MetricRow("/", 25, 8)),
                List.of(new MetricRow("GB", 20, 7)),
                List.of(new MetricRow("Chrome", 16, 6)),
                List.of(new MetricRow("desktop", 12, 5)),
                List.of(new TimeBucket("15 May 10:00", 9, 3))));

        String html = templateEngine().process("cloudflare-analytics", context);

        assertTrue(html.contains("Traffic Analytics"));
        assertTrue(html.contains("screencarbon.gla.ac.uk"));
        assertTrue(html.contains("Top Pages"));
        assertTrue(html.contains("Chrome"));
    }

    private TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
