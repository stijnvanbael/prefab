package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import java.util.Objects;
import java.util.function.Function;
import org.apache.kafka.streams.kstream.KStream;

/** Kafka breakout adapter for injecting a native KStream fragment. */
public record KafkaStreamBreakoutAdapter<V, R>(
        Function<KStream<String, V>, KStream<String, R>> fragment
) implements StreamBreakoutAdapter<V, R, KStream<String, V>, KStream<String, R>> {

    public KafkaStreamBreakoutAdapter {
        Objects.requireNonNull(fragment, "fragment must not be null");
    }

    @Override
    public StreamBackend backend() {
        return StreamBackend.KAFKA;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<KStream<String, V>> nativeInputType() {
        return (Class<KStream<String, V>>) (Class<?>) KStream.class;
    }

    @Override
    public KStream<String, R> apply(KStream<String, V> nativeStream) {
        return fragment.apply(nativeStream);
    }
}

