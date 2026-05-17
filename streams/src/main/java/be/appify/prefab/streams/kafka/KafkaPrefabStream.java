package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.StreamDefinition;
import java.util.List;
import java.util.stream.IntStream;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Branched;

/**
 * Kafka-backed implementation of {@link PrefabStream}.
 *
 * <p>Each operator delegates to the corresponding native {@link KStream} method, preserving
 * Prefab's serialization infrastructure for the terminal {@code to} operations.
 *
 * @param <V> current record value type
 */
public class KafkaPrefabStream<V> implements PrefabStream<V> {
    private final StreamsBuilder streamsBuilder;
    private final KStream<String, V> stream;
    private final KafkaTopicResolver topicResolver;
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;

    /** Constructs a new KafkaPrefabStream. */
    public KafkaPrefabStream(
            StreamsBuilder streamsBuilder,
            KStream<String, V> stream,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer
    ) {
        this.streamsBuilder = streamsBuilder;
        this.stream = stream;
        this.topicResolver = topicResolver;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public PrefabStream<V> filter(Predicate<V> predicate) {
        return wrap(stream.filter((key, value) -> predicate.test(value)));
    }

    @Override
    public <R> PrefabStream<R> map(Function<V, R> mapper) {
        return wrap(stream.mapValues(mapper::apply));
    }

    @Override
    public <R> PrefabStream<R> flatMap(Function<V, Iterable<R>> mapper) {
        return wrap(stream.flatMapValues(mapper::apply));
    }

    @Override
    @SafeVarargs
    public final List<PrefabStream<V>> branch(Predicate<V>... predicates) {
        var branchPrefix = "prefab-branch-";
        var branched = stream.split(Named.as(branchPrefix));
        for (var index = 0; index < predicates.length; index++) {
            var predicate = predicates[index];
            branched.branch((key, value) -> predicate.test(value), Branched.as(Integer.toString(index)));
        }
        var namedBranches = branched.noDefaultBranch();
        return IntStream.range(0, predicates.length)
                .mapToObj(index -> namedBranches.get(branchPrefix + index))
                .map(this::wrap)
                .map(branch -> (PrefabStream<V>) branch)
                .toList();
    }

    @Override
    public PrefabStream<V> merge(PrefabStream<V> other) {
        if (!(other instanceof KafkaPrefabStream<?> otherKafkaStream)) {
            throw new IllegalArgumentException("Cannot merge non-Kafka stream implementation");
        }
        @SuppressWarnings("unchecked")
        var otherTypedStream = (KStream<String, V>) otherKafkaStream.stream;
        return wrap(stream.merge(otherTypedStream));
    }

    @Override
    public StreamDefinition to(Class<?> type) {
        return to(topicResolver.topicForType(type));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StreamDefinition to(String topic) {
        // DynamicSerializer/Deserializer work on Object at runtime; the cast to Serde<V> is safe
        // because the backend selects serialization by topic, not by the generic type parameter.
        var valueSerde = (Serde<V>) new SerdeAdapter<>(serializer, deserializer);
        stream.to(topic, Produced.with(Serdes.String(), valueSerde));
        return new StreamDefinition(streamsBuilder::build);
    }

    private <R> KafkaPrefabStream<R> wrap(KStream<String, R> transformed) {
        return new KafkaPrefabStream<>(streamsBuilder, transformed, topicResolver, serializer, deserializer);
    }
}
