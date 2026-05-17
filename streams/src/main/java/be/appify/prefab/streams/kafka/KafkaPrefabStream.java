package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.streams.StreamDefinition;
import java.util.List;
import java.util.Objects;
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
    private final Class<?> valueType;

    /** Constructs a new KafkaPrefabStream. */
    public KafkaPrefabStream(
            StreamsBuilder streamsBuilder,
            KStream<String, V> stream,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer,
            Class<?> valueType
    ) {
        this.streamsBuilder = streamsBuilder;
        this.stream = stream;
        this.topicResolver = topicResolver;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.valueType = valueType;
    }

    @Override
    public PrefabStream<V> filter(Predicate<V> predicate) {
        return wrap(stream.filter((key, value) -> predicate.test(value)), valueType);
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
    public PrefabStream<V> branch(Predicate<V> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        var branchPrefix = "prefab-branch-";
        var branched = stream.split(Named.as(branchPrefix))
                .branch((key, value) -> predicate.test(value), Branched.as("matched"));
        var namedBranches = branched.noDefaultBranch();
        return wrap(namedBranches.get(branchPrefix + "matched"), valueType);
    }

    @Override
    public <S extends V> PrefabStream<S> branch(Class<S> subtype) {
        Objects.requireNonNull(subtype, "subtype must not be null");
        var filteredAndCasted = stream
                .filter((key, value) -> subtype.isInstance(value))
                .mapValues(subtype::cast);
        return wrap(filteredAndCasted, subtype);
    }

    @Override
    public <S> PrefabStream<S> merge(PrefabStream<? extends S> other) {
        if (!(other instanceof KafkaPrefabStream<?> otherKafkaStream)) {
            throw new IllegalArgumentException("Cannot merge non-Kafka stream implementation");
        }

        validateMergeCompatibility(otherKafkaStream);

        @SuppressWarnings("unchecked")
        var thisTypedStream = (KStream<String, S>) stream;
        @SuppressWarnings("unchecked")
        var otherTypedStream = (KStream<String, S>) otherKafkaStream.stream;
        return wrap(thisTypedStream.merge(otherTypedStream), commonKnownType(valueType, otherKafkaStream.valueType));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, NATIVE_IN, NATIVE_OUT> PrefabStream<R> breakout(
            StreamBreakoutAdapter<V, R, NATIVE_IN, NATIVE_OUT> adapter
    ) {
        var breakoutAdapter = Objects.requireNonNull(adapter, "adapter must not be null");
        if (breakoutAdapter.backend() != StreamBackend.KAFKA) {
            throw new IllegalArgumentException(
                    "Unsupported breakout backend '%s' for Kafka stream; use a Kafka adapter"
                            .formatted(breakoutAdapter.backend())
            );
        }
        if (!breakoutAdapter.nativeInputType().isInstance(stream)) {
            throw new IllegalArgumentException(
                    "Kafka breakout adapter input type mismatch: expected %s but received %s"
                            .formatted(
                                    breakoutAdapter.nativeInputType().getName(),
                                    stream.getClass().getName()
                            )
            );
        }

        var adapted = breakoutAdapter.apply(breakoutAdapter.nativeInputType().cast(stream));
        if (!(adapted instanceof KStream<?, ?> adaptedKStream)) {
            var actualType = adapted == null ? "null" : adapted.getClass().getName();
            throw new IllegalArgumentException(
                    "Kafka breakout adapter must return a KStream but returned %s".formatted(actualType)
            );
        }
        return wrap((KStream<String, R>) adaptedKStream);
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
        return new KafkaPrefabStream<>(streamsBuilder, transformed, topicResolver, serializer, deserializer, null);
    }

    private <R> KafkaPrefabStream<R> wrap(KStream<String, R> transformed, Class<?> runtimeType) {
        return new KafkaPrefabStream<>(streamsBuilder, transformed, topicResolver, serializer, deserializer, runtimeType);
    }

    private void validateMergeCompatibility(KafkaPrefabStream<?> other) {
        if (valueType == null || other.valueType == null) {
            return;
        }
        if (valueType.isAssignableFrom(other.valueType) || other.valueType.isAssignableFrom(valueType)) {
            return;
        }
        if (hasSharedInterface(valueType, other.valueType)) {
            return;
        }
        throw new IllegalArgumentException(
                "Cannot safely merge streams with incompatible known runtime types: %s and %s"
                        .formatted(valueType.getName(), other.valueType.getName())
        );
    }

    private static boolean hasSharedInterface(Class<?> left, Class<?> right) {
        for (var interfaceType : left.getInterfaces()) {
            if (interfaceType.isAssignableFrom(right)) {
                return true;
            }
        }
        for (var interfaceType : right.getInterfaces()) {
            if (interfaceType.isAssignableFrom(left)) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> commonKnownType(Class<?> left, Class<?> right) {
        if (left == null || right == null) {
            return null;
        }
        if (left.isAssignableFrom(right)) {
            return left;
        }
        if (right.isAssignableFrom(left)) {
            return right;
        }
        return null;
    }
}
