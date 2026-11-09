package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.AggregationWindow;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabGroupedStream;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.Store;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.StreamProcessor;
import be.appify.prefab.streams.tracing.TracingConfig;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KGroupedStream;
import be.appify.prefab.streams.tracing.MessageTracing;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.apache.kafka.streams.kstream.StreamJoined;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.core.convert.ConversionService;
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
public class KafkaPrefabStream<K, V extends Keyed<K>> implements PrefabStream<K, V> {
    private final StreamsBuilder streamsBuilder;
    private final KStream<K, V> stream;
    private final KafkaTopicResolver topicResolver;
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;
    private final JsonMapper jsonMapper;
    private final ConversionService conversionService;
    private final Map<String, Object> kafkaClientProperties;
    private final ValueTypeHint<V> valueType;
    private final Serde<K> keySerde;
    private final PrefabStreams streams;
    private final StreamStepNames stepNames;
    private final TracingConfig tracing;

    /**
     * Constructs a new KafkaPrefabStream with message tracing disabled.
     */
    public KafkaPrefabStream(
            StreamsBuilder streamsBuilder,
            KStream<K, V> stream,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer,
            JsonMapper jsonMapper,
            ConversionService conversionService,
            Map<String, Object> kafkaClientProperties,
            @NotNull Class<V> valueType,
            PrefabStreams streams,
            Serde<K> keySerde,
            StreamStepNames stepNames
    ) {
        this(streamsBuilder, stream, topicResolver, serializer, deserializer, jsonMapper, conversionService, kafkaClientProperties,
                valueType, streams, keySerde, stepNames, TracingConfig.disabled());
    }

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
            ConversionService conversionService,
            Map<String, Object> kafkaClientProperties,
            @NotNull Class<V> valueType,
            PrefabStreams streams,
            Serde<K> keySerde,
            StreamStepNames stepNames,
            TracingConfig tracing
    ) {
        this(streamsBuilder, stream, topicResolver, serializer, deserializer, jsonMapper, conversionService, kafkaClientProperties,
                ValueTypeHint.known(Objects.requireNonNull(valueType, "valueType must not be null")), streams, keySerde,
                stepNames, tracing);
    }

    private KafkaPrefabStream(
            StreamsBuilder streamsBuilder,
            KStream<K, V> stream,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer,
            JsonMapper jsonMapper,
            ConversionService conversionService,
            Map<String, Object> kafkaClientProperties,
            ValueTypeHint<V> valueType,
            PrefabStreams streams,
            Serde<K> keySerde,
            StreamStepNames stepNames,
            TracingConfig tracing
    ) {
        this.tracing = Objects.requireNonNull(tracing, "tracing must not be null");
        this.streamsBuilder = streamsBuilder;
        this.stream = stream;
        this.topicResolver = topicResolver;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
        this.conversionService = Objects.requireNonNull(conversionService, "conversionService must not be null");
        this.kafkaClientProperties = Map.copyOf(kafkaClientProperties);
        this.valueType = Objects.requireNonNull(valueType, "valueType must not be null");
        this.streams = streams;
        this.keySerde = Objects.requireNonNull(keySerde, "keySerde must not be null");
        this.stepNames = Objects.requireNonNull(stepNames, "stepNames must not be null");
    }

    KafkaPrefabStream(StreamsBuilder builder, KStream<K, V> stream, KafkaTopicResolver resolver,
                      DynamicSerializer serializer, DynamicDeserializer deserializer, JsonMapper mapper,
                      ConversionService conversions, Map<String, Object> properties, EventType<V> type,
                      PrefabStreams streams, Serde<K> keys, StreamStepNames names, TracingConfig tracing) {
        this(builder, stream, resolver, serializer, deserializer, mapper, conversions, properties,
                ValueTypeHint.known(type), streams, keys, names, tracing);
    }

    @Override
    public PrefabStream<K, V> as(EventType<V> type) {
        return wrap(stream, ValueTypeHint.known(type), keySerde);
    }

    @Override
    public java.util.Optional<EventType<V>> knownEventType() {
        return java.util.Optional.ofNullable(valueType.eventType);
    }

    @Override
    public PrefabStream<K, V> filter(Predicate<V> predicate) {
        return wrap(
                stream.filter((key, value) -> predicate.test(value), Named.as(stepNames.nextFilterName(inputTypeOrNull()))),
                valueType,
                keySerde);
    }

    @Override
    public <VO extends Keyed<K>> PrefabStream<K, VO> map(Function<V, VO> mapper) {
        return wrapUnknown(stream.mapValues(mapper::apply, Named.as(stepNames.nextMapName(inputTypeOrNull()))), keySerde);
    }

    @Override
    public <VO extends Keyed<K>> PrefabStream<K, VO> flatMap(Function<V, Iterable<VO>> mapper) {
        return wrapUnknown(stream.flatMapValues(mapper::apply, Named.as(stepNames.nextFlatMapName(inputTypeOrNull()))), keySerde);
    }

    @Override
    public PrefabStream<K, V> branch(Predicate<V> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        var branchId = stepNames.nextBranchName(inputTypeOrNull());
        var branched = stream.split(Named.as(branchId))
                .branch((key, value) -> predicate.test(value), Branched.as("-matched"));
        var namedBranches = branched.noDefaultBranch();
        return wrap(namedBranches.get(branchId + "-matched"), valueType, keySerde);
    }

    @Override
    public <S extends V> PrefabStream<K, S> branch(Class<S> subtype) {
        Objects.requireNonNull(subtype, "subtype must not be null");
        var branchSubtypeId = stepNames.nextBranchSubtypeName(subtype);
        var filteredAndCasted = stream
                .filter((key, value) -> subtype.isInstance(value), Named.as(branchSubtypeId))
                .mapValues(subtype::cast, Named.as(branchSubtypeId + "-cast"));
        return wrapKnown(filteredAndCasted, subtype, keySerde);
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

        if (tracing.enabled()) {
            return tracedJoin(otherKafkaStream, window, joiner);
        }
        return wrap(
                stream.join(
                        otherKafkaStream.stream,
                        joiner::apply,
                        JoinWindows.ofTimeDifferenceAndGrace(window.timeDifference(), window.grace()),
                        StreamJoined.with(keySerde, joinSerde(valueType),
                                joinSerde(otherKafkaStream.valueType)).withName(stepNames.nextJoinName(inputTypeOrNull(), otherKafkaStream.inputTypeOrNull()))
                ),
                ValueTypeHint.unknown(),
                keySerde
        );
    }

    private <VO extends Keyed<K>, VR extends Keyed<K>> PrefabStream<K, VR> tracedJoin(
            KafkaPrefabStream<K, VO> other,
            JoinWindow window,
            BiFunction<? super V, ? super VO, ? extends VR> joiner
    ) {
        var joinName = stepNames.nextJoinName(inputTypeOrNull(), other.inputTypeOrNull());
        KStream<K, TracedValue<V>> left = stream.processValues(KafkaMessageTracing.<K, V>wrap(),
                Named.as(stepNames.nextTraceWrapName(inputTypeOrNull())));
        KStream<K, TracedValue<VO>> right = other.stream.processValues(KafkaMessageTracing.<K, VO>wrap(),
                Named.as(stepNames.nextTraceWrapName(other.inputTypeOrNull())));
        KStream<K, TracedValue<VR>> joined = left.join(
                right,
                (l, r) -> new TracedValue<VR>(
                        joiner.apply(l.value(), r.value()),
                        MessageTracing.union(l.origins(), r.origins(), tracing.maxCorrelationIds())),
                JoinWindows.ofTimeDifferenceAndGrace(window.timeDifference(), window.grace()),
                StreamJoined.with(keySerde,
                        TracedValueSerde.writing(joinSerde(valueType)),
                        TracedValueSerde.writing(joinSerde(other.valueType))).withName(joinName));
        KStream<K, VR> unwrapped = joined.processValues(KafkaMessageTracing.<K, VR>unwrap(),
                Named.as(stepNames.nextTraceUnwrapName(null)));
        return wrap(unwrapped, ValueTypeHint.unknown(), keySerde);
    }

    @Override
    public <KO> PrefabGroupedStream<KO, V> groupBy(Class<KO> keyType, Function<V, KO> keyExtractor) {
        Objects.requireNonNull(keyType, "keyType must not be null");
        Objects.requireNonNull(keyExtractor, "keyExtractor must not be null");
        var groupKeySerde = new StringKeySerde<>(keyType);
        var groupName = stepNames.nextGroupByName(inputTypeOrNull());
        if (tracing.enabled()) {
            KStream<K, TracedValue<V>> traced = stream.processValues(KafkaMessageTracing.<K, V>wrap(),
                    Named.as(stepNames.nextTraceWrapName(inputTypeOrNull())));
            var tracedGrouped = traced.groupBy(
                    (key, value) -> keyExtractor.apply(value.value()),
                    Grouped.with(groupName, groupKeySerde, TracedValueSerde.writing(joinSerde(valueType)))
            );
            return new KafkaGroupedStream<>(null, tracedGrouped, groupKeySerde);
        }
        var grouped = stream.groupBy(
                (key, value) -> keyExtractor.apply(value),
                Grouped.with(groupName, groupKeySerde, joinSerde(valueType))
        );
        return new KafkaGroupedStream<>(grouped, null, groupKeySerde);
    }

    @Override
    public <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> keyBy(Class<KO> keyType, Function<V, VO> mapper) {
        return rekey(keyType, mapper, stepNames.nextRepartitionName(inputTypeOrNull()));
    }

    @Override
    public <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> keyBy(Class<KO> keyType, EventType<VO> outputType,
                                                              Function<V, VO> mapper) {
        var keys = new StringKeySerde<KO>(keyType);
        var hint = ValueTypeHint.known(outputType);
        KStream<KO, VO> mapped = stream.map((key, value) -> {
            var output = mapper.apply(value);
            return KeyValue.pair(output.key(), output);
        });
        return wrap(mapped.repartition(Repartitioned.<KO, VO>with(keys, joinSerde(hint))
                .withName(stepNames.nextRepartitionName(inputTypeOrNull()))), hint, keys);
    }

    @Override
    public <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> keyBy(
            Class<KO> keyType,
            Function<V, VO> mapper,
            String repartitionName
    ) {
        return rekey(keyType, mapper, stepNames.reserveExplicitName(repartitionName));
    }

    private <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> rekey(
            Class<KO> keyType,
            Function<V, VO> mapper,
            String repartitionName
    ) {
        Objects.requireNonNull(keyType, "keyType must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        var newKeySerde = new StringKeySerde<>(keyType);
        KStream<KO, VO> rekeyed = stream.map((key, value) -> {
            var mapped = mapper.apply(value);
            return KeyValue.pair(mapped.key(), mapped);
        });
        var repartitioned = rekeyed.repartition(
                Repartitioned.<KO, VO>with(newKeySerde, joinSerde(ValueTypeHint.<VO>unknown()))
                        .withName(repartitionName));
        return wrapUnknown(repartitioned, newKeySerde);
    }

    @Override
    public PrefabStream<K, V> repartition() {
        return repartitionAs(stepNames.nextRepartitionName(inputTypeOrNull()));
    }

    @Override
    public PrefabStream<K, V> repartition(String name) {
        return repartitionAs(stepNames.reserveExplicitName(name));
    }

    private PrefabStream<K, V> repartitionAs(String name) {
        var repartitioned = stream.repartition(
                Repartitioned.<K, V>with(keySerde, joinSerde(valueType)).withName(name));
        return wrap(repartitioned, valueType, keySerde);
    }

    private final class KafkaGroupedStream<KO> implements PrefabGroupedStream<KO, V> {
        private final KGroupedStream<KO, V> grouped;
        private final KGroupedStream<KO, TracedValue<V>> tracedGrouped;
        private final Serde<KO> groupKeySerde;

        private KafkaGroupedStream(
                KGroupedStream<KO, V> grouped,
                KGroupedStream<KO, TracedValue<V>> tracedGrouped,
                Serde<KO> groupKeySerde
        ) {
            this.grouped = grouped;
            this.tracedGrouped = tracedGrouped;
            this.groupKeySerde = groupKeySerde;
        }

        @Override
        public <VR extends Keyed<KO>> PrefabStream<KO, VR> aggregate(
                Class<VR> resultType,
                Supplier<VR> initializer,
                BiFunction<? super V, ? super VR, ? extends VR> aggregator
        ) {
            return aggregate(ValueTypeHint.known(resultType), initializer, aggregator);
        }

        @Override
        public <VR extends Keyed<KO>> PrefabStream<KO, VR> aggregate(
                EventType<VR> resultType, Supplier<VR> initializer,
                BiFunction<? super V, ? super VR, ? extends VR> aggregator) {
            return aggregate(ValueTypeHint.known(resultType), initializer, aggregator);
        }

        private <VR extends Keyed<KO>> PrefabStream<KO, VR> aggregate(
                ValueTypeHint<VR> resultHint, Supplier<VR> initializer,
                BiFunction<? super V, ? super VR, ? extends VR> aggregator) {
            Objects.requireNonNull(initializer, "initializer must not be null");
            Objects.requireNonNull(aggregator, "aggregator must not be null");
            var name = stepNames.nextAggregateName(inputTypeOrNull(), resultHint.knownRuntimeType());
            if (tracedGrouped != null) {
                KTable<KO, TracedValue<VR>> table = tracedGrouped.aggregate(
                        () -> new TracedValue<>(initializer.get(), List.of()),
                        (key, value, aggregate) -> tracedAggregate(value, aggregate, aggregator),
                        Named.as(name),
                        Materialized.<KO, TracedValue<VR>, KeyValueStore<Bytes, byte[]>>as(name)
                                .withKeySerde(groupKeySerde)
                                .withValueSerde(TracedValueSerde.writing(joinSerde(resultHint)))
                );
                return wrap(unwrapResult(table.toStream()), resultHint, groupKeySerde);
            }
            KTable<KO, VR> table = grouped.aggregate(
                    initializer::get,
                    (key, value, aggregate) -> aggregator.apply(value, aggregate),
                    Named.as(name),
                    Materialized.<KO, VR, KeyValueStore<Bytes, byte[]>>as(name)
                            .withKeySerde(groupKeySerde)
                            .withValueSerde(joinSerde(resultHint))
            );
            return wrap(table.toStream(), resultHint, groupKeySerde);
        }

        @Override
        public <VR extends Keyed<KO>> PrefabStream<KO, VR> windowedAggregate(
                AggregationWindow window,
                Class<VR> resultType,
                Supplier<VR> initializer,
                BiFunction<? super V, ? super VR, ? extends VR> aggregator
        ) {
            return windowedAggregate(window, ValueTypeHint.known(resultType), initializer, aggregator);
        }

        @Override
        public <VR extends Keyed<KO>> PrefabStream<KO, VR> windowedAggregate(
                AggregationWindow window, EventType<VR> resultType, Supplier<VR> initializer,
                BiFunction<? super V, ? super VR, ? extends VR> aggregator) {
            return windowedAggregate(window, ValueTypeHint.known(resultType), initializer, aggregator);
        }

        private <VR extends Keyed<KO>> PrefabStream<KO, VR> windowedAggregate(
                AggregationWindow window, ValueTypeHint<VR> resultHint, Supplier<VR> initializer,
                BiFunction<? super V, ? super VR, ? extends VR> aggregator) {
            Objects.requireNonNull(window, "window must not be null");
            Objects.requireNonNull(initializer, "initializer must not be null");
            Objects.requireNonNull(aggregator, "aggregator must not be null");
            var name = stepNames.nextWindowedAggregateName(inputTypeOrNull(), resultHint.knownRuntimeType());
            var windows = TimeWindows.ofSizeAndGrace(window.size(), window.grace()).advanceBy(window.advance());
            if (tracedGrouped != null) {
                KTable<Windowed<KO>, TracedValue<VR>> table = tracedGrouped.windowedBy(windows).aggregate(
                        () -> new TracedValue<>(initializer.get(), List.of()),
                        (key, value, aggregate) -> tracedAggregate(value, aggregate, aggregator),
                        Named.as(name),
                        Materialized.<KO, TracedValue<VR>, WindowStore<Bytes, byte[]>>as(name)
                                .withKeySerde(groupKeySerde)
                                .withValueSerde(TracedValueSerde.writing(joinSerde(resultHint)))
                );
                return wrap(
                        unwrapResult(table.toStream((windowedKey, value) -> windowedKey.key())),
                        resultHint,
                        groupKeySerde);
            }
            KTable<Windowed<KO>, VR> table = grouped.windowedBy(windows).aggregate(
                    initializer::get,
                    (key, value, aggregate) -> aggregator.apply(value, aggregate),
                    Named.as(name),
                    Materialized.<KO, VR, WindowStore<Bytes, byte[]>>as(name)
                            .withKeySerde(groupKeySerde)
                            .withValueSerde(joinSerde(resultHint))
            );
            return wrap(table.toStream((windowedKey, value) -> windowedKey.key()), resultHint, groupKeySerde);
        }

        private <VR extends Keyed<KO>> TracedValue<VR> tracedAggregate(
                TracedValue<V> input,
                TracedValue<VR> aggregate,
                BiFunction<? super V, ? super VR, ? extends VR> aggregator
        ) {
            return new TracedValue<>(
                    aggregator.apply(input.value(), aggregate.value()),
                    MessageTracing.union(aggregate.origins(), input.origins(), tracing.maxCorrelationIds()));
        }

        private <VR> KStream<KO, VR> unwrapResult(KStream<KO, TracedValue<VR>> results) {
            return results.processValues(KafkaMessageTracing.<KO, VR>unwrap(),
                    Named.as(stepNames.nextTraceUnwrapName(null)));
        }
    }

    static <K, VO extends Keyed<K>> KafkaPrefabStream<K, VO> mergeStreams(
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
                commonKnownType(leftKafkaStream.valueType, rightKafkaStream.valueType),
                leftKafkaStream.keySerde
        );
    }

    @Override
    public PrefabStream<K, V> deduplicate(Function<V, String> idExtractor, Duration retention) {
        Objects.requireNonNull(idExtractor, "idExtractor must not be null");
        Objects.requireNonNull(retention, "retention must not be null");
        if (retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("retention must be positive");
        }
        var name = stepNames.nextDeduplicateName(inputTypeOrNull());
        // A window store keyed by event identity; its retention covers the look-back and look-ahead of the check.
        streamsBuilder.addStateStore(Stores.windowStoreBuilder(
                Stores.persistentWindowStore(name, retention.multipliedBy(2), Duration.ofMillis(1), false),
                Serdes.String(),
                Serdes.Long()));
        var deduplicated = stream.process(
                () -> new DeduplicationProcessor<>(name, idExtractor, retention),
                Named.as(name),
                name);
        return wrap(deduplicated, valueType, keySerde);
    }
    @Override
    @SuppressWarnings("unchecked")
    public <NI, NO, KO, VO extends Keyed<KO>> PrefabStream<KO, VO> breakout(
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
        return wrapUnknown((KStream<KO, VO>) adaptedKStream, new DeferredStringKeySerde<>());
    }

    @Override
    public <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> process(
            StreamProcessor<K, V, KO, VO> processor
    ) {
        Objects.requireNonNull(processor, "processor must not be null");
        processor.initStreams(streams);

        var stores = processor.stateStores();
        var stateStoreNames = stores.stream()
                .map(Store::name)
                .toArray(String[]::new);
        requireDeclared(stateStoreNames);
        requireGlobalDeclared(processor.globalStores().stream().map(GlobalStore::name).toList());
        var output = stream.process(
                () -> new KafkaPrefabStreamProcessorAdapter<>(processor, tracing.maxCorrelationIds()),
                Named.as(stepNames.nextProcessName(inputTypeOrNull())),
                stateStoreNames
        );

        return wrapUnknown(output, new DeferredStringKeySerde<>());
    }

    private void requireDeclared(String[] storeNames) {
        if (streams instanceof KafkaPrefabStreams kafkaStreams) {
            for (var name : storeNames) {
                if (!kafkaStreams.isStoreDeclared(name)) {
                    throw new IllegalArgumentException(
                            "Store '%s' is not declared; create it with createStore(...) before binding it to a processor"
                                    .formatted(name));
                }
            }
        }
    }

    private void requireGlobalDeclared(Collection<String> storeNames) {
        if (streams instanceof KafkaPrefabStreams kafkaStreams) {
            for (var name : storeNames) {
                if (!kafkaStreams.isGlobalStoreDeclared(name)) {
                    throw new IllegalArgumentException(
                            "Global store '%s' is not declared; create it with globalStore(...) before binding it to a processor"
                                    .formatted(name));
                }
            }
        }
    }

    @Override
    public StreamDefinition to(Class<? super V> type) {
        if (type.getTypeParameters().length > 0) {
            throw new IllegalArgumentException("Generic stream sinks require EventType: " + type.getName());
        }
        return to(topicResolver.topicForType(type));
    }

    @Override
    public StreamDefinition to(EventType<? super V> type) {
        return writeTo(topicResolver.topicForType(type), new ConcreteEventSerde<>(type, serializer, deserializer));
    }

    @Override
    public StreamDefinition to(String topic) {
        // DynamicSerializer/Deserializer work on Object at runtime; the cast to Serde<V> is safe
        // because the backend selects serialization by topic, not by the generic type parameter.
        Serde<? super V> valueSerde = valueType.eventType == null
                ? new SerdeAdapter<V>(serializer.adapt(), deserializer.adapt())
                : new ConcreteEventSerde<>(valueType.eventType, serializer, deserializer);
        return writeTo(topic, valueSerde);
    }

    private StreamDefinition writeTo(String topic, Serde<? super V> valueSerde) {
        var writer = valueSerde.serializer();
        var producedSerde = new SerdeAdapter<V>(new Serializer<>() {
            public byte[] serialize(String destination, V value) {
                return writer.serialize(destination, value);
            }
            public byte[] serialize(String destination, org.apache.kafka.common.header.Headers headers, V value) {
                return writer.serialize(destination, headers, value);
            }
        }, deserializer.adapt());
        var published = tracing.enabled()
                ? stream.processValues(KafkaMessageTracing.<K, V>publish(),
                        Named.as(stepNames.nextTracePublishName(inputTypeOrNull())))
                : stream;
        published.to(topic, Produced.with(keySerde, producedSerde));
        return end();
    }

    @Override
    public StreamDefinition end() {
        return new KafkaStreamDefinition(streamsBuilder::build);
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

    private <KO, VO extends Keyed<KO>> KafkaPrefabStream<KO, VO> wrapKnown(
            KStream<KO, VO> kStream,
            Class<VO> valueType,
            Serde<KO> keySerde
    ) {
        return wrap(kStream, ValueTypeHint.known(valueType), keySerde);
    }

    private <KO, VO extends Keyed<KO>> KafkaPrefabStream<KO, VO> wrapUnknown(KStream<KO, VO> kStream, Serde<KO> keySerde) {
        return wrap(kStream, ValueTypeHint.unknown(), keySerde);
    }

    @SuppressWarnings("unchecked")
    private <KO, VO extends Keyed<KO>> KafkaPrefabStream<KO, VO> wrap(
            KStream<KO, VO> kStream,
            ValueTypeHint<VO> valueType,
            Serde<KO> keySerde
    ) {
        return new KafkaPrefabStream<>(
                streamsBuilder,
                kStream,
                topicResolver,
                serializer,
                deserializer,
                jsonMapper,
                conversionService,
                kafkaClientProperties,
                valueType,
                streams,
                keySerde,
                stepNames,
                tracing
        );
    }

    private <T> Serde<T> joinSerde(ValueTypeHint<T> runtimeType) {
        if (runtimeType.eventType != null) {
            return TracedValueSerde.tolerant(new JsonValueSerde<>(jsonMapper, runtimeType.eventType.type()));
        }
        Serializer<T> joinSerializer = (topic, data) -> serializeForJoin(topic, data, runtimeType);
        Deserializer<T> joinDeserializer = (topic, data) -> deserializeForJoin(topic, data, runtimeType);
        return TracedValueSerde.tolerant(new SerdeAdapter<>(joinSerializer, joinDeserializer));
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
        if (left.eventType != null || right.eventType != null) {
            return Objects.equals(left.eventType, right.eventType) ? (ValueTypeHint<V>) left : ValueTypeHint.unknown();
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
        private final EventType<T> eventType;

        private ValueTypeHint(Class<?> runtimeType, boolean known) {
            this.runtimeType = Objects.requireNonNull(runtimeType, "runtimeType must not be null");
            this.known = known;
            this.eventType = null;
        }

        private ValueTypeHint(EventType<T> type) {
            this.runtimeType = type.rawType();
            this.eventType = type;
            this.known = true;
        }

        static <T> ValueTypeHint<T> known(EventType<T> type) {
            return new ValueTypeHint<>(type);
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
