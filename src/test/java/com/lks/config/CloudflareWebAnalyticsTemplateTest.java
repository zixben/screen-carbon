package com.lks.config;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudflareWebAnalyticsTemplateTest {

	@Test
	void cloudflareFragmentRendersBeaconWhenConfigured() {
		Context context = new Context();
		context.setVariable("cloudflareWebAnalytics",
				new CloudflareWebAnalyticsProperties(true, "test-token", false));

		String html = templateEngine().process("fragments/resources", Set.of("cloudflare-web-analytics"), context);

		assertTrue(html.contains("https://static.cloudflareinsights.com/beacon.min.js"));
		assertTrue(html.contains("data-cf-beacon"));
		assertTrue(html.contains("test-token"));
		assertTrue(html.contains("&quot;spa&quot;:false") || html.contains("\"spa\":false"));
	}

	@Test
	void cloudflareFragmentDoesNotRenderBeaconWhenDisabled() {
		Context context = new Context();
		context.setVariable("cloudflareWebAnalytics",
				new CloudflareWebAnalyticsProperties(false, "test-token", false));

		String html = templateEngine().process("fragments/resources", Set.of("cloudflare-web-analytics"), context);

		assertFalse(html.contains("static.cloudflareinsights.com"));
	}

	@Test
	void commonResourcesIncludesCloudflareBeaconWhenConfigured() {
		Context context = new Context();
		context.setVariable("title", "Screen Carbon Test");
		context.setVariable("styles0", "assets/css/docs.css");
		context.setVariable("styles", "assets/css/index.css");
		context.setVariable("cloudflareWebAnalytics",
				new CloudflareWebAnalyticsProperties(true, "test-token", false));

		String html = templateEngine().process("fragments/resources", Set.of("common-resources"), context);

		assertTrue(html.contains("https://static.cloudflareinsights.com/beacon.min.js"));
		assertTrue(html.contains("test-token"));
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
