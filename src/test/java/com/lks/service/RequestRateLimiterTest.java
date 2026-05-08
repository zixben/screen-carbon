package com.lks.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestRateLimiterTest {

    @Test
    void tryAcquireBlocksAfterLimitAndAllowsAgainAfterWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-08T00:00:00Z"));
        RequestRateLimiter limiter = new RequestRateLimiter(clock);

        assertTrue(limiter.tryAcquire("login:203.0.113.10", 2, Duration.ofMinutes(1)));
        assertTrue(limiter.tryAcquire("login:203.0.113.10", 2, Duration.ofMinutes(1)));
        assertFalse(limiter.tryAcquire("login:203.0.113.10", 2, Duration.ofMinutes(1)));

        clock.advance(Duration.ofMinutes(1).plusMillis(1));

        assertTrue(limiter.tryAcquire("login:203.0.113.10", 2, Duration.ofMinutes(1)));
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
