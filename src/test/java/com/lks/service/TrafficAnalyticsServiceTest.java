package com.lks.service;

import com.lks.dto.TrafficGeoStat;
import com.lks.mapper.TrafficAnalyticsMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrafficAnalyticsServiceTest {

    @Test
    void recordGeoTrafficAggregatesNormalizedCountryAndRegion() {
        TrafficAnalyticsMapper mapper = mock(TrafficAnalyticsMapper.class);
        TrafficAnalyticsService service = new TrafficAnalyticsService(mapper);

        service.recordGeoTraffic(" gb ", " Scotland ");

        verify(mapper).ensureTrafficGeoDailyTable();
        verify(mapper).incrementDailyTraffic("GB", "Scotland");
    }

    @Test
    void recordGeoTrafficDoesNotStoreRawIpOrInvalidHeaders() {
        TrafficAnalyticsMapper mapper = mock(TrafficAnalyticsMapper.class);
        TrafficAnalyticsService service = new TrafficAnalyticsService(mapper);

        service.recordGeoTraffic("192.168.0.1", "<script>");

        verify(mapper).incrementDailyTraffic("ZZ", "Unknown");
    }

    @Test
    void recordGeoTrafficSkipsWhenAnalyticsTableIsUnavailable() {
        TrafficAnalyticsMapper mapper = mock(TrafficAnalyticsMapper.class);
        doThrow(new RuntimeException("no create permission")).when(mapper).ensureTrafficGeoDailyTable();
        TrafficAnalyticsService service = new TrafficAnalyticsService(mapper);

        service.recordGeoTraffic("GB", "Scotland");
        service.recordGeoTraffic("US", "California");

        verify(mapper, times(1)).ensureTrafficGeoDailyTable();
        verify(mapper, never()).incrementDailyTraffic(anyString(), anyString());
    }

    @Test
    void getGeoTrafficValidatesRange() {
        TrafficAnalyticsService service = new TrafficAnalyticsService(mock(TrafficAnalyticsMapper.class));

        assertThrows(IllegalArgumentException.class, () -> service.getGeoTraffic(0, 20));
        assertThrows(IllegalArgumentException.class, () -> service.getGeoTraffic(30, 101));
    }

    @Test
    void getGeoTrafficReturnsMapperResults() {
        TrafficAnalyticsMapper mapper = mock(TrafficAnalyticsMapper.class);
        TrafficGeoStat stat = new TrafficGeoStat();
        stat.setCountryCode("GB");
        stat.setRegion("Scotland");
        stat.setRequestCount(12L);
        when(mapper.findGeoTrafficSince(anyString(), eq(20))).thenReturn(List.of(stat));
        TrafficAnalyticsService service = new TrafficAnalyticsService(mapper);

        List<TrafficGeoStat> result = service.getGeoTraffic(30, 20);

        assertEquals(1, result.size());
        assertEquals("GB", result.get(0).getCountryCode());
        assertEquals(12L, result.get(0).getRequestCount());
    }
}
