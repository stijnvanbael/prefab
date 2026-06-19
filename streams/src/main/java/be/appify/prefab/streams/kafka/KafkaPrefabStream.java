package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.Store;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.StreamProcessor;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.StreamJoined;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kafka-backed implementation of {@link PrefabStream}.
 *
 * <p>Each operator delegates to the corresponding native {@link KStream} method, preserving
 * Prefab's serialization infrastructure for the terminal {@code to} operations.
 *
 * @param <V>
 *         current record value type
 */
public class KafkaPrefabStream<K extends Key<K>, V extends Keyed<K>> implements PrefabStream<K, V> {
    private final StreamsBuilder streamsBuilder;
    private final KStream<K, V> stream;
    private final KafkaTopicResolver topicResolver;
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;
    private final JsonMapper jsonMapper;
    private final ValueTypeHint<V> valueType;
    private final Class<K> keyType;
    private final PrefabStreams streams;
    private final StreamStepNames stepNames;

    /**
     * Constructs a new KafkaPrefabStream.
     */
    public KafkaPrefabStream(
            StreamsBuilder streamsBuilder,
            KStream<K, V> stream,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer,
            JsonMapper jsonMapper,
            @NotNull Class<V> valueType,
            PrefabStreams streams,
            Class<K> keyType,
            StreamStepNames stepNames
    ) {
        this(streamsBuilder, stream, topicResolver, serializer, deserializer, jsonMapper,
                ValueTypeHint.known(Objects.requireNonNull(valueType, "valueType must not be null")), streams, keyType,
                stepNames);
    }

    private KafkaPrefabStream(
            StreamsBuilder streamsBuilder,
            KStream<K, V> stream,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer,
            JsonMapper jsonMapper,
            ValueTypeHint<V> valueType,
            PrefabStreams streams,
            Class<K> keyType,
            StreamStepNames stepNames
    ) {
        this.streamsBuilder = streamsBuilder;
        this.stream = stream;
        this.topicResolver = topicResolver;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
        this.valueType = Objects.requireNonNull(valueType, "valueType must not be null");
        this.streams = streams;
        this.keyType = keyType;
        this.stepNames = Objects.requireNonNull(stepNames, "stepNames must not be null");
    }

    @Override
    public PrefabStream<K, V> filter(Predicate<V> predicate) {
        return wrap(stream.filter((key, value) -> predicate.test(value), Named.as(stepNames.nextFilterName(inputTypeOrNull()))), valueType);
    }

    @Override
    public <VO extends Keyed<K>> PrefabStream<K, VO> map(Function<V, VO> mapper) {
        return wrapUnknown(stream.mapValues(mapper::apply, Named.as(stepNames.nextMapName(inputTypeOrNull()))));
    }

    @Override
    public <VO extends Keyed<K>> PrefabStream<K, VO> flatMap(Function<V, Iterable<VO>> mapper) {
        return wrapUnknown(stream.flatMapValues(mapper::apply, Named.as(stepNames.nextFlatMapName(inputTypeOrNull()))));
    }

    @Override
    public PrefabStream<K, V> branch(Predicate<V> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        var branchId = stepNames.nextBranchName(inputTypeOrNull());
        var branched = stream.split(Named.as(branchId))
                .branch((key, value) -> predicate.test(value), Branched.as("-matched"));
        var namedBranches = branched.noDefaultBranch();
        return wrap(namedBranches.get(branchId + "-matched"), valueType);
    }

    @Override
    public <S extends V> PrefabStream<K, S> branch(Class<S> subtype) {
        Objects.requireNonNull(subtype, "subtype must not be null");
        var branchSubtypeId = stepNames.nextBranchSubtypeName(subtype);
        var filteredAndCasted = stream
                .filter((key, value) -> subtype.isInstance(value), Named.as(branchSubtypeId))
                .mapValues(subtype::cast, Named.as(branchSubtypeId + "-cast"));
        return wrapKnown(filteredAndCasted, subtype);
    }

    @Override
    public PrefabStream<K, V> merge(PrefabStream<K, V> other) {
        return mergeStreams(this, other);
    }

