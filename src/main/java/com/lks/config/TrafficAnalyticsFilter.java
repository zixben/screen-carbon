package com.lks.config;

import com.lks.service.TrafficAnalyticsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class TrafficAnalyticsFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(TrafficAnalyticsFilter.class);
    private static final Set<String> TRACKED_PAGE_PATHS = Set.of(
            "/", "/search-results", "/movies", "/movie", "/tv-shows", "/tv", "/details", "/rate",
            "/finish-rating", "/about", "/privacy-notice", "/signup", "/login", "/reset-password"
    );

    private final TrafficAnalyticsService trafficAnalyticsService;

    @Value("${app.analytics.geo.enabled:true}")
    private boolean enabled;

    @Value("${app.analytics.geo.country-header:CF-IPCountry}")
    private String countryHeader;

    @Value("${app.analytics.geo.region-header:X-App-Region}")
    private String regionHeader;

    public TrafficAnalyticsFilter(TrafficAnalyticsService trafficAnalyticsService) {
        this.trafficAnalyticsService = trafficAnalyticsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean shouldRecord = shouldRecord(request);
        filterChain.doFilter(request, response);

        if (shouldRecord && response.getStatus() < 400) {
            try {
                trafficAnalyticsService.recordGeoTraffic(request.getHeader(countryHeader), request.getHeader(regionHeader));
            } catch (RuntimeException e) {
                log.debug("Traffic analytics recording failed: {}", e.getMessage());
            }
        }
    }

    boolean shouldRecord(HttpServletRequest request) {
        if (!enabled || !"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return TRACKED_PAGE_PATHS.contains(path);
    }
}
