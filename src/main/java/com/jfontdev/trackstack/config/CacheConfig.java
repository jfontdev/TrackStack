package com.jfontdev.trackstack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jfontdev.trackstack.dto.ai.AISuggestionResponseDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackPageResponseDTO;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import com.jfontdev.trackstack.dto.transition.TransitionResponseDTO;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration class for enabling Spring's caching abstraction with RedisJSON.
 * <p>
 * This configuration replaces the legacy {@code GenericJackson2JsonRedisSerializer}
 * approach (which stored JSON strings polluted with {@code @class} type annotations)
 * with a clean JSON serialization compatible with the RedisJSON module.
 * <p>
 * <b>Key improvements:</b>
 * <ul>
 *   <li>Values are stored as valid JSON objects without Jackson {@code @class} hacks.</li>
 *   <li>A lightweight {@code _type} envelope wraps each value, enabling deserialization
 *       without complex {@code PolymorphicTypeValidator} configuration.</li>
 *   <li>Data is compatible with RedisJSON's {@code JSON.SET} / {@code JSON.GET} commands
 *       and JSONPath queries.</li>
 * </ul>
 *
 * @see RedisJSONSerializer
 * @see com.jfontdev.trackstack.service.RedisJSONService
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures the default Redis cache configuration using RedisJSON-compatible
     * serialization.
     * <p>
     * All cacheable response DTOs must be registered in the type registry so that
     * the serializer can deserialize them correctly.
     *
     * @return the customized {@link RedisCacheConfiguration}
     */
    @Bean
    protected RedisCacheConfiguration cacheConfiguration() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Registry of all DTOs that may be stored in the cache.
        // The key is the simple class name used in the _type envelope.
        Map<String, Class<?>> typeRegistry = Map.of(
                "TrackResponseDTO", TrackResponseDTO.class,
                "TrackPageResponseDTO", TrackPageResponseDTO.class,
                "TransitionResponseDTO", TransitionResponseDTO.class,
                "SetlistResponseDTO", SetlistResponseDTO.class,
                "AISuggestionResponseDTO", AISuggestionResponseDTO.class);

        RedisJSONSerializer serializer = new RedisJSONSerializer(objectMapper, typeRegistry);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));
    }
}