    @Override
    public <VO extends Keyed<K>, VR extends Keyed<K>> PrefabStream<K, VR> join(
            PrefabStream<K, VO> other,
            JoinWindow window,
            BiFunction<? super V, ? super VO, ? extends VR> joiner
    ) {
        Objects.requireNonNull(other, "other must not be null");
        Objects.requireNonNull(window, "window must not be null");
        Objects.requireNonNull(joiner, "joiner must not be null");

        if (!(other instanceof KafkaPrefabStream<K, VO> otherKafkaStream)) {
            throw new IllegalArgumentException("Cannot join non-Kafka stream implementation");
        }

        validateSameContext(otherKafkaStream);

        return wrap(
                stream.join(
                        otherKafkaStream.stream,
                        joiner::apply,
                        JoinWindows.ofTimeDifferenceAndGrace(window.timeDifference(), window.grace()),
                        StreamJoined.with(new JsonKeySerde<>(keyType, jsonMapper), joinSerde(valueType),
                                joinSerde(otherKafkaStream.valueType)).withName(stepNames.nextJoinName(inputTypeOrNull(), otherKafkaStream.inputTypeOrNull()))
                ),
                ValueTypeHint.unknown()
        );
    }

    static <K extends Key<K>, VO extends Keyed<K>> KafkaPrefabStream<K, VO> mergeStreams(
            PrefabStream<K, ? extends VO> left,
            PrefabStream<K, ? extends VO> right
    ) {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(right, "right must not be null");

        if (!(left.unwrap() instanceof KafkaPrefabStream<K, ? extends VO> leftKafkaStream)) {
            throw new IllegalArgumentException("Cannot merge non-Kafka stream implementation");
        }
        if (!(right.unwrap() instanceof KafkaPrefabStream<K, ? extends VO> rightKafkaStream)) {
            throw new IllegalArgumentException("Cannot merge non-Kafka stream implementation");
        }

        //noinspection unchecked
        return leftKafkaStream.wrap(
                ((KStream<K, VO>) leftKafkaStream.stream).merge(
                        (KStream<K, VO>) rightKafkaStream.stream,
                        Named.as(leftKafkaStream.stepNames.nextMergeName(
                                leftKafkaStream.inputTypeOrNull(), rightKafkaStream.inputTypeOrNull()
                        ))
                ),
                commonKnownType(leftKafkaStream.valueType, rightKafkaStream.valueType)
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <NI, NO, KO extends Key<KO>, VO extends Keyed<KO>> PrefabStream<KO, VO> breakout(
            StreamBreakoutAdapter<K, V, KO, VO, NI, NO> adapter
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
        return wrapUnknown((KStream<KO, VO>) adaptedKStream);
    }

    @Override
    public <KO extends Key<KO>, VO extends Keyed<KO>> PrefabStream<KO, VO> process(
            StreamProcessor<K, V, KO, VO> processor
    ) {
        Objects.requireNonNull(processor, "processor must not be null");
        processor.initStreams(streams);

        var stores = processor.stateStores();
        var stateStoreNames = stores.stream()
                .map(Store::name)
                .toArray(String[]::new);
        var output = stream.process(
                () -> new KafkaPrefabStreamProcessorAdapter<>(processor),
                Named.as(stepNames.nextProcessName(inputTypeOrNull())),
                stateStoreNames
        );

        return wrapUnknown(output);
    }

    @Override
    public StreamDefinition to(Class<? super V> type) {
        return to(topicResolver.topicForType(type));
    }

    @Override
    public StreamDefinition to(String topic) {
        // DynamicSerializer/Deserializer work on Object at runtime; the cast to Serde<V> is safe
        // because the backend selects serialization by topic, not by the generic type parameter.
        var valueSerde = new SerdeAdapter<V>(serializer.adapt(), deserializer.adapt());
        stream.to(topic, Produced.with(new JsonKeySerde<>(keyType, jsonMapper), valueSerde));
        return new StreamDefinition(streamsBuilder::build);
    }

    @Override
    public PrefabStream<K, V> unwrap() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<V> knownValueType() {
        return valueType.isUnknown() ? null : (Class<V>) valueType.knownRuntimeType();
    }

    private Class<?> inputTypeOrNull() {
        return valueType.isUnknown() ? null : valueType.knownRuntimeType();
    }

    private <KO extends Key<KO>, VO extends Keyed<KO>> KafkaPrefabStream<KO, VO> wrapKnown(
            KStream<KO, VO> kStream,
            Class<VO> valueType
    ) {
        return wrap(kStream, ValueTypeHint.known(valueType));
    }

    private <KO extends Key<KO>, VO extends Keyed<KO>> KafkaPrefabStream<KO, VO> wrapUnknown(KStream<KO, VO> kStream) {
        return wrap(kStream, ValueTypeHint.unknown());
    }

    @SuppressWarnings("unchecked")
    private <KO extends Key<KO>, VO extends Keyed<KO>> KafkaPrefabStream<KO, VO> wrap(
            KStream<KO, VO> kStream,
            ValueTypeHint<VO> valueType
    ) {
        return new KafkaPrefabStream<>(
                streamsBuilder,
                kStream,
                topicResolver,
                serializer,
                deserializer,
                jsonMapper,
                valueType,
                streams,
                (Class<KO>) keyType,
                stepNames
        );
    }

    private <T> Serde<T> joinSerde(ValueTypeHint<T> runtimeType) {
        Serializer<T> joinSerializer = (topic, data) -> serializeForJoin(topic, data, runtimeType);
        Deserializer<T> joinDeserializer = (topic, data) -> deserializeForJoin(topic, data, runtimeType);
        return new SerdeAdapter<>(joinSerializer, joinDeserializer);
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializeForJoin(String topic, byte[] data, ValueTypeHint<T> runtimeType) {
        if (data == null) {
            return null;
        }
        if (!topicResolver.hasSerializationForTopic(topic)) {
            registerInternalTopic(topic, resolveRuntimeType(runtimeType, null));
        }
        return (T) deserializer.deserialize(topic, data);
    }

    private <T> byte[] serializeForJoin(String topic, T data, ValueTypeHint<T> runtimeType) {
        if (data == null) {
            return null;
        }
        if (!topicResolver.hasSerializationForTopic(topic)) {
            registerInternalTopic(topic, resolveRuntimeType(runtimeType, data));
        }
        return serializer.serialize(topic, data);
    }

    private Class<?> resolveRuntimeType(ValueTypeHint<?> runtimeType, Object value) {
        if (!runtimeType.isUnknown()) {
            return runtimeType.knownRuntimeType();
        }
        return value != null ? value.getClass() : null;
    }

    private void registerInternalTopic(String topic, Class<?> runtimeType) {
        if (runtimeType == null) {
            return;
        }

        EventRegistry eventRegistry = serializer.eventRegistry();
        if (!eventRegistry.contains(topic)) {
            eventRegistry.register(topic, resolveSerialization(runtimeType, eventRegistry));
        }
        if (!eventRegistry.hasTypeForTopic(topic)) {
            eventRegistry.registerType(topic, runtimeType);
        }
    }

    private Event.Serialization resolveSerialization(
            Class<?> runtimeType,
            EventRegistry eventRegistry
    ) {
        return eventRegistry.tryTopicForType(runtimeType)
                .filter(eventRegistry::contains)
                .map(eventRegistry::serialization)
                .orElse(Event.Serialization.JSON);
    }

    private void validateSameContext(KafkaPrefabStream<?, ?> other) {
        if (streamsBuilder == other.streamsBuilder
                && topicResolver == other.topicResolver
                && serializer == other.serializer
                && deserializer == other.deserializer) {
            return;
        }
        throw new IllegalArgumentException("Cannot combine streams created by different Kafka stream contexts");
    }

    @SuppressWarnings("unchecked")
    private static <V> ValueTypeHint<V> commonKnownType(ValueTypeHint<? extends V> left, ValueTypeHint<? extends V> right) {
        if (left.isUnknown() || right.isUnknown()) {
            return ValueTypeHint.unknown();
        }
        var leftType = left.knownRuntimeType();
        var rightType = right.knownRuntimeType();
        if (leftType.isAssignableFrom(rightType)) {
            return ValueTypeHint.known((Class<V>) leftType);
        }
        if (rightType.isAssignableFrom(leftType)) {
            return ValueTypeHint.known((Class<V>) rightType);
        }
        return ValueTypeHint.unknown();
    }

    private static final class ValueTypeHint<T> {
        private static final ValueTypeHint<?> UNKNOWN = new ValueTypeHint<>(Object.class, false);

        private final Class<?> runtimeType;
        private final boolean known;

        private ValueTypeHint(Class<?> runtimeType, boolean known) {
            this.runtimeType = Objects.requireNonNull(runtimeType, "runtimeType must not be null");
            this.known = known;
        }

        static <T> ValueTypeHint<T> known(Class<T> runtimeType) {
            return new ValueTypeHint<>(runtimeType, true);
        }

        @SuppressWarnings("unchecked")
        static <T> ValueTypeHint<T> unknown() {
            return (ValueTypeHint<T>) UNKNOWN;
        }

        boolean isUnknown() {
            return !known;
        }

        @SuppressWarnings("unchecked")
        Class<? extends T> knownRuntimeType() {
            if (isUnknown()) {
                throw new IllegalStateException("No known runtime type is available");
            }
            return (Class<? extends T>) runtimeType;
        }
    }
}
