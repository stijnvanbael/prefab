package be.appify.prefab.test.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.serializer.JsonTypeResolver;

import java.util.HashMap;
import java.util.Map;

public class TestJsonTypeResolver {
    private final Map<String, Class<?>> types = new HashMap<>();
    private final JsonTypeResolver delegate;

    public TestJsonTypeResolver() {
        this((topic, data, headers) -> {
            throw new IllegalArgumentException("No type resolver configured for topic " + topic);
        });
    }

    public TestJsonTypeResolver(JsonTypeResolver delegate) {
        this.delegate = delegate;
    }

    public void registerType(String topic, Class<?> type) {
        types.put(topic, type);
    }

    public JavaType resolveType(String topic, byte[] data, Headers headers) {
        var type = types.get(topic);
        if (type != null) {
            return TypeFactory.defaultInstance().constructType(type);
        }
        return delegate.resolveType(topic, data, headers);
    }
}
