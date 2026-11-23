package be.appify.prefab.streams.engine;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.AggregationWindow;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.GlobalStoreUpdater;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabGroupedStream;
import be.appify.prefab.streams.PrefabStream;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.Store;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.StreamProcessor;
import be.appify.prefab.streams.StreamProcessorContext;
import be.appify.prefab.streams.StreamRecord;
import be.appify.prefab.streams.TypeReference;
import be.appify.prefab.streams.tracing.MessageTracing;
import be.appify.prefab.streams.tracing.TracingConfig;
import java.time.Duration;
import java.time.Instant;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.springframework.core.ResolvableType;

/**
 * In-process implementation of the Prefab Streams DSL.
 *
 * <p>Each input delivery is interpreted in one repository transaction. State updates and sink publications are
 * committed together through the inbox and outbox; the broker acknowledgement follows the commit.
 */
public final class InProcessPrefabStreams implements PrefabStreams {
    /** Backend identity used by engine diagnostics and unsupported-operation errors. */
    public static final StreamBackend BACKEND = StreamBackend.named("in-process");

    private static final int MAX_CONFLICT_ATTEMPTS = 3;
    private static final int MAX_OUTBOX_BATCH = 256;
    private static final Duration DEFAULT_INBOX_RETENTION = Duration.ofDays(7);
    private static final System.Logger LOGGER = System.getLogger(InProcessPrefabStreams.class.getName());

    private final StreamSource source;
    private final StreamSink sink;
    private final StreamStateRepository repository;
    private final StreamTopicResolver topicResolver;
    private final TracingConfig tracing;
    private final Duration inboxRetention;
    private final String relayOwner = UUID.randomUUID().toString();
    private final CopyOnWriteArrayList<Route> declaredRoutes = new CopyOnWriteArrayList<>();
    private final Map<String, EngineGlobalStore<?, ?>> globalStores = new LinkedHashMap<>();
    private final Map<String, GlobalDeclaration> globalDeclarationsByName = new LinkedHashMap<>();
    private final Map<String, String> globalStoreNamesByTopic = new LinkedHashMap<>();
    private final Set<String> declaredStoreNames = new LinkedHashSet<>();
    private final Set<String> repartitionNames = new LinkedHashSet<>();
    private final Set<String> sinkTopics = ConcurrentHashMap.newKeySet();
    private final AtomicInteger nameSequence = new AtomicInteger();
    private final ReentrantLock declarationsLock = new ReentrantLock();
    private final ReentrantLock relayLock = new ReentrantLock();
    private final ThreadLocal<StreamStateRepository.Transaction> activeTransaction = new ThreadLocal<>();
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * Creates an engine bound to source and sink adapters.
     *
     * @param source broker source adapter
     * @param sink broker sink adapter
     * @param repository transactional state repository
     * @param topicResolver maps event types to broker topics
     */
    public InProcessPrefabStreams(
            StreamSource source,
            StreamSink sink,
            StreamStateRepository repository,
            StreamTopicResolver topicResolver
    ) {
        this(source, sink, repository, topicResolver, TracingConfig.disabled(), DEFAULT_INBOX_RETENTION);
    }

    /**
     * Creates an engine with explicit message-tracing behavior.
     *
     * @param source broker source adapter
     * @param sink broker sink adapter
     * @param repository transactional state repository
     * @param topicResolver maps event types to broker topics
     * @param tracing message-tracing configuration
     */
    public InProcessPrefabStreams(
            StreamSource source,
            StreamSink sink,
            StreamStateRepository repository,
            StreamTopicResolver topicResolver,
            TracingConfig tracing
    ) {
        this(source, sink, repository, topicResolver, tracing, DEFAULT_INBOX_RETENTION);
    }

    /**
     * Creates an engine with explicit trace and inbox-retention settings.
     *
     * @param source broker source adapter
     * @param sink broker sink adapter
     * @param repository transactional state repository
     * @param topicResolver maps event types to broker topics
     * @param tracing message-tracing configuration
     * @param inboxRetention how long processed message identities are retained
     */
    public InProcessPrefabStreams(
            StreamSource source,
            StreamSink sink,
            StreamStateRepository repository,
            StreamTopicResolver topicResolver,
            TracingConfig tracing,
            Duration inboxRetention
    ) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.sink = Objects.requireNonNull(sink, "sink must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.topicResolver = Objects.requireNonNull(topicResolver, "topicResolver must not be null");
        this.tracing = Objects.requireNonNull(tracing, "tracing must not be null");
        requirePositive(inboxRetention, "inboxRetention");
        this.inboxRetention = inboxRetention;
    }

    @Override
    public <K, V extends Keyed<K>> PrefabStream<K, V> from(Class<V> type) {
        Objects.requireNonNull(type, "type must not be null");
        if (type.getTypeParameters().length > 0) {
            throw new IllegalArgumentException("Generic stream sources require EventType: " + type.getName());
        }
        return source(topicResolver.topicFor(type), type, null);
    }

    @Override
    public <K, V extends Keyed<K>> PrefabStream<K, V> from(EventType<V> type) {
        Objects.requireNonNull(type, "type must not be null");
        return source(topicResolver.topicFor(type), type.rawType(), type);
    }

    private <K, V extends Keyed<K>> PrefabStream<K, V> source(
            String topic,
            Class<?> valueType,
            EventType<V> eventType
    ) {
        return new EnginePrefabStream<>(this,
                List.of(new Route(topic, bindingId(eventType), valueType, List.of())), valueType, eventType);
    }

    @Override
    public <K, M extends Keyed<K>> PrefabStream<K, M> merge(
            PrefabStream<K, ? extends M> left,
            PrefabStream<K, ? extends M> right
    ) {
        var leftEngine = engineStream(left);
        var rightEngine = engineStream(right);
        var routes = new ArrayList<Route>(leftEngine.routes);
        routes.addAll(rightEngine.routes);
        return new EnginePrefabStream<>(this, routes, commonType(leftEngine.valueType, rightEngine.valueType), null);
    }

