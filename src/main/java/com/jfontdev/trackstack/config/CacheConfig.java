package com.jfontdev.trackstack.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for enabling Spring's caching abstraction.
 * <p>
 * By placing {@link EnableCaching} in a dedicated configuration class rather
 * than
 * the main application class, we ensure that caching is not a mandatory feature
 * across all environments (e.g., it can be easily disabled or overridden during
 * testing).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
