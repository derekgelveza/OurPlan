package com.derekgelvez.user.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory account throttle for the MVP. The existing IP limiter remains useful, while
 * this prevents a distributed attacker from making unlimited guesses against one account.
 * Replace the store with Redis or a shared database before running multiple user instances.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_EMAILS = 100_000;

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        AttemptWindow window = attempts.get(email);
        if (window == null) {
            return false;
        }
        if (window.isExpired()) {
            attempts.remove(email, window);
            return false;
        }
        return window.failures >= MAX_FAILURES;
    }

    public void recordFailure(String email) {
        cleanupIfNeeded();
        attempts.compute(email, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new AttemptWindow();
            }
            existing.failures++;
            return existing;
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(email);
    }

    private void cleanupIfNeeded() {
        if (attempts.size() > MAX_TRACKED_EMAILS) {
            attempts.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    private static final class AttemptWindow {
        private final Instant started = Instant.now();
        private int failures = 1;

        private boolean isExpired() {
            return started.plus(WINDOW).isBefore(Instant.now());
        }
    }
}
