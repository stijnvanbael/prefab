---
id: TASK-096
title: Support Kafka Streams
status: To Do
assignee: []
created_date: '2026-03-27 11:14'
updated_date: '2026-03-27 12:03'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 14000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a high-level, platform-agnostic streaming DSL to Prefab that lets users define real-time stream processing pipelines at a functional level, inspired by the Kafka Streams DSL but not tied to it.

The guiding principle is Prefab's core philosophy: **start high, dive deep when you need to**. The API presents common streaming use-cases (filter, map, join, aggregate, branch, windowed aggregation) as first-class, composable operations. Under the hood the first backend will be Kafka Streams, but the abstraction layer must make it straightforward to add alternative backends (e.g. Project Reactor, Flink) later.

A typical pipeline definition should look like this:

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

And a more advanced stateful pipeline:

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

When the high-level API is not enough, users can escape to the raw backend DSL (e.g. the full Kafka Streams `KStream` API) without leaving the Prefab abstraction:

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

`StreamDefinition` beans are discovered automatically by Spring Boot auto-configuration, which builds and starts the topology. Serialization reuses the existing `DynamicSerializer` / `DynamicDeserializer` infrastructure and `SerializationRegistry`.
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
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation Approach Suggestions

The following approaches are proposed for the platform-agnostic Prefab Streams DSL. They are ordered from most to least recommended alignment with the stated vision.

---

### Approach 1 — Prefab Streams DSL with Pluggable Backends (Recommended)

**Core idea:** Define a `PrefabStreams` factory interface and a `StreamBuilder<K, V>` fluent API that is completely decoupled from Kafka. The first (and for now only) concrete backend is a `KafkaStreamsBackend` that translates DSL calls to Kafka Streams `KStream` / `KTable` calls. A `StreamDefinition<V>` is the terminal type that Spring Boot auto-configuration discovers, builds, and starts.

**DSL Layer (`core` or `streams` module):**

```java
// Entry point — injected as a Spring bean
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
    // Kafka Streams escape hatch:
    <R> StreamBuilder<K, R>          withKafkaStreams(Function<KStream<K,V>, KStream<K,R>> fn);
    StreamDefinition<V>              to(Class<V> outputType);
    StreamDefinition<V>              to(String topic);
}

public interface GroupedStreamBuilder<K, V> {
    <R> StreamBuilder<K, R> aggregate(Supplier<R> initializer, BiFunction<R, V, R> aggregator);
    <R> WindowedStreamBuilder<K, V>  windowedBy(Duration windowSize);
}
```

**Backend Interface:**

```java
public interface StreamingBackend {
    <K, V> NativeStreamBuilder<K, V> buildFrom(String topic, Class<V> type);
    void start(List<StreamDefinition<?>> definitions);
    void stop();
}
```

**Kafka Streams Backend:**

```java
public class KafkaStreamsBackend implements StreamingBackend {
    // translates each DSL call to the equivalent KStream call
    // builds Topology and starts KafkaStreams instance
    // uses DynamicSerializer / DynamicDeserializer via Serdes wrappers
}
```

**Auto-configuration:**

```java
@Configuration
@ConditionalOnClass(KafkaStreams.class)
public class StreamsAutoConfiguration {
    @Bean
    public StreamingBackend kafkaStreamsBackend(...) { ... }

    @Bean
    public PrefabStreams prefabStreams(StreamingBackend backend) { ... }

    // Discovers all StreamDefinition beans, wires them to the backend
    @Bean
    public StreamsLifecycle streamsLifecycle(List<StreamDefinition<?>> definitions, StreamingBackend backend) { ... }
}
```

**Feasibility:** High — clean layering; the DSL is pure Java interfaces; Kafka Streams backend is straightforward.  
**Effort:** Medium-High — design of the fluent API, Kafka Streams backend, auto-configuration, and test support.  
**Compatibility:** High — adds a new module and core interfaces; does not touch existing `KafkaPlugin`, `KafkaConsumerWriter`, or event annotation infrastructure.  
**Value:** Very High — platform-agnostic from day one; enables future backend swap with zero user-code changes.

---

### Approach 2 — Thin Wrapper Over Kafka Streams DSL (Kafka-Coupled)

**Core idea:** Skip the backend abstraction layer and directly expose a thin Spring-friendly wrapper around the Kafka Streams `StreamsBuilder`. Users get a fluent API that mirrors the Kafka Streams DSL closely but with Prefab type registration, serialization, and Spring lifecycle management taken care of.

```java
@Bean
KafkaStreamDefinition<ProcessedOrder> processedOrders(PrefabKafkaStreams streams) {
    return streams
        .from(OrderPlaced.class)                 // wraps StreamsBuilder.stream(topic, consumed)
        .filter(order -> order.total() != null)   // wraps KStream.filter()
        .mapValues(order -> new ProcessedOrder(order.orderId()))
        .toTopic(ProcessedOrder.class);           // wraps KStream.to(topic, produced)
}
```

