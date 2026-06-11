package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Key;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A {@link Serde} for {@link Key} instances whose concrete type is unknown at topology-build time.
 *
 * <p>The serializer captures the key's runtime class from the first instance it encounters.
 * The deserializer then uses that captured class to reconstruct keys via {@link Key#parse}.
 *
 * <p>This is safe for state stores that only use {@code get}/{@code put}: Kafka Streams restores
 * persistent stores from their changelog by writing raw bytes directly into RocksDB, bypassing
 * the typed key serde entirely. The deserializer is only exercised during typed iteration
 * ({@code all()}, {@code range()}), which aggregation stores never perform.
 *
 * @param <K> the key type
 */
class DeferredStringKeySerde<K extends Key<K>> implements Serde<K> {

    private final AtomicReference<Class<K>> keyClass = new AtomicReference<>();

    @Override
    public Serializer<K> serializer() {
        return (topic, key) -> {
            if (key == null) {
                return null;
            }
            keyClass.compareAndSet(null, rawClass(key));
            return key.toString().getBytes(UTF_8);
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
            return Key.parse(new String(data, UTF_8), resolved);
        };
    }

    @SuppressWarnings("unchecked")
    private static <K extends Key<K>> Class<K> rawClass(K key) {
        return (Class<K>) key.getClass();
    }
}


