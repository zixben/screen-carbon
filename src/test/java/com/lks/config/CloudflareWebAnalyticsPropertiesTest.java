package com.lks.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudflareWebAnalyticsPropertiesTest {

	@Test
	void disabledWhenFeatureFlagIsOff() {
		CloudflareWebAnalyticsProperties properties =
				new CloudflareWebAnalyticsProperties(false, "test-token", false);

		assertFalse(properties.isEnabled());
	}

	@Test
	void disabledWhenTokenIsMissing() {
		CloudflareWebAnalyticsProperties properties =
				new CloudflareWebAnalyticsProperties(true, " ", false);

		assertFalse(properties.isEnabled());
		assertEquals("", properties.getBeaconConfigJson());
	}

	@Test
	void buildsBeaconConfigWhenEnabled() {
		CloudflareWebAnalyticsProperties properties =
				new CloudflareWebAnalyticsProperties(true, " test-token ", false);

		assertTrue(properties.isEnabled());
		assertEquals("{\"token\":\"test-token\",\"spa\":false}", properties.getBeaconConfigJson());
	}

	@Test
	void escapesBeaconConfigToken() {
		CloudflareWebAnalyticsProperties properties =
				new CloudflareWebAnalyticsProperties(true, "test\"token\\value", true);

		assertEquals("{\"token\":\"test\\\"token\\\\value\",\"spa\":true}", properties.getBeaconConfigJson());
	}
}
