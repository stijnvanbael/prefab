---
id: TASK-096
title: Prefab Streams DSL
status: To Do
assignee: []
created_date: '2026-03-27 11:14'
updated_date: '2026-03-27 17:16'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 14000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a high-level, platform-agnostic streaming DSL to Prefab that lets users define real-time stream processing pipelines at a functional level, inspired by the Kafka Streams DSL but not tied to it.

The guiding principle is Prefab's core philosophy: **start high, dive deep when you need to**. The API presents common streaming use-cases (filter, map, join, aggregate, branch, windowed aggregation, and custom stateful processing) as first-class, composable operations. Multiple backends are supported — Kafka Streams for stateful, fault-tolerant pipelines; Google Cloud Pub/Sub; and AWS SNS/SQS for lighter-weight flows — and users can swap backends without changing pipeline code.

A typical stateless pipeline:

```java
@Bean
StreamDefinition<ProcessedOrder> processedOrders(PrefabStreams streams) {
    return streams
        .from(OrderPlaced.class)
        .filter(order -> order.total().amount().compareTo(BigDecimal.ZERO) > 0)
        .map(order -> new ProcessedOrder(order.orderId(), order.total(), OrderStatus.PENDING))
        .to(ProcessedOrder.class);
}
```

A stateful pipeline with built-in aggregation:

```java
@Bean
StreamDefinition<OrderSummary> orderSummaries(PrefabStreams streams) {
    return streams
        .from(OrderPlaced.class)
        .merge(streams.from(OrderShipped.class))
        .groupBy(OrderId.class, OrderEvent::orderId)
        .aggregate(
            OrderSummary::empty,
            (summary, event) -> switch (event) {
                case OrderPlaced e  -> summary.withStatus("PLACED").withTotal(e.total());
                case OrderShipped e -> summary.withStatus("SHIPPED");
            }
        )
        .to(OrderSummary.class);
}
```

A custom stateful processor plugged into a pipeline using a `StateStore` — without touching Kafka Streams directly:

```java
@Bean
StreamProcessor<OrderId, OrderPlaced, EnrichedOrder> enrichmentProcessor() {
    return (key, event, context) -> {
        CustomerProfile profile = context.store(customerStore()).get(event.customerId());
        return new EnrichedOrder(event, profile);
    };
}

@Bean
StateStore<CustomerId, CustomerProfile> customerStore() {
    return StateStore.keyValue("customer-profiles", CustomerId.class, CustomerProfile.class);
}

@Bean
StreamDefinition<EnrichedOrder> enrichedOrders(
        PrefabStreams streams,
        StreamProcessor<OrderId, OrderPlaced, EnrichedOrder> enrichmentProcessor,
        StateStore<CustomerId, CustomerProfile> customerStore) {
    return streams
        .from(OrderPlaced.class)
        .process(enrichmentProcessor, customerStore)
        .to(EnrichedOrder.class);
}
```

`StateStore` implementations are provided by each backend: Kafka Streams uses in-process fault-tolerant stores; Pub/Sub and SNS/SQS pipelines use pluggable external stores (Redis, DynamoDB, etc.) so stateful operations work identically at the DSL level regardless of backend.

When the high-level API is not enough, users can escape to the raw backend DSL without leaving Prefab:

```java
@Bean
StreamDefinition<Alert> fraudAlerts(PrefabStreams streams) {
    return streams
        .from(OrderPlaced.class)
        .withKafkaStreams(kStream ->
            kStream
                .groupBy((k, v) -> v.customerId().id())
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
                .count()
                .toStream()
                .filter((k, count) -> count > 10)
                .map((k, count) -> KeyValue.pair(k.key(), new Alert(k.key(), count)))
        )
        .to(Alert.class);
}
```

