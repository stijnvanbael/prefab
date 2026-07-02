package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.Aggregation;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * A {@link Serde} for {@link Aggregation} instances whose concrete key and value types are
 * unknown at topology-build time.
 *
 * <p>Jackson can always <em>serialise</em> a concrete object regardless of type erasure, but
 * deserialisation into a raw {@code Aggregation.class} loses the type arguments, making field
 * mapping fail. This serde defers the Jackson {@link JavaType} construction until the first
 * {@code Aggregation} is serialised, at which point the concrete key class (from
 * {@link Aggregation#key()}) and value class (from the first element of
 * {@link Aggregation#values()}) are captured and used to build the full parameterised type.
 *
 * <p>This is safe for state stores that only use {@code get}/{@code put}: Kafka Streams restores
 * persistent stores from their changelog by writing raw bytes directly into RocksDB, bypassing
 * the typed value serde. The deserialiser is called on the second (and later) {@code get} for the
 * same key, by which time the type has already been captured through the preceding {@code put}.
 *
 * @param <K> the grouping key type
 * @param <V> the accumulated value type
 */
class DeferredAggregationSerde<K, V> implements Serde<Aggregation<K, V>> {

    private final AtomicReference<JavaType> capturedType = new AtomicReference<>();
    private final JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Override
    public Serializer<Aggregation<K, V>> serializer() {
        return (topic, aggregation) -> {
            if (aggregation == null) {
                return null;
            }
            captureTypeIfAbsent(aggregation);
            try {
                return objectMapper.writeValueAsBytes(aggregation);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize Aggregation for topic " + topic, e);
            }
        };
    }

    @Override
    public Deserializer<Aggregation<K, V>> deserializer() {
        return (topic, data) -> {
            if (data == null) {
                return null;
            }
            var type = capturedType.get();
            if (type == null) {
                throw new IllegalStateException(
                        "Aggregation type not yet resolved for topic '" + topic + "': "
                        + "no Aggregation has been serialized yet through this serde. "
                        + "Ensure at least one record is written to the store before typed "
                        + "deserialization is attempted (e.g. via store iteration).");
            }
            try {
                return objectMapper.readValue(data, type);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize Aggregation from topic " + topic, e);
            }
        };
    }

    /**
     * Captures the full parameterised {@link JavaType} from the first non-empty {@link Aggregation}.
     * Uses the concrete key class and the concrete class of the first accumulated value to build
     * {@code Aggregation<K, V>} so Jackson can reconstruct both fields correctly.
     */
    private void captureTypeIfAbsent(Aggregation<K, V> aggregation) {
        if (capturedType.get() != null || aggregation.values().isEmpty()) {
            return;
        }
        @SuppressWarnings("unchecked")
        var keyClass = (Class<K>) aggregation.key().getClass();
        @SuppressWarnings("unchecked")
        var valueClass = (Class<V>) aggregation.values().getFirst().getClass();
        var type = objectMapper.getTypeFactory()
                .constructParametricType(Aggregation.class, keyClass, valueClass);
        capturedType.compareAndSet(null, type);
    }
}