    @Override
    public <KS, VS extends Keyed<KS>> Store<KS, VS> createStore(TypeReference<VS> type) {
        Objects.requireNonNull(type, "type must not be null");
        var name = uniqueStoreName(stateStoreName(type.name()));
        return new EngineStore<>(this, name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <KS, S extends Keyed<KS>, K, V extends Keyed<K>> GlobalStore<K, V> globalStore(
            String name,
            EventType<S> sourceType,
            TypeReference<V> storeType,
            Function<S, V> mapper
    ) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return declareGlobal(name, topicResolver.topicFor(sourceType), sourceType.rawType(), bindingId(sourceType),
                storeType, true,
                (record, store) -> {
                    if (record.value() == null) {
                        store.put(record.key(), null);
                    } else {
                        var value = mapper.apply((S) record.value());
                        if (value == null) {
                            store.put(record.key(), null);
                        } else {
                            store.put(value.key(), value);
                        }
                    }
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <KS, S extends Keyed<KS>, K, V extends Keyed<K>> GlobalStore<K, V> declareGlobalStore(
            String name,
            EventType<S> sourceType,
            TypeReference<V> storeType,
            GlobalStoreUpdater<KS, S, K, V> updater
    ) {
        Objects.requireNonNull(updater, "updater must not be null");
        return declareGlobal(name, topicResolver.topicFor(sourceType), sourceType.rawType(), bindingId(sourceType),
                storeType, false,
                (record, store) -> {
                    if (record.value() != null) {
                        updater.update((StreamRecord<KS, S>) record, (Store<K, V>) (Store<?, ?>) store);
                    }
                });
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
        return declareGlobal(name, topicResolver.topicFor(sourceType), sourceType, null,
                TypeReference.of(storeType), true,
                (record, store) -> {
                    if (record.value() == null) {
                        store.put(record.key(), null);
                    } else {
                        var value = mapper.apply((S) record.value());
                        if (value == null) {
                            store.put(record.key(), null);
                        } else {
                            store.put(value.key(), value);
                        }
                    }
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <KS, S extends Keyed<KS>, K, V extends Keyed<K>> GlobalStore<K, V> declareGlobalStore(
            String name,
            Class<S> sourceType,
            Class<V> storeType,
            GlobalStoreUpdater<KS, S, K, V> updater
    ) {
        Objects.requireNonNull(updater, "updater must not be null");
        return declareGlobal(name, topicResolver.topicFor(sourceType), sourceType, null,
                TypeReference.of(storeType), false,
                (record, store) -> {
                    if (record.value() != null) {
                        updater.update((StreamRecord<KS, S>) record, (Store<K, V>) (Store<?, ?>) store);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private <K, V> EngineGlobalStore<K, V> declareGlobal(
            String name,
            String topic,
            Class<?> sourceType,
            String sourceBindingId,
            TypeReference<V> storeType,
            boolean reuseExisting,
            GlobalUpdater updater
    ) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("global store name must not be blank");
        }
        var storeName = globalStoreName(name);
        declarationsLock.lock();
        try {
            var existing = globalStores.get(storeName);
            if (existing != null) {
                if (!reuseExisting) {
                    throw new IllegalArgumentException(
                            "Global store '%s' is already declared; an updater store can only be declared once"
                                    .formatted(storeName));
                }
                var existingDeclaration = globalDeclarationsByName.get(storeName);
                if (!existingDeclaration.topic.equals(topic)
                        || existingDeclaration.sourceType != sourceType
                        || !Objects.equals(existingDeclaration.sourceBindingId, sourceBindingId)
                        || existingDeclaration.storeType != storeType.rawType()) {
                    throw new IllegalArgumentException(
                            "Global store '%s' was already declared differently".formatted(storeName));
                }
                return (EngineGlobalStore<K, V>) existing;
            }
            var existingName = globalStoreNamesByTopic.get(topic);
            if (existingName != null) {
                throw new IllegalArgumentException(
                        "Source topic '%s' already feeds global store '%s'".formatted(topic, existingName));
            }
            var store = new EngineGlobalStore<K, V>(this, storeName);
            globalStores.put(storeName, store);
            globalDeclarationsByName.put(storeName,
                    new GlobalDeclaration(storeName, topic, sourceType, sourceBindingId, storeType.rawType(), updater));
            globalStoreNamesByTopic.put(topic, storeName);
            declaredStoreNames.add(storeName);
            return store;
        } finally {
            declarationsLock.unlock();
        }
    }

    /** Starts subscriptions for the declared topology. */
    public RunningTopology start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("This in-process topology has already been started");
        }
        var subscriptions = new ArrayList<StreamSource.Subscription>();
        ScheduledExecutorService maintenance = null;
        try {
            var routes = List.copyOf(declaredRoutes);
            var globals = snapshotGlobalDeclarations();
            validateUnsupported(routes);
            initializeProcessors(routes);
            warnForUnorderedStatefulSources(routes);
            var topics = new LinkedHashSet<String>();
            routes.stream().map(Route::sourceTopic).forEach(topics::add);
            globals.stream().map(GlobalDeclaration::topic).forEach(topics::add);
            topics.forEach(topic -> subscriptions.add(source.subscribe(topic,
                    (message, acknowledgement) -> processMessage(topic, message, acknowledgement, routes, globals))));
            repository.cleanupExpired(Instant.now(), inboxRetention);
            relayOutbox();
            maintenance = Executors.newSingleThreadScheduledExecutor(task -> {
                var thread = new Thread(task, "prefab-streams-engine-maintenance");
                thread.setDaemon(true);
                return thread;
            });
            maintenance.scheduleWithFixedDelay(this::runMaintenance, 1, 1, TimeUnit.MINUTES);
            return new RunningTopology(subscriptions, routes, maintenance);
        } catch (RuntimeException e) {
            subscriptions.forEach(StreamSource.Subscription::close);
            if (maintenance != null) {
                maintenance.shutdownNow();
            }
            started.set(false);
            throw e;
        }
    }

    private List<GlobalDeclaration> snapshotGlobalDeclarations() {
        declarationsLock.lock();
        try {
            return List.copyOf(globalDeclarationsByName.values());
        } finally {
            declarationsLock.unlock();
        }
    }

    private void validateUnsupported(List<Route> routes) {
        routes.stream()
                .flatMap(route -> route.stages.stream())
                .filter(stage -> stage.unsupportedMessage != null)
                .findFirst()
                .ifPresent(stage -> {
                    throw new IllegalStateException(stage.unsupportedMessage);
                });
    }

    private void initializeProcessors(List<Route> routes) {
        var initialized = new LinkedHashSet<Operator>();
        routes.stream().flatMap(route -> route.stages.stream()).map(Stage::operator)
                .filter(Objects::nonNull).filter(initialized::add)
                .forEach(Operator::start);
    }

    private void warnForUnorderedStatefulSources(List<Route> routes) {
        routes.stream().filter(route -> !source.ordered(route.sourceTopic))
                .flatMap(route -> route.stages.stream().filter(Stage::stateful)
                        .map(stage -> Map.entry(route.sourceTopic, stage.name)))
                .distinct()
                .forEach(entry -> LOGGER.log(System.Logger.Level.WARNING,
                        "Stateful stream operator '{0}' reads unordered source '{1}'; state is version-safe but "
                                + "order-dependent results may vary",
                        entry.getValue(), entry.getKey()));
    }

    private void processMessage(
            String topic,
            StreamMessage original,
            StreamSource.Acknowledgement acknowledgement,
            List<Route> routes,
            List<GlobalDeclaration> globals
    ) {
        var message = tracing.enabled() ? tracedIngress(original) : original;
        for (var attempt = 1; attempt <= MAX_CONFLICT_ATTEMPTS; attempt++) {
            var transaction = repository.beginTransaction();
            var previousTransaction = activeTransaction.get();
            try {
                if (!transaction.recordInbox(topic, message.messageId())) {
                    transaction.rollback();
                    relayOutbox();
                    acknowledgement.acknowledge();
                    return;
                }
                activeTransaction.set(transaction);
                for (var declaration : globals) {
                    if (declaration.topic.equals(topic) && declaration.accepts(message)) {
                        applyGlobalUpdate(declaration, message);
                    }
                }
                for (var route : routes) {
                    if (route.sourceTopic.equals(topic) && route.accepts(message)) {
                        execute(route, message, transaction);
                    }
                }
                transaction.commit();
                relayOutbox();
                acknowledgement.acknowledge();
                return;
            } catch (StreamStateRepository.OptimisticConflictException conflict) {
                transaction.rollback();
                if (attempt == MAX_CONFLICT_ATTEMPTS) {
                    throw conflict;
                }
            } catch (RuntimeException failure) {
                transaction.rollback();
                throw failure;
            } finally {
                if (previousTransaction == null) {
                    activeTransaction.remove();
                } else {
                    activeTransaction.set(previousTransaction);
                }
            }
        }
    }

    private StreamMessage tracedIngress(StreamMessage message) {
        var headers = new LinkedHashMap<>(message.headers());
        var origins = MessageTracing.decode(headers.get(MessageTracing.CORRELATION_IDS_HEADER));
        headers.put(MessageTracing.CORRELATION_IDS_HEADER,
                MessageTracing.encode(boundedCorrelationIds(origins, List.of(message.messageId()))));
        return message.withRecord(message.key(), message.value(), headers);
    }

    private List<String> boundedCorrelationIds(List<String> first, List<String> second) {
        var merged = MessageTracing.union(first, second, tracing.maxCorrelationIds());
        while (!merged.isEmpty()
                && MessageTracing.encode(merged).getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1024) {
            merged = merged.subList(1, merged.size());
        }
        return merged;
    }

    private void execute(Route route, StreamMessage initial, StreamStateRepository.Transaction transaction) {
        var current = List.of(initial);
        var execution = new Execution(transaction);
        for (var stage : route.stages) {
            if (stage.operator == null) {
                continue;
            }
            var next = new ArrayList<StreamMessage>();
            for (var message : current) {
                next.addAll(stage.operator.apply(message, execution));
            }
            current = next;
            if (current.isEmpty()) {
                return;
            }
        }
    }

    private void applyGlobalUpdate(GlobalDeclaration declaration, StreamMessage message) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        var record = new StreamRecord(message.key(), (Keyed) message.value(), message.eventTime(), message.headers());
        @SuppressWarnings("unchecked")
        var store = (Store<Object, Object>) (Store<?, ?>) new EngineStore<>(this, declaration.name);
        declaration.updater.update(record, store);
    }

    private void relayOutbox() {
        if (!relayLock.tryLock()) {
            return;
        }
        try {
            for (var topic : sinkTopics) {
                while (true) {
                    var records = repository.leaseOutbox(
                            topic, relayOwner, Instant.now(), Duration.ofMinutes(1), MAX_OUTBOX_BATCH);
                    if (records.isEmpty()) {
                        break;
                    }
                    for (var record : records) {
                        sink.publish(record.topic(), record.message());
                        repository.completeOutbox(relayOwner, record.id());
                    }
                }
            }
        } finally {
            relayLock.unlock();
        }
    }

    private void runMaintenance() {
        try {
            repository.cleanupExpired(Instant.now(), inboxRetention);
            relayOutbox();
        } catch (RuntimeException failure) {
            LOGGER.log(System.Logger.Level.ERROR, "Streams engine maintenance failed", failure);
        }
    }

    private void register(List<Route> routes) {
        declaredRoutes.addAll(routes);
    }

    private String uniqueStoreName(String requested) {
        declarationsLock.lock();
        try {
            var name = requested;
            while (!declaredStoreNames.add(name)) {
                name = requested + "-" + nameSequence.incrementAndGet();
            }
            return name;
        } finally {
            declarationsLock.unlock();
        }
    }

    private String registerRepartitionName(String name, String displayName) {
        declarationsLock.lock();
        try {
            if (!repartitionNames.add(name)) {
                throw new IllegalArgumentException(
                        "Repartition topic name '%s' conflicts with another name. Choose a unique name"
                                .formatted(displayName));
            }
            return name;
        } finally {
            declarationsLock.unlock();
        }
    }

    private String uniqueRepartitionName(String requested) {
        declarationsLock.lock();
        try {
            var name = requested;
            while (!repartitionNames.add(name)) {
                name = requested + "-" + nameSequence.incrementAndGet();
            }
            return name;
        } finally {
            declarationsLock.unlock();
        }
    }

    private static String bindingId(EventType<?> eventType) {
        return eventType != null && eventType.type() instanceof ParameterizedType ? eventType.id() : null;
    }

    private List<String> aggregateCorrelationIds(AggregateState<?> previous, StreamMessage message) {
        if (!tracing.enabled()) {
            return List.of();
        }
        var existing = previous == null ? List.<String>of() : previous.correlationIds();
        return boundedCorrelationIds(
                existing,
                MessageTracing.decode(message.headers().get(MessageTracing.CORRELATION_IDS_HEADER)));
    }

    private Map<String, String> aggregateHeaders(StreamMessage message, List<String> correlationIds) {
        if (!tracing.enabled()) {
            return message.headers();
        }
        var headers = new LinkedHashMap<>(message.headers());
        if (correlationIds.isEmpty()) {
            headers.remove(MessageTracing.CORRELATION_IDS_HEADER);
        } else {
            headers.put(MessageTracing.CORRELATION_IDS_HEADER, MessageTracing.encode(correlationIds));
        }
        return headers;
    }

    private static String globalStoreName(String name) {
        return "global-" + stateStoreName(name);
    }

    private static String stateStoreName(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1-$2")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "")
                .toLowerCase(java.util.Locale.ROOT);
    }

    private StreamStateRepository.Transaction currentTransaction() {
        var transaction = activeTransaction.get();
        if (transaction == null) {
            throw new IllegalStateException("Stream state is only available while processing a message");
        }
        return transaction;
    }

    private Optional<Object> readState(String namespace, Object key) {
        var transaction = repository.beginTransaction();
        try {
            var value = transaction.get(namespace, key);
            transaction.rollback();
            return value;
        } catch (RuntimeException failure) {
            transaction.rollback();
            throw failure;
        }
    }

    private <K, V extends Keyed<K>> EnginePrefabStream<K, V> engineStream(PrefabStream<K, ? extends V> stream) {
        Objects.requireNonNull(stream, "stream must not be null");
        if (!(stream.unwrap() instanceof InProcessPrefabStreams.EnginePrefabStream<?, ?> unwrapped)
                || unwrapped.owner != this) {
            throw new IllegalArgumentException("Cannot combine streams from different in-process topologies");
        }
        @SuppressWarnings("unchecked")
        var cast = (EnginePrefabStream<K, V>) unwrapped;
        return cast;
    }

    private static Class<?> commonType(Class<?> left, Class<?> right) {
        return Objects.equals(left, right) ? left : null;
    }

    private String nextName(String prefix) {
        return prefix + "-" + nameSequence.incrementAndGet();
    }

    /** A running set of broker subscriptions. */
    public final class RunningTopology implements AutoCloseable {
        private final List<StreamSource.Subscription> subscriptions;
        private final List<Route> routes;
        private final ScheduledExecutorService maintenance;
        private final AtomicBoolean closed = new AtomicBoolean();

        private RunningTopology(
                List<StreamSource.Subscription> subscriptions,
                List<Route> routes,
                ScheduledExecutorService maintenance
        ) {
            this.subscriptions = List.copyOf(subscriptions);
            this.routes = List.copyOf(routes);
            this.maintenance = maintenance;
        }

        /** Returns a backend-neutral description of this topology. */
        public be.appify.prefab.streams.testing.TopologyInspection inspect() {
            var description = routes.stream()
                    .map(route -> route.sourceTopic + " -> " + route.stages.stream().map(Stage::name).toList())
                    .sorted()
                    .reduce((first, second) -> first + System.lineSeparator() + second)
                    .orElse("");
            declarationsLock.lock();
            try {
                return new be.appify.prefab.streams.testing.TopologyInspection(
                        description,
                        declaredStoreNames.stream().sorted().toList(),
                        globalStores.size(),
                        (int) routes.stream().map(Route::sourceTopic).distinct().count());
            } finally {
                declarationsLock.unlock();
            }
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                subscriptions.forEach(StreamSource.Subscription::close);
                maintenance.shutdownNow();
                started.set(false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private final class EnginePrefabStream<K, V extends Keyed<K>> implements PrefabStream<K, V> {
        private final InProcessPrefabStreams owner;
        private final List<Route> routes;
        private final Class<?> valueType;
        private final EventType<V> eventType;

        private EnginePrefabStream(
                InProcessPrefabStreams owner,
                List<Route> routes,
                Class<?> valueType,
                EventType<V> eventType
        ) {
            this.owner = owner;
            this.routes = List.copyOf(routes);
            this.valueType = valueType;
            this.eventType = eventType;
        }

        @Override
        public PrefabStream<K, V> filter(Predicate<V> predicate) {
            Objects.requireNonNull(predicate, "predicate must not be null");
            return append("filter", false, (message, execution) ->
                    predicate.test((V) message.value()) ? List.of(message) : List.of());
        }

        @Override
        public <VO extends Keyed<K>> PrefabStream<K, VO> map(Function<V, VO> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return appendTyped("map", false, (message, execution) -> {
                var value = mapper.apply((V) message.value());
                return value == null ? List.of() : List.of(message.withRecord(message.key(), value, message.headers()));
            }, null, null);
        }

        @Override
        public <VO extends Keyed<K>> PrefabStream<K, VO> flatMap(Function<V, Iterable<VO>> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return appendTyped("flatMap", false, (message, execution) -> {
                var values = mapper.apply((V) message.value());
                if (values == null) {
                    return List.of();
                }
                var result = new ArrayList<StreamMessage>();
                for (var value : values) {
                    if (value != null) {
                        result.add(message.withRecord(message.key(), value, message.headers()));
                    }
                }
                return result;
            }, null, null);
        }

        @Override
        public PrefabStream<K, V> branch(Predicate<V> predicate) {
            return filter(predicate);
        }

        @Override
        public <S extends V> PrefabStream<K, S> branch(Class<S> subtype) {
            Objects.requireNonNull(subtype, "subtype must not be null");
            return appendTyped("branch-" + subtype.getSimpleName(), false, (message, execution) ->
                    subtype.isInstance(message.value())
                            ? List.of(message.withRecord(message.key(), subtype.cast(message.value()), message.headers()))
                            : List.of(), subtype, null);
        }

        @Override
        public PrefabStream<K, V> merge(PrefabStream<K, V> other) {
            var otherStream = owner.engineStream(other);
            var merged = new ArrayList<>(routes);
            merged.addAll(otherStream.routes);
            return new EnginePrefabStream<>(owner, merged, commonType(valueType, otherStream.valueType), null);
        }

        @Override
        public <VO extends Keyed<K>, VR extends Keyed<K>> PrefabStream<K, VR> join(
                PrefabStream<K, VO> other,
                JoinWindow window,
                BiFunction<? super V, ? super VO, ? extends VR> joiner
        ) {
            Objects.requireNonNull(window, "window must not be null");
            Objects.requireNonNull(joiner, "joiner must not be null");
            var otherStream = owner.engineStream(other);
            var name = owner.nextName("join");
            var operation = new JoinOperator(name, window, (left, right) ->
                    joiner.apply((V) left.value(), (VO) right.value()));
            var joinedRoutes = new ArrayList<Route>();
            routes.stream().map(route -> route.append(new Stage(name + "-left", true, operation.left()))).forEach(joinedRoutes::add);
            otherStream.routes.stream().map(route -> route.append(new Stage(name + "-right", true, operation.right())))
                    .forEach(joinedRoutes::add);
            return new EnginePrefabStream<>(owner, joinedRoutes, null, null);
        }

        @Override
        public <KO> PrefabGroupedStream<KO, V> groupBy(Class<KO> keyType, Function<V, KO> keyExtractor) {
            Objects.requireNonNull(keyType, "keyType must not be null");
            Objects.requireNonNull(keyExtractor, "keyExtractor must not be null");
            var groupName = owner.uniqueRepartitionName("group-by-" + kebabCase(
                    valueType == null ? "unknown" : valueType.getSimpleName()) + "-repartition");
            var groupedRoutes = routes.stream()
                    .map(route -> route.append(new Stage(groupName, false, (message, execution) -> List.of(message))))
                    .toList();
            return new EngineGroupedStream<>(groupedRoutes, keyType, (value) -> keyExtractor.apply((V) value));
        }

        @Override
        public <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> keyBy(
                Class<KO> keyType,
                Function<V, VO> mapper
        ) {
            return rekey(mapper, "repartition-" + kebabCase(
                        valueType == null ? "unknown" : valueType.getSimpleName()) + "-repartition", false, null);
        }

        @Override
        public <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> keyBy(
                Class<KO> keyType,
                EventType<VO> outputType,
                Function<V, VO> mapper
        ) {
            return appendTyped("keyBy", false, (message, execution) -> {
                var value = mapper.apply((V) message.value());
                return List.of(message.withRecord(value.key(), value, message.headers()));
            }, outputType.rawType(), outputType);
        }

        @Override
        public <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> keyBy(
                Class<KO> keyType,
                Function<V, VO> mapper,
                String repartitionName
        ) {
            var validatedName = validateRepartitionName(repartitionName);
            return rekey(mapper, validatedName + "-repartition", true, validatedName);
        }

        private <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> rekey(
                Function<V, VO> mapper,
                String name,
                boolean explicitName,
                String displayName
        ) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            var registeredName = explicitName
                    ? owner.registerRepartitionName(name, displayName)
                    : owner.uniqueRepartitionName(name);
            return appendTyped(registeredName, false, (message, execution) -> {
                var value = mapper.apply((V) message.value());
                return value == null ? List.of() : List.of(message.withRecord(value.key(), value, message.headers()));
            }, null, null);
        }

        @Override
        public PrefabStream<K, V> repartition() {
            var name = owner.uniqueRepartitionName("repartition-" + kebabCase(
                    valueType == null ? "unknown" : valueType.getSimpleName()) + "-repartition");
            return append(name, false, (message, execution) -> List.of(message));
        }

        @Override
        public PrefabStream<K, V> repartition(String name) {
            var validatedName = validateRepartitionName(name);
            var stepName = owner.registerRepartitionName(validatedName + "-repartition", validatedName);
            return append(stepName, false, (message, execution) -> List.of(message));
        }

        @Override
        public PrefabStream<K, V> deduplicate(Function<V, String> idExtractor, Duration retention) {
            Objects.requireNonNull(idExtractor, "idExtractor must not be null");
            requirePositive(retention, "retention");
            var name = owner.uniqueStoreName("deduplicate-" + kebabCase(
                    valueType == null ? "unknown" : valueType.getSimpleName()));
            return append(name, true, (message, execution) -> {
                var identity = idExtractor.apply((V) message.value());
                if (identity == null) {
                    return List.of(message);
                }
                execution.transaction.deleteExpired(name, identity, message.eventTime());
                var previous = execution.transaction.get(name, identity).map(Instant.class::cast);
                if (previous.isPresent() && !message.eventTime().isAfter(previous.orElseThrow().plus(retention))) {
                    return List.of();
                }
                execution.transaction.putExpiring(
                        name, identity, identity, message.eventTime(), message.eventTime().plus(retention));
                return List.of(message);
            });
        }

        @Override
        public <NI, NO, KO, VO extends Keyed<KO>> PrefabStream<KO, VO> breakout(
                StreamBreakoutAdapter<K, V, KO, VO, NI, NO> adapter
        ) {
            Objects.requireNonNull(adapter, "adapter must not be null");
            return appendTyped("breakout", false, (message, execution) -> List.of(message), null, null,
                    "The in-process streams backend does not support breakout; use a backend-native topology.");
        }

        @Override
        public <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> process(
                StreamProcessor<K, V, KO, VO> processor
        ) {
            Objects.requireNonNull(processor, "processor must not be null");
            processor.initStreams(owner);
            var stores = List.copyOf(processor.stateStores());
            var global = List.copyOf(processor.globalStores());
            stores.forEach(store -> owner.declareStoreName(store.name()));
            for (var store : global) {
                if (!owner.globalStores.containsKey(store.name())) {
                    throw new IllegalArgumentException("Global store '%s' is not declared".formatted(store.name()));
                }
            }
            var operation = new ProcessorOperator<>(processor, stores, global);
            var storeNames = stores.stream().map(Store::name).toList();
            var stageName = storeNames.isEmpty() ? "process" : "process(" + String.join(",", storeNames) + ")";
            var outputValueType = ResolvableType.forClass(processor.getClass())
                    .as(StreamProcessor.class).getGeneric(3).resolve();
            return appendTyped(stageName, !stores.isEmpty(), operation, outputValueType, null);
        }

        @Override
        public StreamDefinition to(Class<? super V> type) {
            if (type.getTypeParameters().length > 0) {
                throw new IllegalArgumentException("Generic stream sinks require EventType: " + type.getName());
            }
            return writeTo(owner.topicResolver.topicFor(type), null);
        }

        @Override
        public StreamDefinition to(EventType<? super V> type) {
            return writeTo(owner.topicResolver.topicFor(type), type);
        }

        @Override
        public PrefabStream<K, V> as(EventType<V> type) {
            return new EnginePrefabStream<>(owner, routes, type.rawType(), type);
        }

        @Override
        public StreamDefinition to(String topic) {
            Objects.requireNonNull(topic, "topic must not be null");
            if (topic.isBlank()) {
                throw new IllegalArgumentException("topic must not be blank");
            }
            return writeTo(topic, eventType);
        }

        private StreamDefinition writeTo(String topic, EventType<?> outputType) {
            owner.sinkTopics.add(topic);
            if (outputType != null) {
                owner.topicResolver.register(outputType, topic);
            } else if (valueType != null
                    && routes.stream().anyMatch(route -> route.sourceValueType != valueType)) {
                owner.topicResolver.register(valueType, topic);
            }
            var stage = new Stage("to(" + topic + ")", false, (message, execution) -> {
                var headers = new LinkedHashMap<>(message.headers());
                if (outputType != null) {
                    headers.put(EventType.HEADER, outputType.id());
                }
                var messageId = UUID.randomUUID().toString();
                headers.put(MessageTracing.MESSAGE_ID_HEADER, messageId);
                if (tracing.enabled()) {
                    var correlation = boundedCorrelationIds(
                            MessageTracing.decode(headers.get(MessageTracing.CORRELATION_IDS_HEADER)),
                            List.of(message.messageId()));
                    if (!correlation.isEmpty()) {
                        headers.put(MessageTracing.CORRELATION_IDS_HEADER, MessageTracing.encode(correlation));
                    }
                }
                execution.transaction.enqueue(topic,
                        new StreamMessage(messageId, message.key(), message.value(), headers, message.eventTime()));
                return List.of();
            });
            owner.register(routes.stream().map(route -> route.append(stage)).toList());
            return new EngineStreamDefinition(topic);
        }

        @Override
        public StreamDefinition end() {
            owner.register(routes);
            return new EngineStreamDefinition("end");
        }

        @Override
        public PrefabStream<K, V> unwrap() {
            return this;
        }

        @Override
        public Class<V> knownValueType() {
            @SuppressWarnings("unchecked")
            var known = (Class<V>) valueType;
            return known;
        }

        @Override
        public Optional<EventType<V>> knownEventType() {
            return Optional.ofNullable(eventType);
        }

        private PrefabStream<K, V> append(
                String name,
                boolean stateful,
                Operator operation
        ) {
            return new EnginePrefabStream<>(owner, routes.stream()
                    .map(route -> route.append(new Stage(name, stateful, operation))).toList(), valueType, eventType);
        }

        private <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> appendTyped(
                String name,
                boolean stateful,
                Operator operation,
                Class<?> newValueType,
                EventType<?> newEventType
        ) {
            return appendTyped(name, stateful, operation, newValueType, newEventType, null);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> appendTyped(
                String name,
                boolean stateful,
                Operator operation,
                Class<?> newValueType,
                EventType<?> newEventType,
                String unsupportedMessage
        ) {
            var stage = new Stage(name, stateful, operation, unsupportedMessage);
            return new EnginePrefabStream(owner, routes.stream().map(route -> route.append(stage)).toList(),
                    newValueType, (EventType) newEventType);
        }

        @SuppressWarnings("unchecked")
        private final class EngineGroupedStream<KO> implements PrefabGroupedStream<KO, V> {
            private final List<Route> groupedRoutes;
            private final Class<KO> keyType;
            private final Function<Object, KO> keyExtractor;

            private EngineGroupedStream(List<Route> groupedRoutes, Class<KO> keyType, Function<Object, KO> keyExtractor) {
                this.groupedRoutes = List.copyOf(groupedRoutes);
                this.keyType = keyType;
                this.keyExtractor = keyExtractor;
            }

            @Override
            public <VR extends Keyed<KO>> PrefabStream<KO, VR> aggregate(
                    Class<VR> resultType,
                    Supplier<VR> initializer,
                    BiFunction<? super V, ? super VR, ? extends VR> aggregator
            ) {
                return aggregate(resultType, null, initializer, aggregator);
            }

            @Override
            public <VR extends Keyed<KO>> PrefabStream<KO, VR> aggregate(
                    EventType<VR> resultType,
                    Supplier<VR> initializer,
                    BiFunction<? super V, ? super VR, ? extends VR> aggregator
            ) {
                return aggregate(resultType.rawType(), resultType, initializer, aggregator);
            }

            private <VR extends Keyed<KO>> PrefabStream<KO, VR> aggregate(
                    Class<?> resultType,
                    EventType<VR> resultEventType,
                    Supplier<VR> initializer,
                    BiFunction<? super V, ? super VR, ? extends VR> aggregator
            ) {
                Objects.requireNonNull(initializer, "initializer must not be null");
                Objects.requireNonNull(aggregator, "aggregator must not be null");
                var storeName = owner.uniqueStoreName("aggregate-" + sourceTypeName() + "-"
                        + kebabCase(resultType == null ? "result" : resultType.getSimpleName()));
                var operation = (Operator) (message, execution) -> {
                    if (message.value() == null) {
                        return List.of();
                    }
                    var groupKey = keyExtractor.apply(message.value());
                    var previous = execution.transaction.get(storeName, groupKey)
                            .map(AggregateState.class::cast)
                            .orElse(null);
                    var current = previous == null ? initializer.get() : (VR) previous.value();
                    var updated = aggregator.apply((V) message.value(), current);
                    if (updated == null) {
                        throw new IllegalStateException("Aggregate function returned null for key " + groupKey);
                    }
                    var correlations = owner.aggregateCorrelationIds(previous, message);
                    execution.transaction.put(storeName, groupKey, new AggregateState<>(updated, correlations));
                    return List.of(message.withRecord(groupKey, updated,
                            owner.aggregateHeaders(message, correlations)));
                };
                var stage = new Stage(storeName, true, operation);
                return new EnginePrefabStream<>(owner,
                        groupedRoutes.stream().map(route -> route.append(stage)).toList(), resultType, resultEventType);
            }

            @Override
            public <VR extends Keyed<KO>> PrefabStream<KO, VR> windowedAggregate(
                    AggregationWindow window,
                    Class<VR> resultType,
                    Supplier<VR> initializer,
                    BiFunction<? super V, ? super VR, ? extends VR> aggregator
            ) {
                return windowedAggregate(window, resultType, null, initializer, aggregator);
            }

            @Override
            public <VR extends Keyed<KO>> PrefabStream<KO, VR> windowedAggregate(
                    AggregationWindow window,
                    EventType<VR> resultType,
                    Supplier<VR> initializer,
                    BiFunction<? super V, ? super VR, ? extends VR> aggregator
            ) {
                return windowedAggregate(window, resultType.rawType(), resultType, initializer, aggregator);
            }

            private <VR extends Keyed<KO>> PrefabStream<KO, VR> windowedAggregate(
                    AggregationWindow window,
                    Class<?> resultType,
                    EventType<VR> resultEventType,
                    Supplier<VR> initializer,
                    BiFunction<? super V, ? super VR, ? extends VR> aggregator
            ) {
                Objects.requireNonNull(window, "window must not be null");
                Objects.requireNonNull(initializer, "initializer must not be null");
                Objects.requireNonNull(aggregator, "aggregator must not be null");
                var storeName = owner.uniqueStoreName("windowed-aggregate-" + sourceTypeName() + "-"
                        + kebabCase(resultType == null ? "result" : resultType.getSimpleName()));
                var operation = (Operator) (message, execution) -> {
                    if (message.value() == null) {
                        return List.of();
                    }
                    var groupKey = keyExtractor.apply(message.value());
                    var streamTimeName = storeName + "-stream-time";
                    var streamTime = execution.transaction.get(streamTimeName, groupKey)
                            .map(Instant.class::cast)
                            .map(previous -> previous.isAfter(message.eventTime()) ? previous : message.eventTime())
                            .orElse(message.eventTime());
                    execution.transaction.put(streamTimeName, groupKey, streamTime);
                    execution.transaction.deleteExpired(storeName, groupKey, streamTime);
                    var outputs = new ArrayList<StreamMessage>();
                    var sizeMillis = window.size().toMillis();
                    var advanceMillis = window.advance().toMillis();
                    var eventMillis = message.eventTime().toEpochMilli();
                    var start = Math.floorDiv(eventMillis, advanceMillis) * advanceMillis;
                    while (start + sizeMillis > eventMillis) {
                        var end = Instant.ofEpochMilli(start + sizeMillis);
                        if (!streamTime.isAfter(end.plus(window.grace()))) {
                            var windowKey = new WindowKey(groupKey, start);
                            var previous = execution.transaction.get(storeName, windowKey)
                                    .map(AggregateState.class::cast)
                                    .orElse(null);
                            var current = previous == null ? initializer.get() : (VR) previous.value();
                            var updated = aggregator.apply((V) message.value(), current);
                            if (updated == null) {
                                throw new IllegalStateException("Window aggregate returned null for key " + groupKey);
                            }
                            var correlations = owner.aggregateCorrelationIds(previous, message);
                            execution.transaction.putExpiring(storeName, windowKey, groupKey,
                                    new AggregateState<>(updated, correlations), end.plus(window.grace()));
                            outputs.add(message.withRecord(groupKey, updated,
                                    owner.aggregateHeaders(message, correlations)));
                        }
                        start -= advanceMillis;
                    }
                    return outputs;
                };
                var stage = new Stage(storeName, true, operation);
                return new EnginePrefabStream<>(owner,
                        groupedRoutes.stream().map(route -> route.append(stage)).toList(), resultType, resultEventType);
            }

            private String sourceTypeName() {
                return kebabCase(valueType == null ? "unknown" : valueType.getSimpleName());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private final class ProcessorOperator<KI, VI extends Keyed<KI>, KO, VO extends Keyed<KO>> implements Operator {
        private final StreamProcessor<KI, VI, KO, VO> processor;
        private final Collection<Store<?, ?>> stores;
        private final Collection<GlobalStore<?, ?>> globals;
        private final EngineProcessorContext<KO, VO> context = new EngineProcessorContext<>();

        private ProcessorOperator(
                StreamProcessor<KI, VI, KO, VO> processor,
                Collection<Store<?, ?>> stores,
                Collection<GlobalStore<?, ?>> globals
        ) {
            this.processor = processor;
            this.stores = stores;
            this.globals = globals;
        }

        @Override
        public void start() {
            processor.initContext(context);
            stores.forEach(store -> store.init(context));
            globals.forEach(store -> store.init(context));
        }

        @Override
        public List<StreamMessage> apply(StreamMessage message, Execution execution) {
            context.begin(message);
            try {
                processor.process(new StreamRecord<>((KI) message.key(), (VI) message.value(),
                        message.eventTime(), message.headers()));
                return context.takeForwarded();
            } finally {
                context.end();
            }
        }
    }

    private final class EngineProcessorContext<K, V extends Keyed<K>> implements StreamProcessorContext<K, V> {
        private final ThreadLocal<ProcessorInvocation> invocation = new ThreadLocal<>();

        private void begin(StreamMessage input) {
            invocation.set(new ProcessorInvocation(input, new ArrayList<>()));
        }

        private List<StreamMessage> takeForwarded() {
            return List.copyOf(current().forwarded);
        }

        private void end() {
            invocation.remove();
        }

        @Override
        public void forward(StreamRecord<K, V> record) {
            var current = current();
            var headers = new LinkedHashMap<>(current.input.headers());
            headers.putAll(record.headers() == null ? Map.of() : record.headers());
            current.forwarded.add(new StreamMessage(
                    current.input.messageId(),
                    record.key(),
                    record.value(),
                    headers,
                    record.timestamp() == null ? current.input.eventTime() : record.timestamp()));
        }

        @Override
        public void schedule(Duration interval, java.util.function.Consumer<Instant> task) {
            throw new IllegalStateException(
                    "StreamProcessorContext.schedule is unsupported by the in-process backend; see TASK-363");
        }

        @Override
        public int maxCorrelationIds() {
            return tracing.maxCorrelationIds();
        }

        private ProcessorInvocation current() {
            var current = invocation.get();
            if (current == null) {
                throw new IllegalStateException("Processor context is only available while processing a message");
            }
            return current;
        }
    }

    @SuppressWarnings("unchecked")
    private final class EngineStore<K, V> implements Store<K, V> {
        private final InProcessPrefabStreams owner;
        private final String name;

        private EngineStore(InProcessPrefabStreams owner, String name) {
            this.owner = owner;
            this.name = name;
        }

        @Override
        public Optional<V> get(K key) {
            var transaction = owner.activeTransaction.get();
            var value = transaction == null
                    ? owner.readState(name, key)
                    : transaction.get(name, key);
            return value.map(stored -> (V) stored);
        }

        @Override
        public void put(K key, V value) {
            if (value == null) {
                owner.currentTransaction().delete(name, key);
            } else {
                owner.currentTransaction().put(name, key, value);
            }
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void init(StreamProcessorContext<?, ?> context) {
        }
    }

    @SuppressWarnings("unchecked")
    private final class EngineGlobalStore<K, V> implements GlobalStore<K, V> {
        private final InProcessPrefabStreams owner;
        private final String name;

        private EngineGlobalStore(InProcessPrefabStreams owner, String name) {
            this.owner = owner;
            this.name = name;
        }

        @Override
        public Optional<V> get(K key) {
            var transaction = owner.activeTransaction.get();
            var value = transaction == null
                    ? owner.readState(name, key)
                    : transaction.get(name, key);
            return value.map(stored -> (V) stored);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void init(StreamProcessorContext<?, ?> context) {
        }
    }

    private final class EngineStreamDefinition implements StreamDefinition {
        private final String name;

        private EngineStreamDefinition(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private interface GlobalUpdater {
        void update(StreamRecord<?, ?> record, Store<Object, Object> store);
    }

    private final class JoinOperator {
        private final String name;
        private final JoinWindow window;
        private final BiFunction<StreamMessage, StreamMessage, Object> joiner;

        private JoinOperator(
                String name,
                JoinWindow window,
                BiFunction<StreamMessage, StreamMessage, Object> joiner
        ) {
            this.name = name;
            this.window = window;
            this.joiner = joiner;
        }

        private Operator left() {
            return (message, execution) -> apply(message, execution, true);
        }

        private Operator right() {
            return (message, execution) -> apply(message, execution, false);
        }

        private List<StreamMessage> apply(StreamMessage message, Execution execution, boolean leftSide) {
            if (message.value() == null) {
                return List.of();
            }
            var key = message.key();
            var state = execution.transaction.get(name, key).map(JoinState.class::cast).orElse(JoinState.empty());
            var streamTime = state.streamTime.isAfter(message.eventTime()) ? state.streamTime : message.eventTime();
            var oldestAccepted = streamTime.minus(window.timeDifference()).minus(window.grace());
            if (message.eventTime().isBefore(oldestAccepted)) {
                return List.of();
            }
            var matches = leftSide ? state.right : state.left;
            var results = matches.stream()
                    .filter(candidate -> Duration.between(candidate.eventTime(), message.eventTime()).abs()
                            .compareTo(window.timeDifference()) <= 0)
                    .map(candidate -> {
                        var first = leftSide ? message : candidate;
                        var second = leftSide ? candidate : message;
                        var value = joiner.apply(first, second);
                        var headers = mergeHeaders(first.headers(), second.headers());
                        return new StreamMessage(message.messageId(), key, value, headers, message.eventTime());
                    }).toList();
            var left = prune(leftSide ? append(state.left, message) : state.left, oldestAccepted);
            var right = prune(leftSide ? state.right : append(state.right, message), oldestAccepted);
            execution.transaction.put(name, key, new JoinState(left, right, streamTime));
            return results;
        }

        private List<StreamMessage> append(List<StreamMessage> records, StreamMessage message) {
            var copy = new ArrayList<>(records);
            copy.add(message);
            return List.copyOf(copy);
        }

        private List<StreamMessage> prune(List<StreamMessage> records, Instant oldestAccepted) {
            return records.stream().filter(record -> !record.eventTime().isBefore(oldestAccepted)).toList();
        }
    }

    private final class Execution {
        private final StreamStateRepository.Transaction transaction;

        private Execution(StreamStateRepository.Transaction transaction) {
            this.transaction = transaction;
        }
    }

    @FunctionalInterface
    private interface Operator {
        List<StreamMessage> apply(StreamMessage message, Execution execution);

        default void start() {
        }
    }

    private record Stage(String name, boolean stateful, Operator operator, String unsupportedMessage) {
        private Stage(String name, boolean stateful, Operator operator) {
            this(name, stateful, operator, null);
        }
    }

    private record Route(String sourceTopic, String sourceBindingId, Class<?> sourceValueType, List<Stage> stages) {
        private Route {
            stages = List.copyOf(stages);
        }

        private boolean accepts(StreamMessage message) {
            return sourceBindingId == null || sourceBindingId.equals(message.headers().get(EventType.HEADER));
        }

        private Route append(Stage stage) {
            var next = new ArrayList<>(stages);
            next.add(stage);
            return new Route(sourceTopic, sourceBindingId, sourceValueType, next);
        }
    }

    private record GlobalDeclaration(
            String name,
            String topic,
            Class<?> sourceType,
            String sourceBindingId,
            Class<?> storeType,
            GlobalUpdater updater
    ) {
        private boolean accepts(StreamMessage message) {
            return sourceBindingId == null || sourceBindingId.equals(message.headers().get(EventType.HEADER));
        }
    }

    private record ProcessorInvocation(StreamMessage input, List<StreamMessage> forwarded) {
    }

    private record WindowKey(Object key, long startMillis) {
    }

    private record AggregateState<V>(V value, List<String> correlationIds) {
        private AggregateState {
            correlationIds = List.copyOf(correlationIds);
        }
    }

    private record JoinState(List<StreamMessage> left, List<StreamMessage> right, Instant streamTime) {
        private JoinState {
            left = List.copyOf(left);
            right = List.copyOf(right);
        }

        private static JoinState empty() {
            return new JoinState(List.of(), List.of(), Instant.MIN);
        }
    }

    private static String validateRepartitionName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Repartition topic name must not be null or blank");
        }
        if (!name.matches("[A-Za-z0-9._-]+") || !name.matches(".*[A-Za-z0-9].*")) {
            throw new IllegalArgumentException(
                    "Invalid repartition topic name '%s'; use only letters, digits, '.', '_' or '-'".formatted(name));
        }
        return name;
    }

    private static String kebabCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1-$2")
                .toLowerCase(java.util.Locale.ROOT);
    }

    private static void requirePositive(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static Map<String, String> mergeHeaders(Map<String, String> left, Map<String, String> right) {
        var headers = new LinkedHashMap<>(left);
        right.forEach(headers::putIfAbsent);
        var firstIds = MessageTracing.decode(left.get(MessageTracing.CORRELATION_IDS_HEADER));
        var secondIds = MessageTracing.decode(right.get(MessageTracing.CORRELATION_IDS_HEADER));
        if (!firstIds.isEmpty() || !secondIds.isEmpty()) {
            headers.put(MessageTracing.CORRELATION_IDS_HEADER, MessageTracing.encode(MessageTracing.union(
                    firstIds,
                    secondIds,
                    TracingConfig.DEFAULT_MAX_CORRELATION_IDS)));
        }
        return Map.copyOf(headers);
    }

    private void declareStoreName(String name) {
        declarationsLock.lock();
        try {
            declaredStoreNames.add(name);
        } finally {
            declarationsLock.unlock();
        }
    }
}