`StreamDefinition` beans are discovered automatically by Spring Boot auto-configuration. Serialization reuses the existing `DynamicSerializer` / `DynamicDeserializer`, `PubSubSerializer` / `PubSubDeserializer`, and `SnsSerializer` / `SqsDeserializer` infrastructure via `SerializationRegistry`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Multiple implementation approaches for the DSL abstraction layer are identified, described, and compared
- [ ] #2 Approaches are ranked by feasibility, effort, compatibility with existing Prefab architecture, and value delivered
- [ ] #3 A preferred approach is selected and documented with rationale
- [ ] #4 A platform-agnostic `PrefabStreams` DSL is implemented in a `streams` module (or as an extension of the `kafka` module) with at least: `from`, `filter`, `map`, `flatMap`, `merge`, `branch`, `groupBy`, `aggregate`, `windowedAggregate`, `join`, and `to` operations
- [ ] #5 A Kafka Streams backend wires the DSL operations to `KStream` / `KTable` / `KGroupedStream` calls and integrates with `KafkaConfiguration`, `DynamicSerializer`, and `DynamicDeserializer`
- [ ] #6 An escape-hatch API (e.g. `.withKafkaStreams(Function<KStream<K,V>, KStream<K,V>>)`) allows dropping to the raw Kafka Streams DSL for advanced use cases
- [ ] #7 `StreamDefinition` beans declared as Spring `@Bean` methods are auto-discovered and started by a `StreamsAutoConfiguration` class
- [ ] #8 Test support is provided: an in-memory backend (or `TopologyTestDriver`-based helper) allows unit-testing stream pipelines without a running Kafka broker
- [ ] #9 An `examples/kafka-streams` module demonstrates common patterns (stateless transform, stateful aggregation, join, windowed aggregation, escape-hatch) with integration tests
- [ ] #10 Existing Kafka producer/consumer integration tests continue to pass
- [ ] #11 A StreamProcessor<K, V, R> abstraction is defined that encapsulates custom stateful or stateless per-record logic, can be plugged into a pipeline with .process(processor) or .process(processor, stateStore...), and has no dependency on any specific messaging platform
- [ ] #12 A StateStore<K, V> abstraction is defined (keyValue and windowed variants) with at least three implementations: in-memory (for tests and local dev), Kafka Streams-backed (fault-tolerant, in-process), and an interface contract that allows Redis-, DynamoDB-, or Firestore-backed implementations for non-Kafka backends
- [ ] #13 The Pub/Sub (Google Cloud) backend design is documented: how subscriptions map to from(), topics to to(), how ordering keys become stream keys, which DSL operations run in-process, and which stateful operations require a pluggable StateStore
- [ ] #14 The SNS/SQS (AWS) backend design is documented: how SQS queues map to from(), SNS topics to to(), how FIFO message group IDs map to stream keys, which stateful operations require an external StateStore, and how fan-out via SNS maps to merge()
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
<!-- SECTION:PLAN:BEGIN -->
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation Approach Suggestions

The following approaches are proposed for the platform-agnostic Prefab Streams DSL. They are ordered from most to least recommended alignment with the stated vision.

---

### Approach 1 — Prefab Streams DSL with Pluggable Backends (Recommended)

**Core idea:** Define a `PrefabStreams` factory interface and a `StreamBuilder<K, V>` fluent API completely decoupled from any messaging platform. The first concrete backend is a `KafkaStreamsBackend`; additional backends for Google Cloud Pub/Sub and AWS SNS/SQS follow the same interface. A `StreamDefinition<V>` is the terminal type that Spring Boot auto-configuration discovers, builds, and starts.

**DSL Layer (`core` or `streams` module):**

```java
// Entry point
public interface PrefabStreams {
    <V> StreamBuilder<String, V> from(Class<V> eventType);
}

// Fluent builder
public interface StreamBuilder<K, V> {
    StreamBuilder<K, V>              filter(Predicate<V> predicate);
    <R> StreamBuilder<K, R>          map(Function<V, R> mapper);
    <R> StreamBuilder<K, R>          flatMap(Function<V, Iterable<R>> mapper);
    StreamBuilder<K, V>              merge(StreamBuilder<K, V> other);
    GroupedStreamBuilder<K, V>       groupBy(Function<V, K> keyExtractor);
    BranchBuilder<K, V>              branch(Predicate<V> predicate);
    <R> StreamBuilder<K, R>          process(StreamProcessor<K, V, R> processor,
                                              StateStore<?, ?>... stores);
    // Kafka Streams escape hatch:
    <R> StreamBuilder<K, R>          withKafkaStreams(Function<KStream<K,V>, KStream<K,R>> fn);
    StreamDefinition<V>              to(Class<V> outputType);
    StreamDefinition<V>              to(String topic);
}

public interface GroupedStreamBuilder<K, V> {
    <R> StreamBuilder<K, R>          aggregate(Supplier<R> initializer,
                                               BiFunction<R, V, R> aggregator);
    WindowedStreamBuilder<K, V>      windowedBy(Duration windowSize);
}
```

