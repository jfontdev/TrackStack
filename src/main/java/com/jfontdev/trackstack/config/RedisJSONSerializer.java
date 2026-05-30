package com.jfontdev.trackstack.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Redis serializer that stores values as clean JSON objects
 * compatible with the RedisJSON module.
 * <p>
 * Unlike {@code GenericJackson2JsonRedisSerializer}, this serializer does
 * not pollute the JSON with Jackson's {@code @class} type annotations.
 * Instead, it wraps each value in a lightweight envelope:
 *
 * <pre>{@code
 * {
 *   "_type": "TrackResponseDTO",
 *   "payload": { ...actual DTO fields... }
 * }
 * }</pre>
 *
 * <p>
 * The {@code _type} field contains the <b>simple class name</b> (not the
 * fully-qualified name), making the JSON readable and queryable. The
 * {@code payload} field holds the actual serialized DTO. On deserialization,
 * the type name is resolved against a registry of known classes.
 * <p>
 * <b>Why this matters for RedisJSON:</b>
 * <ul>
 *   <li>The value is valid JSON that can be stored natively by RedisJSON
 *       (using {@code JSON.SET}) rather than as an opaque string.</li>
 *   <li>JSONPath queries work on the payload without seeing Jackson
 *       internals: {@code $.payload.bpm} instead of {@code $['@class']}.</li>
 *   <li>No {@link com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator}
 *       configuration is needed.</li>
 * </ul>
 *
 * @see CacheConfig
 * @see com.jfontdev.trackstack.service.RedisJSONService
 */
public class RedisJSONSerializer implements RedisSerializer<Object> {

    private static final String TYPE_FIELD = "_type";
    private static final String PAYLOAD_FIELD = "payload";

    private final ObjectMapper objectMapper;
    private final Map<String, Class<?>> typeRegistry;

    /**
     * Constructs a new serializer with the given ObjectMapper and type registry.
     *
     * @param objectMapper  the Jackson ObjectMapper for JSON conversion
     * @param typeRegistry  a map from simple class names to their {@code Class} objects
     */
    public RedisJSONSerializer(ObjectMapper objectMapper, Map<String, Class<?>> typeRegistry) {
        this.objectMapper = objectMapper;
        this.typeRegistry = new HashMap<>(typeRegistry);
    }

    /**
     * Serializes the given object into a JSON envelope.
     * <p>
     * The result is a JSON object with {@code _type} and {@code payload} fields.
     *
     * @param object the object to serialize
     * @return the JSON envelope as UTF-8 bytes
     * @throws SerializationException if JSON conversion fails
     */
    @Override
    public byte[] serialize(Object object) throws SerializationException {
        if (object == null) {
            return new byte[0];
        }

        try {
            String typeName = object.getClass().getSimpleName();
            Map<String, Object> envelope = new HashMap<>();
            envelope.put(TYPE_FIELD, typeName);
            envelope.put(PAYLOAD_FIELD, object);

            String json = objectMapper.writeValueAsString(envelope);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize object of type "
                    + object.getClass().getName(), e);
        }
    }

    /**
     * Deserializes the given JSON envelope back into the original Java object.
     * <p>
     * Reads the {@code _type} field, looks up the corresponding class in the
     * registry, and deserializes the {@code payload} field into that class.
     *
     * @param bytes the JSON envelope as UTF-8 bytes
     * @return the deserialized Java object
     * @throws SerializationException if JSON conversion or type lookup fails
     */
    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            String json = new String(bytes, StandardCharsets.UTF_8);
            Map<String, Object> envelope = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));

            String typeName = (String) envelope.get(TYPE_FIELD);
            if (typeName == null) {
                throw new SerializationException("JSON envelope missing '" + TYPE_FIELD + "' field");
            }

            Class<?> targetClass = typeRegistry.get(typeName);
            if (targetClass == null) {
                throw new SerializationException("Unknown type name in envelope: " + typeName
                        + ". Registered types: " + typeRegistry.keySet());
            }

            Object payload = envelope.get(PAYLOAD_FIELD);
            // Re-serialize payload to JSON then deserialize into target type
            // This is more reliable than casting a LinkedHashMap
            String payloadJson = objectMapper.writeValueAsString(payload);
            return objectMapper.readValue(payloadJson, targetClass);
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize JSON envelope", e);
        }
    }
}
