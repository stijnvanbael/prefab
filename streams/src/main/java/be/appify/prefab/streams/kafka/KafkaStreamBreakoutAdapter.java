package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import java.util.Objects;
import java.util.function.Function;
import org.apache.kafka.streams.kstream.KStream;

/** Kafka breakout adapter for injecting a native KStream fragment. */
public record KafkaStreamBreakoutAdapter<KI, KO, V extends Keyed<KI>, R extends Keyed<KO>>(
        Function<KStream<KI, V>, KStream<KO, R>> fragment
) implements StreamBreakoutAdapter<KI, V, KO, R, KStream<KI, V>, KStream<KO, R>> {

    public KafkaStreamBreakoutAdapter {
        Objects.requireNonNull(fragment, "fragment must not be null");
    }

    @Override
    public StreamBackend backend() {
        return StreamBackend.KAFKA;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<KStream<KI, V>> nativeInputType() {
        return (Class<KStream<KI, V>>) (Class<?>) KStream.class;
    }

    @Override
    public KStream<KO, R> apply(KStream<KI, V> nativeStream) {
        return fragment.apply(nativeStream);
    }
}
