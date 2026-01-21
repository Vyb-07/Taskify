package com.taskify.taskify.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

@Component("taskCacheKeyGenerator")
public class TaskCacheKeyGenerator implements KeyGenerator {

    private final CacheManager cacheManager;

    public TaskCacheKeyGenerator(@Lazy CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Object generate(Object target, Method method, Object... params) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : "anonymous";

        String roles = (authentication != null) ? authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.joining(",")) : "";

        // Versions for surgical invalidation of lists
        String version = "0";
        Cache versionCache = cacheManager.getCache("taskVersions");
        if (versionCache != null && authentication != null) {
            version = versionCache.get(username, String.class);
            if (version == null) {
                version = "0";
                versionCache.put(username, version);
            }
        }

        StringBuilder key = new StringBuilder();
        key.append(username).append(":");
        key.append(roles).append(":");
        key.append(version).append(":");
        key.append(method.getName()).append(":");

        for (Object param : params) {
            if (param != null) {
                key.append(param.toString()).append("|");
            } else {
                key.append("null|");
            }
        }

        return key.toString();
    }
}
