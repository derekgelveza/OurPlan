package com.derekgelvez.user.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limits how often one client (IP address) can call each {@code /auth/**} endpoint, to slow down
 * password guessing, verification-code guessing and email spam. Over the limit the client gets
 * 429 with a {@code Retry-After} header.
 *
 * <p>Counts are kept in memory, so the limit is per server instance and resets on restart. Behind
 * a reverse proxy, configure {@code server.forward-headers-strategy} so the real client IP is used.
 *
 * <p>Not a {@code @Component}: {@link SecurityConfiguration} adds it to the security filter chain
 * only, so it doesn't also run as a second servlet filter.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int maxRequestsPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith(request.getContextPath() + "/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        // Bound memory: once many clients are tracked, drop the windows that have already ended.
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(window -> now - window.start >= WINDOW_MILLIS);
        }

        // One counter per client per endpoint, so /auth/login and /auth/resend have separate budgets.
        // Fixed one-minute windows: a new window starts when none exists or the previous one has ended.
        String key = request.getRemoteAddr() + '|' + request.getRequestURI();
        Window window = windows.compute(key, (k, existing) ->
                existing == null || now - existing.start >= WINDOW_MILLIS ? new Window(now) : existing);

        if (window.count.incrementAndGet() > maxRequestsPerMinute) {
            long retryAfterSeconds = Math.max(1, (window.start + WINDOW_MILLIS - now + 999) / 1000);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            ApiSecuritySupport.writeError(response, 429, "RATE_LIMITED", "Too many requests. Try again later.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Request count for one client/endpoint pair within one fixed one-minute window. */
    private static final class Window {
        private final long start;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long start) {
            this.start = start;
        }
    }
}
