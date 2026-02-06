package com.taskify.taskify.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

        @Value("${app.cache.tasks.status.ttl:10}")
        private int tasksStatusTtl;

        @Value("${app.cache.tasks.status.max-size:500}")
        private int tasksStatusMaxSize;

        @Value("${app.cache.tasks.details.ttl:30}")
        private int tasksDetailsTtl;

        @Value("${app.cache.tasks.details.max-size:1000}")
        private int tasksDetailsMaxSize;

        @Value("${app.cache.tasks.versions.ttl:60}")
        private int tasksVersionsTtl;

        @Bean
        public CacheManager cacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                                "tasks", "taskDetails", "taskVersions", "intentOverview");

                cacheManager.registerCustomCache("tasks", Caffeine.newBuilder()
                                .expireAfterWrite(tasksStatusTtl, TimeUnit.MINUTES)
                                .maximumSize(tasksStatusMaxSize)
                                .recordStats()
                                .<Object, Object>build());

                cacheManager.registerCustomCache("taskDetails", Caffeine.newBuilder()
                                .expireAfterWrite(tasksDetailsTtl, TimeUnit.MINUTES)
                                .maximumSize(tasksDetailsMaxSize)
                                .recordStats()
                                .<Object, Object>build());

                cacheManager.registerCustomCache("taskVersions", Caffeine.newBuilder()
                                .expireAfterWrite(tasksVersionsTtl, TimeUnit.MINUTES)
                                .maximumSize(10000) // One per active user
                                .<Object, Object>build());

                cacheManager.registerCustomCache("weeklyReview", Caffeine.newBuilder()
                                .expireAfterWrite(1, TimeUnit.HOURS)
                                .maximumSize(5000)
                                .<Object, Object>build());

                cacheManager.registerCustomCache("dailyCheckIn", Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .maximumSize(5000)
                                .<Object, Object>build());

                cacheManager.registerCustomCache("intentOverview", Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .maximumSize(5000)
                                .<Object, Object>build());

                return cacheManager;
        }
}
