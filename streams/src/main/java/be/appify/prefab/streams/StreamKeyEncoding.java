package be.appify.prefab.streams;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;

/**
 * Encodes stream keys using the shared wire shape: single-component records use their component as UTF-8,
 * while all other key types use JSON.
 *
 * @param <K> the key type
 */
public final class StreamKeyEncoding<K> {
    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final Class<K> keyType;
    private final RecordComponent component;
    private final Method accessor;
    private final Constructor<K> constructor;

    /**
     * Creates an encoder for the given key type.
     *
     * @param keyType the concrete key class
     */
    public StreamKeyEncoding(Class<K> keyType) {
        this.keyType = keyType;
        if (keyType.isRecord() && keyType.getRecordComponents().length == 1) {
            component = keyType.getRecordComponents()[0];
            accessor = component.getAccessor();
            constructor = constructorFor(keyType, component);
            if (!accessor.trySetAccessible() || !constructor.trySetAccessible()) {
                throw new IllegalArgumentException("Single-field key record must be accessible: " + keyType.getName());
            }
        } else {
            component = null;
            accessor = null;
            constructor = null;
        }
    }

    /**
     * Encodes a key, preserving the Kafka backend's existing byte representation.
     *
     * @param key the key
     * @return encoded bytes, or {@code null} for a null key
     */
    public byte[] encode(K key) {
        if (key == null) {
            return null;
        }
        if (component == null) {
            try {
                return MAPPER.writeValueAsBytes(key);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize key of type " + keyType.getName(), e);
            }
        }
        try {
            return String.valueOf(accessor.invoke(key)).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize single-field key " + keyType.getName(), e);
        }
    }

    /**
     * Decodes a key.
     *
     * @param bytes encoded key bytes
     * @return the key, or {@code null} for null bytes
     */
    public K decode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (component == null) {
            try {
                return MAPPER.readValue(bytes, keyType);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize key of type " + keyType.getName(), e);
            }
        }
        var raw = new String(bytes, StandardCharsets.UTF_8);
        var value = convertSingleField(raw, component.getType());
        try {
            return constructor.newInstance(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize single-field key " + keyType.getName(), e);
        }
    }

    private static <K> Constructor<K> constructorFor(Class<K> keyType, RecordComponent component) {
        try {
            return keyType.getDeclaredConstructor(component.getType());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No canonical constructor found for single-field key " + keyType.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object convertSingleField(String raw, Class<?> targetType) {
        if (targetType == String.class) {
            return raw;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(raw);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(raw);
        }
        if (targetType == Short.class || targetType == short.class) {
            return Short.parseShort(raw);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(raw);
        }
        if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(raw);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(raw);
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return Byte.parseByte(raw);
        }
        if (targetType == Character.class || targetType == char.class) {
            if (raw.length() != 1) {
                throw new IllegalArgumentException("Expected single character but got: " + raw);
            }
            return raw.charAt(0);
        }
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) targetType.asSubclass(Enum.class), raw);
        }
        try {
            Method valueOf = targetType.getMethod("valueOf", String.class);
            return valueOf.invoke(null, raw);
        } catch (NoSuchMethodException ignored) {
            // fall back to constructor path below
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert single-field key value to " + targetType.getName(), e);
        }
        try {
            Constructor<?> constructor = targetType.getDeclaredConstructor(String.class);
            return constructor.newInstance(raw);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unsupported single-field key component type " + targetType.getName() + ". "
                            + "Provide a String constructor or static valueOf(String).",
                    e);
        }
    }
}
