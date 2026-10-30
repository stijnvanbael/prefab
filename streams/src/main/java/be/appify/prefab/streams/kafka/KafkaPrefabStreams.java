package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.Aggregation;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.GlobalStoreUpdater;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.Store;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import be.appify.prefab.streams.TypeReference;
import be.appify.prefab.streams.tracing.TracingConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.state.Stores;
import org.springframework.core.convert.ConversionService;
import tools.jackson.databind.json.JsonMapper;

/** Kafka-backed implementation for the baseline source DSL operation. */
public class KafkaPrefabStreams implements PrefabStreams {
    private final StreamsBuilder streamsBuilder;
    private final KafkaTopicResolver topicResolver;
    private final DynamicSerializer serializer;
    private final DynamicDeserializer deserializer;
    private final JsonMapper jsonMapper;
    private final ConversionService conversionService;
    private final Map<String, Object> kafkaClientProperties;
    private final StreamStepNames stepNames;
    private final Map<String, GlobalStoreDefinition> globalStores = new HashMap<>();
    private final Map<String, String> globalStoreNamesByTopic = new HashMap<>();
    private final Set<String> declaredStores = new HashSet<>();
    private final TracingConfig tracing;

    /**
     * Constructs a new KafkaPrefabStreams with message tracing disabled.
     */
    public KafkaPrefabStreams(
            StreamsBuilder streamsBuilder,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer,
            JsonMapper jsonMapper,
            ConversionService conversionService,
            Map<String, Object> kafkaClientProperties
    ) {
        this(streamsBuilder, topicResolver, serializer, deserializer, jsonMapper, conversionService,
                kafkaClientProperties, TracingConfig.disabled());
    }

    /**
     * Constructs a new KafkaPrefabStreams.
     */
    public KafkaPrefabStreams(
            StreamsBuilder streamsBuilder,
            KafkaTopicResolver topicResolver,
            DynamicSerializer serializer,
            DynamicDeserializer deserializer,
            JsonMapper jsonMapper,
            ConversionService conversionService,
            Map<String, Object> kafkaClientProperties,
            TracingConfig tracing
    ) {
        this.tracing = tracing;
        this.streamsBuilder = streamsBuilder;
        this.topicResolver = topicResolver;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.jsonMapper = jsonMapper;
        this.conversionService = conversionService;
        this.kafkaClientProperties = Map.copyOf(kafkaClientProperties);
        this.stepNames = new StreamStepNames();
    }

    @Override
    public <K, V extends Keyed<K>> PrefabStream<K, V> from(Class<V> type) {
        var topic = topicResolver.topicForType(type);
        var keyType = keyTypeOf(type);
        var keySerde = new StringKeySerde<>(keyType);
        var valueSerde = new SerdeAdapter<V>(serializer.adapt(), deserializer.adapt());
        // DynamicSerializer/Deserializer operate on Object at runtime; the cast is safe because
        // the topic is registered for exactly this type and the serde will deserialize to V.
        KStream<K, V> source = streamsBuilder.stream(topic,
                Consumed.with(keySerde, valueSerde));
        KStream<K, V> stream = tracing.enabled()
                ? source.processValues(KafkaMessageTracing.<K, V>ingress(), Named.as(stepNames.nextTraceIngressName(type)))
                : source;
        return new KafkaPrefabStream<>(
                streamsBuilder,
                stream,
                topicResolver,
                serializer,
                deserializer,
                jsonMapper,
                conversionService,
                kafkaClientProperties,
                type,
                this,
                keySerde,
                stepNames,
                tracing
        );
    }

    @SuppressWarnings("unchecked")
    public static <K, V extends Keyed<K>> Class<K> keyTypeOf(Class<V> valueType) {
        return (Class<K>) resolveKeyType(valueType, Map.of(), valueType);
    }

