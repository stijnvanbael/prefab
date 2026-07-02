package be.appify.prefab.streams.kafka;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.function.Function;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.json.JsonMapper;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StringKeySerde<K> implements Serde<K> {
    private final StringKeySerializer<K> serializer;
    private final StringKeyDeserializer<K> deserializer;

    public StringKeySerde(Class<K> keyType) {
        var codec = codecFor(keyType);
        serializer = new StringKeySerializer<>(codec.serialize());
        deserializer = new StringKeyDeserializer<>(codec.deserialize());
    }

    @Override
    public Serializer<K> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<K> deserializer() {
        return deserializer;
    }

    private record KeyCodec<K>(Function<K, byte[]> serialize, Function<byte[], K> deserialize) {
    }

    private static <K> KeyCodec<K> codecFor(Class<K> keyType) {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        if (isSingleFieldRecord(keyType)) {
            return singleFieldRecordCodec(keyType);
        }
        return new KeyCodec<>(
                key -> {
                    try {
                        return mapper.writeValueAsBytes(key);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to serialize key of type " + keyType.getName(), e);
                    }
                },
                bytes -> {
                    try {
                        return mapper.readValue(bytes, keyType);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to deserialize key of type " + keyType.getName(), e);
                    }
                }
        );
    }

    private static <K> KeyCodec<K> singleFieldRecordCodec(Class<K> keyType) {
        var components = keyType.getRecordComponents();
        var component = components[0];
        var accessor = component.getAccessor();
        var constructor = constructorFor(keyType, component);
        return new KeyCodec<>(
                key -> {
                    try {
                        return String.valueOf(accessor.invoke(key)).getBytes(UTF_8);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to serialize single-field key " + keyType.getName(), e);
                    }
                },
                bytes -> {
                    var raw = new String(bytes, UTF_8);
                    var value = convertSingleField(raw, component.getType());
                    try {
                        return constructor.newInstance(value);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to deserialize single-field key " + keyType.getName(), e);
                    }
                }
        );
    }

    private static boolean isSingleFieldRecord(Class<?> keyType) {
        return keyType.isRecord() && keyType.getRecordComponents().length == 1;
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

    private record StringKeySerializer<K>(Function<K, byte[]> serializerFunction) implements Serializer<K> {

        @Override
        public byte[] serialize(String topic, K data) {
            if (data == null) {
                return null;
            }
            return serializerFunction.apply(data);
        }
    }

    private record StringKeyDeserializer<K>(Function<byte[], K> deserializerFunction) implements Deserializer<K> {

        @Override
        public K deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }
            return deserializerFunction.apply(data);
        }
    }
}
