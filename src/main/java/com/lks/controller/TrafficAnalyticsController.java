package com.lks.controller;

import com.lks.dto.TrafficGeoStat;
import com.lks.service.TrafficAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/analytics")
public class TrafficAnalyticsController {
    private final TrafficAnalyticsService trafficAnalyticsService;

    public TrafficAnalyticsController(TrafficAnalyticsService trafficAnalyticsService) {
        this.trafficAnalyticsService = trafficAnalyticsService;
    }

    @GetMapping("/geo-traffic")
    public ResponseEntity<Map<String, List<TrafficGeoStat>>> getGeoTraffic(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(Map.of("items", trafficAnalyticsService.getGeoTraffic(days, limit)));
    }
}