Auto-configuration discovers `KafkaStreamDefinition` beans and registers them with a single `KafkaStreams` instance.

**Feasibility:** High — the simplest possible implementation; minimal new code.  
**Effort:** Low — mostly a thin delegate over `StreamsBuilder`; auto-configuration wires everything.  
**Compatibility:** High — no changes to existing infrastructure.  
**Value:** Medium — useful but permanently coupled to Kafka Streams; future migration to another backend requires user-code rewrites.

---

### Approach 3 — Project Reactor / Reactive Streams Backend

**Core idea:** Implement the DSL on top of Project Reactor (`Flux` / `Mono`) and reactive Kafka (`reactor-kafka`) instead of Kafka Streams. This gives access to the full reactive operator catalogue (buffer, window, groupBy, scan, merge, zip) while remaining platform-agnostic at the Prefab DSL level. The backend interface from Approach 1 is implemented as a `ReactorKafkaBackend`.

```java
// Under the hood:
// PrefabStreams.from(OrderPlaced.class)
// → Flux<OrderPlaced> via KafkaReceiver<String, OrderPlaced>
// → operators map directly to Reactor operators
// → terminal .to() subscribes and forwards to KafkaSender
```

**Feasibility:** Medium — reactive Kafka is mature but reactive stream management (backpressure, offsets, error handling) is more complex than Kafka Streams' built-in exactly-once and fault-tolerance.  
**Effort:** High — requires reactive Kafka dependency, offset management, and a different threading model.  
**Compatibility:** High — same `PrefabStreams` interface as Approach 1; users see no difference.  
**Value:** High — reactive operators are very expressive; fits naturally with reactive Spring WebFlux apps; but higher operational complexity than Kafka Streams for stateful operations.

---

### Approach 4 — Annotation Processor Plugin Generating Topology Builder Code

**Core idea:** Keep the annotation processor as the primary driver. Introduce `@Stream` annotation on a method whose return type (`StreamDefinition<T>`) describes the output. The annotation processor inspects the method body (using the compiler tree API) and generates a topology class. The user writes a "template" method body that is lifted into a generated class.

```java
// User code:
@Stream
public StreamDefinition<ProcessedOrder> processedOrders(OrderPlaced order) {
    return StreamDsl.filter(order.total().amount() > 0)
                   .map(o -> new ProcessedOrder(o.orderId(), o.total()));
}

// Generated (by KafkaStreamsPlugin):
public class ProcessedOrderTopologyBuilder {
    public void addTo(StreamsBuilder builder) {
        builder.stream("order-placed", ...)
               .filter((k, v) -> v.total().amount() > 0)
               .mapValues(v -> new ProcessedOrder(v.orderId(), v.total()))
               .to("processed-orders", ...);
    }
}
```

**Feasibility:** Low — reading and transforming method bodies via the compiler tree API is fragile and complex; not a pattern used elsewhere in Prefab.  
**Effort:** Very High — requires deep annotation processor changes; compiler tree API is notoriously difficult to use correctly across JDK versions.  
**Compatibility:** Low — significantly increases annotation processor complexity.  
**Value:** High if it works — but the fragility makes this approach risky.

---

### Approach 5 — Prefab Streams DSL with Spring Integration Backend

**Core idea:** Implement the DSL backend using Spring Integration's `MessageChannel` and `IntegrationFlow` abstractions instead of Kafka Streams. Spring Integration already provides channel adapters for Kafka, making it possible to define the same filter/map/aggregate operations without depending on Kafka Streams at all.

```java
// PrefabStreams.from(OrderPlaced.class)
// → KafkaMessageDrivenChannelAdapter
// → IntegrationFlowBuilder.filter(...).transform(...).handle(...)
// → KafkaProducerMessageHandler for output
```

**Feasibility:** Medium — Spring Integration Kafka adapters are mature; `IntegrationFlow` supports most DSL operations.  
**Effort:** High — new dependency; Spring Integration has its own learning curve; stateful operations (aggregation, windowing) are less elegant than Kafka Streams.  
**Compatibility:** High — Spring Integration is a Spring project; fits well with Spring Boot.  
**Value:** Medium — works well for stateless pipelines; stateful operations are verbose; no native exactly-once support.

---

### Summary Table

| Approach | Feasibility | Effort | Compatibility | Value |
|---|---|---|---|---|
| 1 — Prefab DSL + Pluggable Backends | High | Medium-High | High | Very High |
| 2 — Thin Kafka Streams Wrapper | High | Low | High | Medium |
| 3 — Project Reactor Backend | Medium | High | High | High |
| 4 — Annotation Processor Code Generation | Low | Very High | Low | High (if it works) |
| 5 — Spring Integration Backend | Medium | High | High | Medium |

**Recommended starting point:** Approach 1 — design the `PrefabStreams` DSL interfaces and implement the Kafka Streams backend. The clean separation means Approach 3 (Reactor) can be added as an alternative backend later without changing user code. Approach 2 is a valid quick-win if platform-agnosticism is not an immediate priority.
<!-- SECTION:NOTES:END -->
