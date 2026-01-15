package be.appify.prefab.test.kafka;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.serializer.JacksonJsonTypeResolver;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

class TestJsonTypeResolver {
    private final Map<String, Class<?>> types = new HashMap<>();
    private final JacksonJsonTypeResolver delegate;

    public TestJsonTypeResolver(JacksonJsonTypeResolver delegate) {
        this.delegate = delegate;
    }

    public void registerType(String topic, Class<?> type) {
        types.put(topic, type);
    }

    public JavaType resolveType(String topic, byte[] data, Headers headers) {
        var type = types.get(topic);
        if (type != null) {
            return TypeFactory.unsafeSimpleType(type);
        }
        return delegate.resolveType(topic, data, headers);
    }
}
