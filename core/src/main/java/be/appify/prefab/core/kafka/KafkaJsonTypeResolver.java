package be.appify.prefab.core.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.serializer.JsonTypeResolver;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * KafkaJsonTypeResolver resolves Java types for Kafka topics based on a registered mapping.
 */
@Component
public class KafkaJsonTypeResolver implements JsonTypeResolver {
    private final Map<String, Class<?>> types = new HashMap<>();

    /** Constructs a new KafkaJsonTypeResolver. */
    public KafkaJsonTypeResolver() {
    }

    @Override
    public JavaType resolveType(String topic, byte[] data, Headers headers) {
        if (types.containsKey(topic)) {
            return TypeFactory.defaultInstance().constructType(types.get(topic));
        } else {
            throw new IllegalArgumentException("No type registered for topic: " + topic);
        }
    }

    /**
     * Registers a Java type for a specific Kafka topic.
     *
     * @param topic the Kafka topic
     * @param type  the Java class type to register
     */
    public void registerType(String topic, Class<?> type) {
        types.put(topic, type);
    }
}
