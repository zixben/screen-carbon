package com.lks.config;

import com.lks.service.TrafficAnalyticsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TrafficAnalyticsFilterTest {
    private TrafficAnalyticsService trafficAnalyticsService;
    private TrafficAnalyticsFilter filter;

    @BeforeEach
    void setUp() {
        trafficAnalyticsService = mock(TrafficAnalyticsService.class);
        filter = new TrafficAnalyticsFilter(trafficAnalyticsService);
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "countryHeader", "CF-IPCountry");
        ReflectionTestUtils.setField(filter, "regionHeader", "X-App-Region");
    }

    @Test
    void recordsSuccessfulTrackedPageRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/movie");
        request.addHeader("CF-IPCountry", "GB");
        request.addHeader("X-App-Region", "Scotland");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        verify(trafficAnalyticsService).recordGeoTraffic("GB", "Scotland");
    }

    @Test
    void skipsFailedTrackedPageRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/movie");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain failingChain = (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse).setStatus(404);

        filter.doFilterInternal(request, response, failingChain);

        verifyNoInteractions(trafficAnalyticsService);
    }

    @Test
    void skipsWhenDisabled() throws ServletException, IOException {
        ReflectionTestUtils.setField(filter, "enabled", false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/movie");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        verifyNoInteractions(trafficAnalyticsService);
    }

    @Test
    void shouldRecordOnlyConfiguredPublicPages() {
        assertTrue(filter.shouldRecord(new MockHttpServletRequest("GET", "/")));
        assertTrue(filter.shouldRecord(new MockHttpServletRequest("GET", "/rate")));
        assertTrue(filter.shouldRecord(new MockHttpServletRequest("GET", "/finish-rating")));
        assertFalse(filter.shouldRecord(new MockHttpServletRequest("POST", "/rate")));
        assertFalse(filter.shouldRecord(new MockHttpServletRequest("GET", "/score/getAvgFraction")));
        assertFalse(filter.shouldRecord(new MockHttpServletRequest("GET", "/assets/js/movie.js")));
        assertFalse(filter.shouldRecord(new MockHttpServletRequest("GET", "/admin")));
    }

    @Test
    void shouldRecordPageBehindContextPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/screen-carbon/movie");
        request.setContextPath("/screen-carbon");

        assertTrue(filter.shouldRecord(request));
    }
}
