package com.example.demo.docgen.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration to enable/disable caching based on application properties.
 * 
 * Uses Caffeine cache with configurable TTL for different cache stores:
 * - documentTemplates: YAML template definitions
 * - rawResources: Binary files (PDFs, FTLs)
 * 
 * Configuration properties:
 * - docgen.cache.document-templates.ttl-hours: TTL for template definitions (default: 24)
 * - docgen.cache.document-templates.max-size: Max entries (default: 500)
 * - docgen.cache.raw-resources.ttl-hours: TTL for binary files (default: 12)
 * - docgen.cache.raw-resources.max-size: Max entries (default: 200)
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "docgen.templates.cache-enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    @Value("${docgen.cache.document-templates.ttl-hours:24}")
    private int documentTemplatesTtlHours;

    @Value("${docgen.cache.document-templates.max-size:500}")
    private int documentTemplatesMaxSize;

    @Value("${docgen.cache.raw-resources.ttl-hours:12}")
    private int rawResourcesTtlHours;

    @Value("${docgen.cache.raw-resources.max-size:200}")
    private int rawResourcesMaxSize;

    /**
     * Create a Caffeine-based cache manager with TTL configuration.
     * Different caches have different expiry times based on content type.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Build the Caffeine cache with default settings
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .recordStats()  // Enable statistics for monitoring
                .maximumSize(1000)); // Max 1000 entries across caches
        
        // Register individual caches with their own TTL
        cacheManager.registerCustomCache("documentTemplates", 
            Caffeine.newBuilder()
                .recordStats()
                .maximumSize(documentTemplatesMaxSize)
                .expireAfterWrite(documentTemplatesTtlHours, TimeUnit.HOURS)
                .build());
        
        cacheManager.registerCustomCache("rawResources",
            Caffeine.newBuilder()
                .recordStats()
                .maximumSize(rawResourcesMaxSize)
                .expireAfterWrite(rawResourcesTtlHours, TimeUnit.HOURS)
                .build());
        
        return cacheManager;
    }
}


