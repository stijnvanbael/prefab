package be.appify.prefab.streams.kafka;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * A {@link Serde} for key instances whose concrete type is unknown at topology-build time.
 *
 * <p>The serializer captures the key's runtime class from the first instance it encounters and uses
 * JSON serialization. The deserializer then uses that captured class to reconstruct keys via Jackson.
 *
 * <p>This is safe for state stores that only use {@code get}/{@code put}: Kafka Streams restores
 * persistent stores from their changelog by writing raw bytes directly into RocksDB, bypassing
 * the typed key serde entirely. The deserializer is only exercised during typed iteration
 * ({@code all()}, {@code range()}), which aggregation stores never perform.
 *
 * @param <K> the key type
 */
class DeferredJsonKeySerde<K> implements Serde<K> {

    private final AtomicReference<Class<K>> keyClass = new AtomicReference<>();
    private final JsonMapper mapper;

    DeferredJsonKeySerde(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Serializer<K> serializer() {
        return (topic, key) -> {
            if (key == null) {
                return null;
            }
            keyClass.compareAndSet(null, rawClass(key));
            try {
                return mapper.writeValueAsBytes(key);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to serialize key of type " + key.getClass().getName() + " for topic " + topic,
                        e);
            }
        };
    }

    @Override
    public Deserializer<K> deserializer() {
        return (topic, data) -> {
            if (data == null) {
                return null;
            }
            var resolved = keyClass.get();
            if (resolved == null) {
                throw new IllegalStateException(
                        "Key type not yet resolved for topic '" + topic + "': "
                        + "no key has been serialized through this serde yet. "
                        + "Ensure at least one record is written to the store before typed key "
                        + "deserialization is attempted (e.g. via store iteration).");
            }
            try {
                return mapper.readValue(data, resolved);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to deserialize key of type " + resolved.getName() + " from topic " + topic,
                        e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <K> Class<K> rawClass(K key) {
        return (Class<K>) key.getClass();
    }
}
