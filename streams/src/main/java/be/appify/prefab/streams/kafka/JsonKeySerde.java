package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Key;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON-based {@link Serde} for {@link Key} types.
 *
 * <p>Serializes keys to JSON using Jackson; deserializes JSON back to strongly-typed key instances.
 * This eliminates the need for manual {@code parse(String)} and {@code toString()} implementations.
 *
 * @param <K> the key type
 */
public class JsonKeySerde<K extends Key<K>> implements Serde<K> {
    private final JsonKeySerializer<K> serializer;
    private final JsonKeyDeserializer<K> deserializer;

    /**
     * Constructs a new JsonKeySerde for the given key type.
     *
     * @param keyType the concrete key class
     * @param mapper  the Jackson JsonMapper to use for serialization/deserialization
     */
    public JsonKeySerde(Class<K> keyType, JsonMapper mapper) {
        this.serializer = new JsonKeySerializer<>(mapper);
        this.deserializer = new JsonKeyDeserializer<>(keyType, mapper);
    }

    @Override
    public Serializer<K> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<K> deserializer() {
        return deserializer;
    }

    private static class JsonKeySerializer<K> implements Serializer<K> {
        private final JsonMapper mapper;

        JsonKeySerializer(JsonMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public byte[] serialize(String topic, K data) {
            if (data == null) {
                return null;
            }
            try {
                return mapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to serialize key of type " + data.getClass().getName() + " for topic " + topic,
                        e);
            }
        }
    }

    private record JsonKeyDeserializer<K>(Class<K> keyType, JsonMapper mapper) implements Deserializer<K> {

        @Override
        public K deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }
            try {
                return mapper.readValue(data, keyType);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to deserialize key of type " + keyType.getName() + " from topic " + topic,
                        e);
            }
        }
    }
}

