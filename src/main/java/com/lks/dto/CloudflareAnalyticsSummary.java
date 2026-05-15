package com.lks.dto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CloudflareAnalyticsSummary {
    private static final DateTimeFormatter RANGE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
            .withZone(ZoneId.of("Europe/London"));

    private final boolean configured;
    private final boolean available;
    private final String message;
    private final String requestHost;
    private final int lookbackDays;
    private final Instant from;
    private final Instant to;
    private final Instant generatedAt;
    private final long pageViews;
    private final long visits;
    private final List<MetricRow> topPaths;
    private final List<MetricRow> countries;
    private final List<MetricRow> browsers;
    private final List<MetricRow> devices;
    private final List<TimeBucket> series;

    public CloudflareAnalyticsSummary(boolean configured, boolean available, String message, String requestHost,
            int lookbackDays, Instant from, Instant to, Instant generatedAt, long pageViews, long visits,
            List<MetricRow> topPaths, List<MetricRow> countries, List<MetricRow> browsers, List<MetricRow> devices,
            List<TimeBucket> series) {
        this.configured = configured;
        this.available = available;
        this.message = message;
        this.requestHost = requestHost;
        this.lookbackDays = lookbackDays;
        this.from = from;
        this.to = to;
        this.generatedAt = generatedAt;
        this.pageViews = pageViews;
        this.visits = visits;
        this.topPaths = List.copyOf(topPaths);
        this.countries = List.copyOf(countries);
        this.browsers = List.copyOf(browsers);
        this.devices = List.copyOf(devices);
        this.series = List.copyOf(series);
    }

    public static CloudflareAnalyticsSummary unavailable(String message) {
        Instant now = Instant.now();
        return new CloudflareAnalyticsSummary(false, false, message, "", 0, now, now, now, 0, 0,
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public boolean isConfigured() {
        return configured;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestHost() {
        return requestHost;
    }

    public int getLookbackDays() {
        return lookbackDays;
    }

    public String getFromLabel() {
        return RANGE_FORMATTER.format(from);
    }

    public String getToLabel() {
        return RANGE_FORMATTER.format(to);
    }

    public String getGeneratedAtLabel() {
        return RANGE_FORMATTER.format(generatedAt);
    }

    public long getPageViews() {
        return pageViews;
    }

    public long getVisits() {
        return visits;
    }

    public List<MetricRow> getTopPaths() {
        return topPaths;
    }

    public List<MetricRow> getCountries() {
        return countries;
    }

    public List<MetricRow> getBrowsers() {
        return browsers;
    }

    public List<MetricRow> getDevices() {
        return devices;
    }

    public List<TimeBucket> getSeries() {
        return series;
    }

    public static class MetricRow {
        private final String label;
        private final long pageViews;
        private final long visits;

        public MetricRow(String label, long pageViews, long visits) {
            this.label = label;
            this.pageViews = pageViews;
            this.visits = visits;
        }

        public String getLabel() {
            return label;
        }

        public long getPageViews() {
            return pageViews;
        }

        public long getVisits() {
            return visits;
        }
    }

    public static class TimeBucket {
        private final String label;
        private final long pageViews;
        private final long visits;

        public TimeBucket(String label, long pageViews, long visits) {
            this.label = label;
            this.pageViews = pageViews;
            this.visits = visits;
        }

        public String getLabel() {
            return label;
        }

        public long getPageViews() {
            return pageViews;
        }

        public long getVisits() {
            return visits;
        }
    }
}
