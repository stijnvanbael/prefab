package be.appify.prefab.core.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public interface Key<K extends Key<K>> {
    Map<Class<? extends Key<?>>, Function<String, ? extends Key<?>>> KEY_TYPES = new ConcurrentHashMap<>();

    static <K extends Key<K>> void register(Class<? extends K> keyType, Function<String, K> deserializerFunction) {
        KEY_TYPES.put(keyType, deserializerFunction);
    }

    static <K extends Key<K>> K parse(String key, Class<K> keyType) {
        Function<String, ? extends Key<?>> deserializerFunction = KEY_TYPES.get(keyType);
        if (deserializerFunction == null) {
            throw new IllegalArgumentException("No deserializer function registered for key type: " + keyType.getName());
        }
        return keyType.cast(deserializerFunction.apply(key));
    }
}
