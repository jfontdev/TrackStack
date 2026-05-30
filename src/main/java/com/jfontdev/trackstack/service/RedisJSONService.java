package com.jfontdev.trackstack.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jfontdev.trackstack.dto.track.TrackResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for querying cached data using JSONPath-like expressions.
 * <p>
 * RedisJSON stores values as native JSON objects (via the
 * {@link com.jfontdev.trackstack.config.RedisJSONSerializer} envelope).
 * This service provides a client-side JSONPath engine that:
 * <ul>
 *   <li>Scans cache keys matching a pattern</li>
 *   <li>Parses the JSON envelope to extract the {@code payload}</li>
 *   <li>Filters the payload using simple JSONPath predicates</li>
 * </ul>
 * <p>
 * <b>Why client-side instead of server-side JSONPath?</b>
 * Spring's {@code RedisCacheManager} stores values via the standard
 * {@code SET} command (String type), even when the payload is JSON.
 * To keep the Spring Cache abstraction working (with
 * {@code @Cacheable} / {@code @CacheEvict}), this service reads the
 * String values and evaluates JSONPath predicates in the JVM using
 * Jackson's {@link JsonNode} API. Future iterations can switch to
 * native {@code JSON.GET} commands if a custom cache manager is
 * implemented.
 * <p>
 * <b>Example usage:</b>
 * <pre>{@code
 * List<TrackResponseDTO> fastTracks = redisJSONService.queryCache(
 *         "tracks::*",
 *         "$.payload.bpm",
 *         JsonNode::isNumber,
 *         node -> node.asDouble() > 130,
 *         TrackResponseDTO.class);
 * }</pre>
 *
 * @see com.jfontdev.trackstack.config.CacheConfig
 * @see com.jfontdev.trackstack.config.RedisJSONSerializer
 */
@Service
public class RedisJSONService {

    private static final Logger log = LoggerFactory.getLogger(RedisJSONService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisJSONService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Queries cache entries matching a key pattern and filters by a JSONPath-like
     * predicate on the payload.
     * <p>
     * Steps:
     * <ol>
     *   <li>Scan Redis for keys matching {@code keyPattern}.</li>
     *   <li>For each key, read the cached JSON envelope.</li>
     *   <li>Extract the {@code payload} field from the envelope.</li>
     *   <li>Navigate to the field specified by {@code jsonPath} within the payload.</li>
     *   <li>Apply the type check and value predicate.</li>
     *   <li>Deserialize matching payloads into {@code targetType}.</li>
     * </ol>
     *
     * @param keyPattern    Redis key pattern (e.g., {@code "tracks::*"})
     * @param jsonPath      JSONPath expression pointing to the field to filter on
     *                      (e.g., {@code "$.payload.bpm"})
     * @param typeCheck     a check that the JSON node at the path is of the expected type
     * @param valuePredicate the filter condition applied to the JSON node value
     * @param targetType    the Java class to deserialize matching payloads into
     * @param <T>           the type of the returned objects
     * @return list of deserialized objects whose payload matched the predicate
     */
    public <T> List<T> queryCache(String keyPattern, String jsonPath,
                                   JsonNodeTypeCheck typeCheck,
                                   JsonNodePredicate valuePredicate,
                                   Class<T> targetType) {
        log.debug("Querying cache with pattern: {}, jsonPath: {}", keyPattern, jsonPath);

        Set<String> keys = stringRedisTemplate.keys(keyPattern);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        List<T> results = new ArrayList<>();
        for (String key : keys) {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                continue;
            }

            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode payload = root.path("payload");
                if (payload.isMissingNode()) {
                    continue;
                }

                JsonNode valueNode = navigateJsonPath(payload, jsonPath);
                if (valueNode == null || valueNode.isMissingNode()) {
                    continue;
                }

                if (typeCheck.matches(valueNode) && valuePredicate.test(valueNode)) {
                    T deserialized = objectMapper.treeToValue(payload, targetType);
                    results.add(deserialized);
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse cached JSON for key {}: {}", key, e.getMessage());
            }
        }

        log.debug("Query returned {} results", results.size());
        return results;
    }

    /**
     * Queries the tracks cache for tracks whose BPM is within a given range.
     * <p>
     * Convenience method that demonstrates JSONPath-like querying on the
     * {@code tracks} cache.
     *
     * @param minBpm minimum BPM (inclusive), or {@code null} for no lower bound
     * @param maxBpm maximum BPM (inclusive), or {@code null} for no upper bound
     * @return list of matching tracks
     */
    public List<TrackResponseDTO> findTracksByBpmRange(Double minBpm, Double maxBpm) {
        return queryCache(
                "tracks::*",
                "$.bpm",
                JsonNode::isNumber,
                node -> {
                    double bpm = node.asDouble();
                    boolean aboveMin = minBpm == null || bpm >= minBpm;
                    boolean belowMax = maxBpm == null || bpm <= maxBpm;
                    return aboveMin && belowMax;
                },
                TrackResponseDTO.class);
    }

    /**
     * Navigates a JSONPath expression on a {@link JsonNode}.
     * <p>
     * Supports simple dot-notation paths like {@code "$.bpm"} or
     * {@code "$.payload.energy"}. Array indexing is not supported.
     *
     * @param root    the root JSON node to navigate from
     * @param jsonPath the JSONPath expression (must start with {@code $})
     * @return the node at the given path, or a missing node if not found
     */
    private JsonNode navigateJsonPath(JsonNode root, String jsonPath) {
        if (!jsonPath.startsWith("$")) {
            throw new IllegalArgumentException("JSONPath must start with $");
        }

        String path = jsonPath.substring(1); // Remove leading $
        if (path.isEmpty() || path.equals(".")) {
            return root;
        }

        JsonNode current = root;
        String[] segments = path.split("\\.");
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            current = current.path(segment);
            if (current.isMissingNode()) {
                return current;
            }
        }
        return current;
    }

    /**
     * Functional interface for checking the type of a {@link JsonNode}.
     */
    @FunctionalInterface
    public interface JsonNodeTypeCheck {
        boolean matches(JsonNode node);
    }

    /**
     * Functional interface for applying a predicate to a {@link JsonNode} value.
     */
    @FunctionalInterface
    public interface JsonNodePredicate {
        boolean test(JsonNode node);
    }
}
