package com.example.crm.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String DASHBOARD_CACHE = "dashboard_cache";
    public static final String PERSONAL_STATS_CACHE = "personal_stats_cache";

    @Bean("dashboardCache")
    @Primary
    public Cache<String, Object> dashboardCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(10)
                .build();
    }

    @Bean("personalStatsCache")
    public Cache<String, Object> personalStatsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }
}