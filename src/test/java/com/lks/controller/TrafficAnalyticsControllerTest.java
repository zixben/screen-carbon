package com.lks.controller;

import com.lks.dto.TrafficGeoStat;
import com.lks.service.TrafficAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrafficAnalyticsControllerTest {

    @Test
    void getGeoTrafficReturnsItems() {
        TrafficAnalyticsService service = mock(TrafficAnalyticsService.class);
        TrafficGeoStat stat = new TrafficGeoStat();
        stat.setCountryCode("GB");
        stat.setRegion("Scotland");
        stat.setRequestCount(12L);
        when(service.getGeoTraffic(30, 20)).thenReturn(List.of(stat));
        TrafficAnalyticsController controller = new TrafficAnalyticsController(service);

        ResponseEntity<Map<String, List<TrafficGeoStat>>> response = controller.getGeoTraffic(30, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().get("items").size());
        assertEquals("GB", response.getBody().get("items").get(0).getCountryCode());
    }
}