**StreamProcessor and StateStore abstractions:**

```java
// Platform-agnostic per-record processor; may be stateless or access state stores
@FunctionalInterface
public interface StreamProcessor<K, V, R> {
    R process(K key, V value, ProcessorContext<K> context);
}

// Context provided at processing time — allows store access, timestamp introspection
public interface ProcessorContext<K> {
    <StoreK, StoreV> ReadWriteStore<StoreK, StoreV> store(StateStore<StoreK, StoreV> definition);
    Instant timestamp();
    K key();
}

// A store *definition* (not the store itself — the backend manages the lifecycle)
public interface StateStore<K, V> {
    String name();
    Class<K> keyType();
    Class<V> valueType();

    static <K, V> StateStore<K, V> keyValue(String name, Class<K> k, Class<V> v) { ... }
    static <K, V> StateStore<K, V> windowed(String name, Class<K> k, Class<V> v,
                                            Duration window) { ... }
}

// Unified read/write access regardless of backing store technology
public interface ReadWriteStore<K, V> {
    V get(K key);
    void put(K key, V value);
    void delete(K key);
    Iterator<KeyValue<K, V>> all();
    Iterator<KeyValue<K, V>> range(K from, K to);
}
```

**Backend Interface:**

```java
public interface StreamingBackend {
    <K, V> NativeStreamBuilder<K, V> buildFrom(String topic, Class<V> type);
    <K, V> ReadWriteStore<K, V>      resolveStore(StateStore<K, V> definition);
    void start(List<StreamDefinition<?>> definitions);
    void stop();
}
```

**Kafka Streams Backend:** translates `StreamProcessor` → `Transformer`, `StateStore` → in-process `KeyValueStore` / `WindowStore`. Uses `DynamicSerializer` / `DynamicDeserializer` via `Serdes` wrappers.

**Pub/Sub Backend:** maps `from()` to `PubSubUtil.subscribe()`, `to()` to `PubSubTemplate.publish()`. Resolves `StateStore` to a user-supplied external store bean (Redis, Firestore, etc.).

**SNS/SQS Backend:** maps `from()` to `SqsUtil` subscription, `to()` to `SnsClient.publish()`. Resolves `StateStore` to a user-supplied external store bean (DynamoDB, ElastiCache, etc.).

**Auto-configuration:**

```java
@Configuration
@ConditionalOnClass(KafkaStreams.class)
public class KafkaStreamsAutoConfiguration {
    @Bean StreamingBackend kafkaStreamsBackend(...) { ... }
    @Bean PrefabStreams prefabStreams(StreamingBackend backend) { ... }
    @Bean StreamsLifecycle streamsLifecycle(List<StreamDefinition<?>> defs,
                                            StreamingBackend backend) { ... }
}
```

**Feasibility:** High — clean layering; pure Java interfaces; all three backends reuse existing serialization infrastructure.
**Effort:** High — three backends, `StreamProcessor`/`StateStore` abstraction layer, auto-configuration per backend, test support.
**Compatibility:** High — adds new modules; does not touch existing Kafka producer/consumer, Pub/Sub subscriber, or SNS/SQS infrastructure.
**Value:** Very High — platform-agnostic from day one; processor/store wrappers eliminate the need for backend-specific escape hatches for most advanced use cases.

---

### Approach 2 — Thin Wrapper Over Kafka Streams DSL (Kafka-Coupled)

**Core idea:** Skip the backend abstraction layer and directly expose a thin Spring-friendly wrapper around the Kafka Streams `StreamsBuilder`. Users get a fluent API that mirrors the Kafka Streams DSL closely but with Prefab type registration, serialization, and Spring lifecycle management handled automatically.

```java
@Bean
KafkaStreamDefinition<ProcessedOrder> processedOrders(PrefabKafkaStreams streams) {
    return streams
        .from(OrderPlaced.class)
        .filter(order -> order.total() != null)
        .mapValues(order -> new ProcessedOrder(order.orderId()))
        .toTopic(ProcessedOrder.class);
}
```

