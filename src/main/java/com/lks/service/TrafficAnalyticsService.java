package com.lks.service;

import com.lks.dto.TrafficGeoStat;
import com.lks.mapper.TrafficAnalyticsMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class TrafficAnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(TrafficAnalyticsService.class);
    private static final String UNKNOWN_COUNTRY = "ZZ";
    private static final String UNKNOWN_REGION = "Unknown";
    private static final int MAX_REGION_LENGTH = 80;
    private static final int MAX_DAYS = 366;
    private static final int MAX_LIMIT = 100;

    private final TrafficAnalyticsMapper trafficAnalyticsMapper;
    private volatile boolean schemaReady;
    private volatile boolean schemaChecked;

    public TrafficAnalyticsService(TrafficAnalyticsMapper trafficAnalyticsMapper) {
        this.trafficAnalyticsMapper = trafficAnalyticsMapper;
    }

    @PostConstruct
    void initializeSchema() {
        ensureSchemaReady();
    }

    public void recordGeoTraffic(String rawCountryCode, String rawRegion) {
        if (!ensureSchemaReady()) {
            return;
        }

        String countryCode = normalizeCountryCode(rawCountryCode);
        String region = normalizeRegion(rawRegion);
        trafficAnalyticsMapper.incrementDailyTraffic(countryCode, region);
    }

    public List<TrafficGeoStat> getGeoTraffic(int days, int limit) {
        validateRange(days, limit);
        if (!ensureSchemaReady()) {
            return List.of();
        }

        String startDate = LocalDate.now().minusDays(days - 1L).toString();
        return trafficAnalyticsMapper.findGeoTrafficSince(startDate, limit);
    }

    boolean ensureSchemaReady() {
        if (schemaReady) {
            return true;
        }
        if (schemaChecked) {
            return false;
        }

        synchronized (this) {
            if (schemaReady) {
                return true;
            }
            if (schemaChecked) {
                return false;
            }

            try {
                trafficAnalyticsMapper.ensureTrafficGeoDailyTable();
                schemaReady = true;
                return true;
            } catch (RuntimeException e) {
                log.warn("Traffic analytics table is not available: {}", e.getMessage());
                return false;
            } finally {
                schemaChecked = true;
            }
        }
    }

    String normalizeCountryCode(String countryCode) {
        if (countryCode == null) {
            return UNKNOWN_COUNTRY;
        }
        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{2}") || "XX".equals(normalized)) {
            return UNKNOWN_COUNTRY;
        }
        return normalized;
    }

    String normalizeRegion(String region) {
        if (region == null || region.trim().isEmpty()) {
            return UNKNOWN_REGION;
        }
        String normalized = region.trim();
        if (normalized.length() > MAX_REGION_LENGTH) {
            normalized = normalized.substring(0, MAX_REGION_LENGTH);
        }
        if (!normalized.matches("[A-Za-z0-9 ._'-]+")) {
            return UNKNOWN_REGION;
        }
        return normalized;
    }

    private void validateRange(int days, int limit) {
        if (days < 1 || days > MAX_DAYS) {
            throw new IllegalArgumentException("Analytics date range is invalid.");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Analytics limit is invalid.");
        }
    }
}
