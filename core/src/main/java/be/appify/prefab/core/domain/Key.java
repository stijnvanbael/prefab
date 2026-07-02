package be.appify.prefab.core.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public interface Key<K extends Key<K>> {
    Map<Class<? extends Key<?>>, Function<String, ? extends Key<?>>> KEY_TYPES = new ConcurrentHashMap<>();

    /**
     * Registers a deserialization function for a key type.
     *
     * @deprecated JSON serialization via {@link tools.jackson.databind.json.JsonMapper} is now the default.
     *             Key types no longer require manual {@code parse(String)} / {@code toString()} implementations.
     *             This method is kept for backward compatibility only and will be removed in a future release.
     */
    @Deprecated(since = "0.11.0", forRemoval = true)
    static <K extends Key<K>> void register(Class<? extends K> keyType, Function<String, K> deserializerFunction) {
        KEY_TYPES.put(keyType, deserializerFunction);
    }

    /**
     * Parses a string into a key of the given type using a registered deserializer function.
     *
     * @deprecated JSON serialization via {@link tools.jackson.databind.json.JsonMapper} is now the default.
     *             Key types no longer require manual {@code parse(String)} / {@code toString()} implementations.
     *             This method is kept for backward compatibility only and will be removed in a future release.
     */
    @Deprecated(since = "0.11.0", forRemoval = true)
    static <K extends Key<K>> K parse(String key, Class<K> keyType) {
        Function<String, ? extends Key<?>> deserializerFunction = KEY_TYPES.get(keyType);
        if (deserializerFunction == null) {
            throw new IllegalArgumentException("No deserializer function registered for key type: " + keyType.getName());
        }
        return keyType.cast(deserializerFunction.apply(key));
    }
}
