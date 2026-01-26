package com.taskify.taskify.security;

import com.taskify.taskify.service.RateLimitService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final MeterRegistry meterRegistry;

    public RateLimitFilter(RateLimitService rateLimitService, MeterRegistry meterRegistry) {
        this.rateLimitService = rateLimitService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Skip excluded paths
        if (isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Identify key and type
        boolean isAuthEndpoint = path.startsWith("/api/v1/auth");
        String key = resolveKey(request);

        // 3. Resolve bucket and consume
        Bucket bucket = rateLimitService.resolveBucket(key, isAuthEndpoint);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            meterRegistry.counter("taskify.rate_limit.rejections").increment();
            handleRateLimitExceeded(response, probe);
        }
    }

    private boolean isExcluded(String path) {
        return path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/actuator/health") ||
                path.equals("/favicon.ico");
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return "user:" + auth.getName();
        }
        return "ip:" + getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private void handleRateLimitExceeded(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
        if (waitForRefill < 1)
            waitForRefill = 1;

        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(waitForRefill));
        response.getWriter().write("{ \"message\": \"Too many requests. Please try again later.\", \"status\": 429 }");
    }
}
