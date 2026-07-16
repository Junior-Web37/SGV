package com.sgv.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Desktop-only cache configuration.
 * Uses a simple in-memory ConcurrentMapCacheManager for desktop mode.
 */
@Configuration
public class CacheConfig {

    public static final String DASHBOARD_STATS_CACHE = "dashboardStats";
    public static final String PRODUCTS_CACHE = "products";
    public static final String BRANCHES_CACHE = "branches";
    public static final String SALE_SUMMARY_CACHE = "saleSummary";
    public static final String LOW_STOCK_CACHE = "lowStock";

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple", matchIfMissing = true)
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            DASHBOARD_STATS_CACHE,
            PRODUCTS_CACHE,
            BRANCHES_CACHE,
            SALE_SUMMARY_CACHE,
            LOW_STOCK_CACHE
        );
    }
}
