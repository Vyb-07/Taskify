package com.taskify.taskify.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${rate.limit.auth.capacity}")
    private int authCapacity;

    @Value("${rate.limit.auth.refill.tokens}")
    private int authRefillTokens;

    @Value("${rate.limit.auth.refill.seconds}")
    private int authRefillSeconds;

    @Value("${rate.limit.api.capacity}")
    private int apiCapacity;

    @Value("${rate.limit.api.refill.tokens}")
    private int apiRefillTokens;

    @Value("${rate.limit.api.refill.seconds}")
    private int apiRefillSeconds;

    public Bucket resolveBucket(String key, boolean isAuthEndpoint) {
        return buckets.computeIfAbsent(key, k -> createNewBucket(isAuthEndpoint));
    }

    public void clearBuckets() {
        buckets.clear();
    }

    private Bucket createNewBucket(boolean isAuthEndpoint) {
        int capacity = isAuthEndpoint ? authCapacity : apiCapacity;
        int refillTokens = isAuthEndpoint ? authRefillTokens : apiRefillTokens;
        int refillSeconds = isAuthEndpoint ? authRefillSeconds : apiRefillSeconds;

        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(refillTokens, Duration.ofSeconds(refillSeconds))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
