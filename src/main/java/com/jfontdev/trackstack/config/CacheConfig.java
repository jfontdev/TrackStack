package com.jfontdev.trackstack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuration class for enabling Spring's caching abstraction with Redis.
 * <p>
 * By placing {@link EnableCaching} in a dedicated configuration class rather
 * than the main application class, we ensure that caching is not a mandatory
 * feature
 * across all environments (e.g., it can be easily disabled or overridden during
 * testing).
 * <p>
 * This configuration sets up Redis as the cache provider, using JSON
 * serialization
 * for cache values to make them human-readable in Redis clients, and sets a
 * default
 * Time-To-Live (TTL) for cache entries.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures the default Redis cache configuration.
     * <p>
     * This bean customizes how data is stored in Redis:
     * <ul>
     * <li>Keys are serialized as plain strings.</li>
     * <li>Values are serialized as JSON (using Jackson) instead of JDK binary
     * serialization.
     * This makes debugging and inspecting the cache much easier.</li>
     * <li>A default Time-To-Live (TTL) of 10 minutes is applied to all cache
     * entries
     * to ensure stale data is eventually evicted.</li>
     * <li>Null values are not cached to prevent caching empty results.</li>
     * </ul>
     *
     * @return the customized {@link RedisCacheConfiguration}
     */
    @Bean
    protected RedisCacheConfiguration cacheConfiguration() {
        // Create a custom ObjectMapper for JSON serialization that can handle Java 8
        // date/time types and includes type information for polymorphic
        // deserialization.
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // We need to configure Jackson to include type information in the JSON.
        // Without this, Redis stores a plain JSON array/object, and when Spring tries
        // to read it back, it doesn't know what Java class to instantiate (especially
        // for Collections like List or generic wrappers).
        // To avoid insecure deserialization, we explicitly restrict which packages
        // are allowed for polymorphic deserialization instead of allowing all types.
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.util.")
                .allowIfSubType("com.jfontdev.trackstack.dto.")
                .build();

        /**
         * activateDefaultTyping tells Jackson to embed the class name into the JSON.
         * We use EVERYTHING so that even Collections (like List) and wrapper types
         * (like Long) get their type information saved.
         * We use As.PROPERTY to store the type info as a JSON property ("@class":"...")
         * instead of wrapping the object in a JSON array, which is crucial for Java
         * Records to deserialize correctly.
         */
        objectMapper.activateDefaultTypingAsProperty(ptv, ObjectMapper.DefaultTyping.EVERYTHING, "@class");

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));
    }
}
