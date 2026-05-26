package com.tasktracker.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

    private final boolean enabled;
    private final int maxAttempts;
    private final long windowMillis;
    private final long blockMillis;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            @Value("${app.security.auth-rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.auth-rate-limit.max-attempts:20}") int maxAttempts,
            @Value("${app.security.auth-rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${app.security.auth-rate-limit.block-seconds:900}") long blockSeconds
    ) {
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowSeconds * 1000L;
        this.blockMillis = blockSeconds * 1000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || !HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.equals(AUTH_PATH_PREFIX + "login")
                || path.equals(AUTH_PATH_PREFIX + "register")
                || path.equals(AUTH_PATH_PREFIX + "refresh")
                || path.equals(AUTH_PATH_PREFIX + "oauth/exchange"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getRequestURI() + ":" + clientIp(request);
        long now = Instant.now().toEpochMilli();
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(now));
        if (!bucket.allow(now, maxAttempts, windowMillis, blockMillis)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many authentication attempts\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Bucket {
        private long windowStartedAt;
        private long blockedUntil;
        private int attempts;

        private Bucket(long now) {
            this.windowStartedAt = now;
        }

        private synchronized boolean allow(long now, int maxAttempts, long windowMillis, long blockMillis) {
            if (blockedUntil > now) {
                return false;
            }
            if (now - windowStartedAt > windowMillis) {
                windowStartedAt = now;
                attempts = 0;
            }
            attempts++;
            if (attempts > maxAttempts) {
                blockedUntil = now + blockMillis;
                return false;
            }
            return true;
        }
    }
}
