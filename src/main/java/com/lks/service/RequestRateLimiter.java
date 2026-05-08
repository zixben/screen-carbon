package com.lks.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RequestRateLimiter {
    private final Clock clock;
    private final ConcurrentMap<String, Deque<Long>> attemptsByKey = new ConcurrentHashMap<>();

    public RequestRateLimiter() {
        this(Clock.systemUTC());
    }

    RequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Rate limit key is required.");
        }
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("Rate limit must be positive.");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limit window must be positive.");
        }

        long now = clock.millis();
        long cutoff = now - window.toMillis();
        Deque<Long> attempts = attemptsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst() <= cutoff) {
                attempts.removeFirst();
            }
            if (attempts.size() >= maxRequests) {
                return false;
            }
            attempts.addLast(now);
            return true;
        }
    }
}