**Feasibility:** High — simplest possible implementation.
**Effort:** Low — thin delegate over `StreamsBuilder`; auto-configuration wires everything.
**Compatibility:** High — no changes to existing infrastructure.
**Value:** Medium — useful but permanently coupled to Kafka Streams; Pub/Sub and SNS/SQS users need a different API; no processor/store abstraction.

---

### Approach 3 — Project Reactor / Reactive Streams Backend

**Core idea:** Implement the DSL on top of Project Reactor (`Flux` / `Mono`) and reactive Kafka (`reactor-kafka`). The backend interface from Approach 1 is implemented as a `ReactorKafkaBackend`; `StateStore` maps to a Reactor-friendly store (Redis via `ReactiveRedisTemplate`).

**Feasibility:** Medium — reactive Kafka is mature; stateful operations require careful backpressure and offset management.
**Effort:** High — new dependency; reactive threading model differs significantly from Kafka Streams.
**Compatibility:** High — same `PrefabStreams` interface as Approach 1.
**Value:** High — fits naturally with Spring WebFlux apps; expressive reactive operators; but higher operational complexity for stateful operations.

---

### Approach 4 — Annotation Processor Plugin Generating Topology Builder Code

**Core idea:** Introduce `@Stream` on a method; the annotation processor reads the method body via the compiler tree API and generates a topology builder class.

**Feasibility:** Low — reading and transforming method bodies is fragile and JDK-version-sensitive.
**Effort:** Very High — deep annotation processor changes.
**Compatibility:** Low — significantly increases annotation processor complexity.
**Value:** High if it works — fragility makes this approach risky.

---

### Approach 5 — Prefab Streams DSL with Spring Integration Backend

**Core idea:** Back the DSL with Spring Integration `IntegrationFlow` and Kafka/Pub/Sub/SQS channel adapters.

**Feasibility:** Medium — Spring Integration adapters are mature; stateful operations are verbose.
**Effort:** High — new dependency; less elegant for windowed/join operations.
**Compatibility:** High — fits Spring Boot well.
**Value:** Medium — good for stateless pipelines; stateful operations less natural than Kafka Streams.

---

### Platform Capability Matrix

Not all DSL operations can be supported natively by every backend. The matrix below shows which operations are natively supported and which require a pluggable `StateStore`.

| DSL Operation | Kafka Streams | Google Pub/Sub | AWS SNS/SQS |
|---|---|---|---|
| `from(Class)` | ✅ Native (topic) | ✅ Native (subscription) | ✅ Native (SQS queue) |
| `filter(Predicate)` | ✅ Native | ✅ In-process | ✅ In-process |
| `map(Function)` | ✅ Native | ✅ In-process | ✅ In-process |
| `flatMap(Function)` | ✅ Native | ✅ In-process | ✅ In-process |
| `merge(StreamBuilder)` | ✅ Native | ✅ Multiple subscriptions | ✅ Multiple queues |
| `branch(Predicate)` | ✅ Native | ✅ In-process | ✅ In-process |
| `to(Class)` | ✅ Native (topic) | ✅ Native (Pub/Sub topic) | ✅ Native (SNS topic) |
| `process(Processor)` | ✅ Via Transformer | ✅ In-process | ✅ In-process |
| `process(Processor, StateStore)` | ✅ In-process store | 🔌 External store required | 🔌 External store required |
| `groupBy(...).aggregate(...)` | ✅ Native (KTable) | 🔌 External store required | 🔌 External store required |
| `windowedAggregate(...)` | ✅ Native (WindowStore) | 🔌 External store + timer | 🔌 External store + timer |
| `join(...)` | ✅ Native (KStream-KTable) | 🔌 External store required | 🔌 External store required |
| `.withKafkaStreams(fn)` | ✅ Full access | ❌ N/A | ❌ N/A |

**Legend:** ✅ Native | �� Requires pluggable `StateStore` | ❌ Not supported

The `StateStore` abstraction bridges the gap: on Kafka Streams the backend provides in-process fault-tolerant stores automatically; on Pub/Sub and SNS/SQS the user provides an external store bean that the backend resolves at startup, failing fast with a descriptive error if none is found.

---

### Pub/Sub Backend Design