    private static Class<?> resolveKeyType(Class<?> type, Map<TypeVariable<?>, Type> subs, Class<?> originalType) {
        // Check direct generic interfaces for Keyed<...>
        for (var genericInterface : type.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType pt && pt.getRawType() == Keyed.class) {
                return extractConcreteClass(resolve(pt.getActualTypeArguments()[0], subs), originalType);
            }
        }
        // Recurse into each super-interface
        for (var genericInterface : type.getGenericInterfaces()) {
            var rawInterface = toRawClass(genericInterface);
            if (rawInterface == null || rawInterface == Keyed.class) {
                continue;
            }
            try {
                return resolveKeyType(rawInterface, substitutionsFor(rawInterface, genericInterface, subs), originalType);
            } catch (IllegalArgumentException ignored) {
                // not found in this interface branch; continue searching
            }
        }
        // Recurse into superclass
        var superClass = type.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return resolveKeyType(superClass, substitutionsFor(superClass, type.getGenericSuperclass(), subs), originalType);
        }
        throw new IllegalArgumentException("Cannot resolve key type for %s".formatted(originalType.getName()));
    }

    private static Type resolve(Type type, Map<TypeVariable<?>, Type> subs) {
        return type instanceof TypeVariable<?> tv ? subs.getOrDefault(tv, tv) : type;
    }

    private static Class<?> extractConcreteClass(Type type, Class<?> originalType) {
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) {
            return c;
        }
        throw new IllegalArgumentException("Cannot resolve key type for %s".formatted(originalType.getName()));
    }

    private static Class<?> toRawClass(Type type) {
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) {
            return c;
        }
        return null;
    }

    private static Map<TypeVariable<?>, Type> substitutionsFor(Class<?> rawClass, Type genericType,
            Map<TypeVariable<?>, Type> parentSubs) {
        if (!(genericType instanceof ParameterizedType pt)) {
            return parentSubs;
        }
        var params = rawClass.getTypeParameters();
        var args = pt.getActualTypeArguments();
        if (params.length == 0) {
            return parentSubs;
        }
        var result = new HashMap<>(parentSubs);
        for (var i = 0; i < params.length; i++) {
            result.put(params[i], resolve(args[i], parentSubs));
        }
        return Map.copyOf(result);
    }

    @Override
    public <K, M extends Keyed<K>> PrefabStream<K, M> merge(PrefabStream<K, ? extends M> left,
            PrefabStream<K, ? extends M> right) {
        return KafkaPrefabStream.mergeStreams(left, right);
    }

    @Override
    public <KS, VS extends Keyed<KS>> Store<KS, VS> createStore(TypeReference<VS> type) {
        var name = toStoreName(type.name());
        streamsBuilder.addStateStore(Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(name),
                keySerde(type),
                valueSerde(type)));
        declaredStores.add(name);
        return new KafkaPrefabStoreAdapter<>(name);
    }

    boolean isStoreDeclared(String name) {
        return declaredStores.contains(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <KS, S extends Keyed<KS>, VS extends Keyed<KS>> GlobalStore<KS, VS> globalStore(
            String name,
            Class<S> sourceType,
            Class<VS> storeType,
            Function<S, VS> mapper
    ) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        var storeName = globalStoreName(name);
        var existing = globalStores.get(storeName);
        if (existing != null) {
            return (GlobalStore<KS, VS>) existing.reuse(sourceType, storeType);
        }
        return registerGlobalStore(storeName, sourceType, storeType,
                () -> new GlobalStoreUpdateProcessor<>(storeName, mapper));
    }

    @Override
    public <KS, S extends Keyed<KS>, K, V extends Keyed<K>> GlobalStore<K, V> declareGlobalStore(
            String name,
            Class<S> sourceType,
            Class<V> storeType,
            GlobalStoreUpdater<KS, S, K, V> updater
    ) {
        Objects.requireNonNull(updater, "updater must not be null");
        var storeName = globalStoreName(name);
        if (globalStores.containsKey(storeName)) {
            throw new IllegalArgumentException(
                    "Global store '%s' is already declared; a store maintained by an updater can only be declared once"
                            .formatted(storeName));
        }
        return registerGlobalStore(storeName, sourceType, storeType,
                () -> new GlobalStoreWriterProcessor<>(storeName, updater));
    }

    private static String globalStoreName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("global store name must not be blank");
        }
        return "global-" + toStoreName(name);
    }

    private <KS, S extends Keyed<KS>, K, V extends Keyed<K>> GlobalStore<K, V> registerGlobalStore(
            String storeName,
            Class<S> sourceType,
            Class<V> storeType,
            ProcessorSupplier<KS, S, Void, Void> updateSupplier
    ) {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(storeType, "storeType must not be null");
        var topic = topicResolver.topicForType(sourceType);
        var topicOwner = globalStoreNamesByTopic.get(topic);
        if (topicOwner != null) {
            throw new IllegalArgumentException(
                    "Global store '%s' cannot read topic '%s' (source type %s): the topic already feeds global store '%s'. Kafka allows one global store per source topic"
                            .formatted(storeName, topic, sourceType.getName(), topicOwner));
        }
        var typeReference = TypeReference.of(storeType);
        var sourceKeySerde = new StringKeySerde<KS>(keyTypeOf(sourceType));
        var sourceValueSerde = new SerdeAdapter<S>(serializer.adapt(), deserializer.adapt());
        streamsBuilder.addGlobalStore(
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(storeName),
                        this.<K, V>keySerde(typeReference),
                        globalValueSerde(sourceType, typeReference)).withLoggingDisabled(),
                topic,
                Consumed.with(sourceKeySerde, sourceValueSerde),
                updateSupplier);
        var store = new KafkaPrefabGlobalStoreAdapter<K, V>(storeName, streamsBuilder::build);
        globalStores.put(storeName, new GlobalStoreDefinition(sourceType, storeType, store));
        globalStoreNamesByTopic.put(topic, storeName);
        return store;
    }
    /**
     * Resolves the value serde of a global store. When the stored type equals the source type the topic-based
     * serde is used; otherwise the registry cannot know the stored type, so it is serialised as JSON.
     */
    private <K, V extends Keyed<K>> Serde<V> globalValueSerde(Class<?> sourceType, TypeReference<V> storeType) {
        if (sourceType == storeType.rawType()) {
            return valueSerde(storeType);
        }
        return new JsonValueSerde<>(jsonMapper, storeType.rawType());
    }
    boolean isGlobalStoreDeclared(String name) {
        return globalStores.containsKey(name);
    }


    /**
     * Resolves the value serde for a state store.
     *
     * <p>For {@link Aggregation} stores the generic type parameters ({@code K} and {@code V})
     * are unknown at topology-build time, so a {@link DeferredAggregationSerde} is used. It
     * captures the concrete types from the first instance seen during serialisation and uses a
     * dedicated Jackson {@code ObjectMapper} to deserialise correctly.
     * All other value types use the standard {@link SerdeAdapter}.
     */
    @SuppressWarnings("unchecked")
    private <KS, VS extends Keyed<KS>> Serde<VS> valueSerde(TypeReference<VS> type) {
        if (type.rawType() == (Class<?>) Aggregation.class) {
            return (Serde<VS>) new DeferredAggregationSerde<>();
        }
        return new SerdeAdapter<>(serializer.adapt(), deserializer.adapt());
    }

    /**
     * Resolves the key serde for a state store.
     *
     * <p>When the value type carries concrete key-type information (e.g. a domain record that
     * directly implements {@code Keyed<ConcreteKey>}), a {@link JsonKeySerde} is returned.
     * When the key type is a type variable that cannot be resolved statically — which happens for
     * generic wrappers such as {@code Aggregation<KO, V>} — a {@link DeferredJsonKeySerde} is
     * returned instead. The deferred serde learns the concrete key class from the first key it
     * serialises, which is safe for stores that only use {@code get}/{@code put}.
     */
    private <KS, VS extends Keyed<KS>> Serde<KS> keySerde(TypeReference<VS> type) {
        try {
            return new JsonKeySerde<>(
                    keyTypeOf(type.rawType()),
                    jsonMapper,
                    serializer.eventRegistry(),
                    conversionService,
                    kafkaClientProperties);
        } catch (IllegalArgumentException e) {
            return new DeferredJsonKeySerde<>(jsonMapper);
        }
    }

    public static String toKebabCase(String value) {
        return value.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
    }

    /**
     * Converts a fully-qualified type name (possibly containing generic type parameters) into a
     * name that is safe for use as a Kafka state-store name and as a file-system directory entry.
     *
     * <p>Angle brackets, commas, and spaces introduced by generic type parameters are replaced
     * with hyphens; consecutive hyphens and trailing punctuation are collapsed.
     */
    public static String toStoreName(String typeName) {
        return toKebabCase(typeName)
                .replaceAll("\\W", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("[-.]+$", "");
    }

    private record GlobalStoreDefinition(Class<?> sourceType, Class<?> storeType, GlobalStore<?, ?> store) {
        GlobalStore<?, ?> reuse(Class<?> requestedSourceType, Class<?> requestedStoreType) {
            if (sourceType != requestedSourceType || storeType != requestedStoreType) {
                throw new IllegalArgumentException(
                        "Global store '%s' was already declared with source=%s, store=%s but received source=%s, store=%s"
                                .formatted(store.name(), sourceType.getName(), storeType.getName(),
                                        requestedSourceType.getName(), requestedStoreType.getName()));
            }
            return store;
        }
    }
}