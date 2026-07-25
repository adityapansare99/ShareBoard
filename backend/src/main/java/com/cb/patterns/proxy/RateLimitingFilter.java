package com.cb.patterns.proxy;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Order(1)
public class RateLimitingFilter implements Filter {

    private final int capacity;
    private final int refillRate;
    private final long refillIntervalMs;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${planar.rate-limiter.capacity:10}") int capacity,
            @Value("${planar.rate-limiter.refill-rate:2}") int refillRate,
            @Value("${planar.rate-limiter.refill-interval-seconds:1}") int refillIntervalSec) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.refillIntervalMs = refillIntervalSec * 1000L;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip rate limiting for static resources and auth endpoints
        String path = httpRequest.getRequestURI();
        if (path.startsWith("/oauth2/") || path.startsWith("/login/") ||
            path.startsWith("/ws") || path.startsWith("/css/") || path.startsWith("/js/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpRequest);
        TokenBucket bucket = buckets.computeIfAbsent(clientIp,
            k -> new TokenBucket(capacity, refillRate, refillIntervalMs));

        if (!bucket.tryConsume()) {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"error\":\"RATE_LIMITED\",\"message\":\"Too many requests\",\"retryAfterMs\":"
                + refillIntervalMs + "}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