**Source mapping:**
- `from(OrderPlaced.class)` → creates/reuses a Pub/Sub subscription via `PubSubUtil.subscribe()` (existing)
- Deserialization via `PubSubDeserializer` (existing)
- Pub/Sub ordering key from the message attribute becomes the stream record key

**Sink mapping:**
- `to(ProcessedOrder.class)` → publishes to a Pub/Sub topic via `PubSubTemplate` (existing)
- Serialization via `PubSubSerializer` (existing)

**Stateless operations:** Run in the subscriber thread pool (same pattern as existing `PubSubSubscriberWriter`-generated code).

**Stateful operations:** Backend resolves `StateStore` beans at startup. If no matching bean is found, startup fails with a descriptive error. Recommended implementations:

- `RedisPrefabStateStore` — backed by Spring Data Redis
- `FirestorePrefabStateStore` — backed by Google Cloud Firestore
- `InMemoryStateStore` — for local dev and testing

**Error handling:** Reuses the existing Pub/Sub dead-lettering and retry infrastructure from `PubSubUtil`.

**Example — stateful enrichment on Pub/Sub:**

```java
@Bean
StateStore<CustomerId, CustomerProfile> customerStore(RedisTemplate<String, String> redis) {
    return new RedisPrefabStateStore<>("customer-profiles",
        CustomerId.class, CustomerProfile.class, redis);
}
```

---

### SNS/SQS Backend Design

**Source mapping:**
- `from(OrderPlaced.class)` → consumes from an SQS queue via `SqsUtil` / `SqsSubscriptionRequest` (existing)
- Deserialization via `SqsDeserializer` (existing)
- SQS FIFO message group ID becomes the stream record key (enables per-key ordering)

**Sink mapping:**
- `to(ProcessedOrder.class)` → publishes to an SNS topic via `SnsClient` (existing)
- Serialization via `SnsSerializer` (existing)

**Fan-out / fan-in:** SNS fan-out (one topic → many SQS queues) maps naturally to `merge()` on the consumer side and is already handled by the existing SNS/SQS subscription infrastructure.

**Stateful operations:** Same pattern as Pub/Sub. Recommended external store for AWS deployments:

- `DynamoDbPrefabStateStore` — backed by AWS DynamoDB
- `ElastiCachePrefabStateStore` — backed by ElastiCache (Redis)
- `InMemoryStateStore` — for local dev and testing

**Error handling:** Reuses the existing DLT pattern from the `sns-sqs` module.

**Example — saga with stateful SNS/SQS pipeline:**

```java
@Bean
StreamDefinition<FulfillmentTriggered> fulfillmentStream(
        PrefabStreams streams,
        StreamProcessor<OrderId, OrderEvent, FulfillmentTriggered> sagaProcessor,
        StateStore<OrderId, OrderState> orderStore) {
    return streams
        .from(OrderPlaced.class)
        .merge(streams.from(PaymentReceived.class))
        .process(sagaProcessor, orderStore)
        .to(FulfillmentTriggered.class);
}

@Bean
StateStore<OrderId, OrderState> orderStore(DynamoDbClient dynamo) {
    return new DynamoDbPrefabStateStore<>("order-saga-state",
        OrderId.class, OrderState.class, dynamo);
}
```

---

### Summary Table

| Approach | Feasibility | Effort | Compatibility | Value |
|---|---|---|---|---|
| 1 — Prefab DSL + Pluggable Backends | High | High | High | Very High |
| 2 — Thin Kafka Streams Wrapper | High | Low | High | Medium |
| 3 — Project Reactor Backend | Medium | High | High | High |
| 4 — Annotation Processor Code Generation | Low | Very High | Low | High (if it works) |
| 5 — Spring Integration Backend | Medium | High | High | Medium |

**Recommended starting point:** Approach 1 — design the `PrefabStreams` / `StreamProcessor` / `StateStore` interfaces and implement the Kafka Streams backend first. The Pub/Sub and SNS/SQS backends follow the same `StreamingBackend` interface and reuse the existing serialization infrastructure, so they can be delivered as follow-up tasks without changing user-facing pipeline code.
<!-- SECTION:NOTES:END -->

<!-- SECTION:PLAN:END -->

<!-- SECTION:PLAN:END -->
