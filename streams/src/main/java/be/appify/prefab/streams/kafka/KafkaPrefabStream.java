package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.Store;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.StreamProcessor;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.StreamJoined;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final PrefabStreams streams;

    /**
     * Constructs a new KafkaPrefabStream.
     */
    public KafkaPrefabStream(
            StreamsBuilder streamsBuilder,
            KStream<String, V> stream,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer,
            Class<?> valueType, PrefabStreams streams
    ) {
        this.streamsBuilder = streamsBuilder;
        this.stream = stream;
        this.topicResolver = topicResolver;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.valueType = valueType;
        this.streams = streams;
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
        var branchId = "branch-" + UUID.randomUUID();
        var branched = stream.split(Named.as(branchId))
                .branch((key, value) -> predicate.test(value), Branched.as("matched"));
        var namedBranches = branched.noDefaultBranch();
        return wrap(namedBranches.get(branchId + "-matched"), valueType);
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
    public PrefabStream<V> merge(PrefabStream<? extends V> other) {
        return mergeStreams(this, other);
    }

    @Override
    public <VO, VR> PrefabStream<VR> join(
            PrefabStream<VO> other,
            JoinWindow window,
            BiFunction<? super V, ? super VO, ? extends VR> joiner
    ) {
        Objects.requireNonNull(other, "other must not be null");
        Objects.requireNonNull(window, "window must not be null");
        Objects.requireNonNull(joiner, "joiner must not be null");

        if (!(other instanceof KafkaPrefabStream<?> otherKafkaStream)) {
            throw new IllegalArgumentException("Cannot join non-Kafka stream implementation");
        }

        validateSameContext(otherKafkaStream);

        @SuppressWarnings("unchecked")
        var rightStream = (KStream<String, VO>) otherKafkaStream.stream;
        @SuppressWarnings("unchecked")
        var leftSerde = (Serde<V>) joinSerde(valueType);
        @SuppressWarnings("unchecked")
        var rightSerde = (Serde<VO>) joinSerde(otherKafkaStream.valueType);

        return wrap(
                stream.join(
                        rightStream,
                        joiner::apply,
                        JoinWindows.ofTimeDifferenceAndGrace(window.timeDifference(), window.grace()),
                        StreamJoined.<String, V, VO>with(Serdes.String(), leftSerde, rightSerde)
                )
        );
    }

    static <V> KafkaPrefabStream<V> mergeStreams(PrefabStream<? extends V> left, PrefabStream<? extends V> right) {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(right, "right must not be null");

        if (!(left instanceof KafkaPrefabStream<?> leftKafkaStream)) {
            throw new IllegalArgumentException("Cannot merge non-Kafka stream implementation");
        }
        if (!(right instanceof KafkaPrefabStream<?> rightKafkaStream)) {
            throw new IllegalArgumentException("Cannot merge non-Kafka stream implementation");
        }

        leftKafkaStream.validateSameContext(rightKafkaStream);

        @SuppressWarnings("unchecked")
        var leftTypedStream = (KStream<String, V>) leftKafkaStream.stream;
        @SuppressWarnings("unchecked")
        var rightTypedStream = (KStream<String, V>) rightKafkaStream.stream;
        return leftKafkaStream.wrap(
                leftTypedStream.merge(rightTypedStream),
                commonKnownType(leftKafkaStream.valueType, rightKafkaStream.valueType)
        );
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
    public <VO> PrefabStream<VO> process(StreamProcessor<V, VO> processor) {
        Objects.requireNonNull(processor, "processor must not be null");
        processor.initStreams(streams);

        var stores = processor.stateStores();
        var stateStoreNames = stores.stream()
                .map(Store::name)
                .toArray(String[]::new);
        var output = stream.process(() -> new KafkaPrefabStreamProcessorAdapter<>(processor), stateStoreNames);

        return wrap(output);
    }

    @Override
    public StreamDefinition to(Class<? super V> type) {
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
        return new KafkaPrefabStream<>(streamsBuilder, transformed, topicResolver, serializer, deserializer, null, streams);
    }

    private <R> KafkaPrefabStream<R> wrap(KStream<String, R> transformed, Class<?> runtimeType) {
        return new KafkaPrefabStream<>(
                streamsBuilder, transformed, topicResolver, serializer, deserializer, runtimeType, streams);
    }

    @SuppressWarnings("unchecked")
    private <T> Serde<T> joinSerde(Class<?> runtimeType) {
        Deserializer<T> joinDeserializer = (topic, data) -> deserializeForJoin(topic, data, runtimeType);
        return new SerdeAdapter<>((Serializer<T>) serializer, joinDeserializer);
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializeForJoin(String topic, byte[] data, Class<?> runtimeType) {
        if (data == null) {
            return null;
        }
        if (topicResolver.hasSerializationForTopic(topic) || runtimeType == null) {
            return (T) deserializer.deserialize(topic, data);
        }
        return (T) OBJECT_MAPPER.readValue(data, runtimeType);
    }

    private void validateSameContext(KafkaPrefabStream<?> other) {
        if (streamsBuilder == other.streamsBuilder
                && topicResolver == other.topicResolver
                && serializer == other.serializer
                && deserializer == other.deserializer) {
            return;
        }
        throw new IllegalArgumentException("Cannot combine streams created by different Kafka stream contexts");
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
