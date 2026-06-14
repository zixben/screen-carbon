package com.lks.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorsConfigTest {

    @Test
    void allowsConfiguredCanonicalAndWwwOrigins() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins",
                "https://screencarbontest.gla.ac.uk, https://www.screencarbontest.gla.ac.uk");

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest("POST", "/score/add"));

        assertNotNull(configuration);
        assertEquals("https://screencarbontest.gla.ac.uk",
                configuration.checkOrigin("https://screencarbontest.gla.ac.uk"));
        assertEquals("https://www.screencarbontest.gla.ac.uk",
                configuration.checkOrigin("https://www.screencarbontest.gla.ac.uk"));
        assertNull(configuration.checkOrigin("https://example.com"));
    }
}
