package be.appify.prefab.processor.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.serializer.JsonTypeResolver;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class KafkaJsonTypeResolver implements JsonTypeResolver {
    private final Map<String, Class<?>> types = new HashMap<>();

    @Override
    public JavaType resolveType(String topic, byte[] data, Headers headers) {
        if (types.containsKey(topic)) {
            return TypeFactory.defaultInstance().constructType(types.get(topic));
        } else {
            throw new IllegalArgumentException("No type registered for topic: " + topic);
        }
    }

    public void registerType(String topic, Class<?> type) {
        types.put(topic, type);
    }
}
