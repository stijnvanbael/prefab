---
id: TASK-096
title: Support Kafka Streams
status: To Do
assignee: []
created_date: '2026-03-27 11:14'
updated_date: '2026-03-27 11:14'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 14000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Extend Prefab to support Kafka Streams, enabling annotation-driven stream processing in the spirit of the framework.

Kafka Streams is a Java client library that turns Kafka topics into real-time processing pipelines: filtering, transforming, joining, aggregating, and materialising domain events into fault-tolerant local state stores. It runs inside the application process — no separate cluster component — and integrates natively with Spring Boot.

Adding Kafka Streams support would allow Prefab users to define stream processing topologies through annotations — the same way they already define REST endpoints, persistence, and plain Kafka producers/consumers — so all the boilerplate topology and configuration code is generated rather than written by hand.

The implementation should follow the existing Prefab patterns:
- A new annotation-processor plugin (`KafkaStreamsPlugin`) discovers annotated processor methods and generates topology builder classes.
- Runtime support classes live in the `core` module and auto-configure via Spring Boot.
- The plugin module follows the same structure as the existing `kafka`, `sns-sqs`, and `pubsub` modules.
- A working `examples/kafka-streams` module demonstrates the feature end-to-end.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Multiple implementation approaches are identified, described, and compared
- [ ] #2 Approaches are ranked by feasibility, effort, compatibility with the existing Prefab architecture, and value delivered
- [ ] #3 A preferred approach is selected and documented with rationale
- [ ] #4 At least one new annotation (e.g. `@StreamProcessor`) is defined in the core module to drive code generation
- [ ] #5 A `KafkaStreamsPlugin` is added to the `kafka` module (or a new `kafka-streams` module) that generates Kafka Streams topology builder classes from the annotated methods
- [ ] #6 Core runtime support is added (auto-configuration, topology registration, serializer/deserializer wiring)
- [ ] #7 Generated topology wires correctly with the existing Kafka infrastructure (`KafkaConfiguration`, `DynamicSerializer`, `DynamicDeserializer`)
- [ ] #8 Test support is provided (e.g. `TopologyTestDriver`-based assertion helpers in the `test` module)
- [ ] #9 An `examples/kafka-streams` module demonstrates the feature end-to-end with integration tests
- [ ] #10 Existing Kafka integration tests continue to pass
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation Approach Suggestions

The following approaches are proposed for supporting Kafka Streams in Prefab. They are ordered from most to least aligned with the existing framework design.

---

### Approach 1 — Annotation-Driven Topology Generation (Recommended)

**Core idea:** Introduce a `@StreamProcessor` annotation (analogous to `@EventHandler`) placed on service methods. The annotation processor generates a `KafkaStreamsTopologyBuilder` class per aggregate that builds the `Topology` object and registers it with Spring Boot auto-configuration.

**How it works:**
1. User annotates a method with `@StreamProcessor(inputTopic = "orders", outputTopic = "processed-orders")`.
2. `KafkaStreamsPlugin` detects these methods and generates a `${Aggregate}TopologyBuilder` that calls the annotated method inside a `KStream.mapValues()` (or `filter`, `flatMapValues`, etc.) chain.
3. A generated `KafkaStreamsConfiguration` bean builds and starts one `KafkaStreams` instance per topology.
4. Serialization reuses `DynamicSerializer` / `DynamicDeserializer` already present in the `core` module.

**Sub-annotations for common operations:**
- `@StreamFilter` — generates a `KStream.filter()` step from a `Predicate` method.
- `@StreamJoin` — generates a `KStream.join()` linking two event types by a shared key.
- `@StreamAggregate` — generates a `KTable` materialised from a `Reducer` or `Aggregator` method.

**Feasibility:** High — fits naturally into the existing plugin model; no changes to the annotation processor core.  
**Effort:** Medium — one new plugin class, two or three writer classes, a handful of core configuration classes.  
**Compatibility:** High — parallel to the existing `KafkaPlugin`; does not touch existing Kafka producer/consumer code.  
**Value:** High — covers the most common Kafka Streams patterns declaratively.

---

### Approach 2 — CQRS Read-Model Projections via KTable

**Core idea:** Introduce a `@Projection` annotation on a domain record. The annotation processor generates a Kafka Streams topology that consumes domain events and materialises them into a `KTable`-backed read model. A generated Spring Data-like repository interface queries the `ReadOnlyKeyValueStore` via Kafka Streams Interactive Queries.

**How it works:**
1. User defines `record OrderSummary(OrderId id, String status, Money total) {}` and annotates it with `@Projection(from = {OrderCreated.class, OrderShipped.class})`.
2. `KafkaStreamsPlugin` generates a topology that folds the listed event types into a `KTable<OrderId, OrderSummary>` using a generated reducer.
3. A generated `OrderSummaryRepository` exposes `findById()` and `findAll()` backed by `ReadOnlyKeyValueStore`.
4. Optional: the existing HTTP layer generates read endpoints backed by the projection repository.

**Feasibility:** Medium — requires new annotation and new code generation path; Interactive Queries add runtime complexity (multi-instance routing).  
**Effort:** High — annotation processor changes, new repository abstraction, optional HTTP layer integration.  
**Compatibility:** Medium — read model concept is new to Prefab; may require framework-level extension points.  
**Value:** Very High — enables CQRS with eventually-consistent read models without a separate database.

---

### Approach 3 — Stateful Process Managers / Sagas via Kafka Streams

**Core idea:** Introduce a `@ProcessManager` annotation on a class that orchestrates a multi-step business process. The annotation processor generates a stateful Kafka Streams topology that correlates multiple event types (stored in a `KTable` or `WindowedStore`) and emits a command or outcome event when the saga reaches a terminal state.

**How it works:**
1. User defines `@ProcessManager` class with `@On(OrderPlaced.class)` and `@On(PaymentReceived.class)` handler methods.
2. `KafkaStreamsPlugin` generates a topology with a `KTable` keyed by process ID that accumulates state as events arrive.
3. When a terminal condition is met (e.g., both events received), the topology emits a `FulfillmentStarted` event to an output topic.

**Feasibility:** Medium — process manager patterns are well-defined; the challenge is generating correct stateful topology code from annotations.  
**Effort:** Very High — complex code generation, requires careful handling of state store schema and upgrade paths.  
**Compatibility:** High — complements the existing event-driven model.  
**Value:** High — eliminates the most complex event-driven boilerplate.

---

### Approach 4 — Enhanced Kafka Consumers with Windowed Operations

**Core idea:** Replace (or supplement) the existing simple Kafka consumers generated by `KafkaPlugin` with Kafka Streams-based consumers that support time-window aggregations and deduplication. No new annotation is needed; `@EventHandler` gains an optional `window` attribute.

**How it works:**
1. User adds `@EventHandler(platform = KAFKA, window = @TumblingWindow(size = 1, unit = MINUTES))` to an event handler method.
2. `KafkaStreamsPlugin` detects the `window` attribute and generates a `KStream.windowedBy(...).aggregate(...)` topology instead of a plain `KafkaListener`.
3. The window result is forwarded to the handler method as a `List<T>` of accumulated events.

**Feasibility:** High — incremental extension of the existing Kafka consumer model.  
**Effort:** Low-Medium — extends existing `KafkaConsumerWriter` with an optional windowing branch.  
**Compatibility:** High — non-breaking; plain `@EventHandler` methods are unaffected.  
**Value:** Medium — solves a real use case (deduplication, micro-batching) with minimal disruption.

---

### Approach 5 — Interactive Queries REST Layer

**Core idea:** Generate REST endpoints backed by Kafka Streams Interactive Queries, allowing stateless HTTP clients to query the materialised state of a Kafka Streams application without a separate database.

**How it works:**
1. User annotates a `KTable` materialisation (from Approach 1 or 2) with `@QueryableStore`.
2. `KafkaStreamsPlugin` generates a `GetById` and `GetList` REST controller backed by `ReadOnlyKeyValueStore` rather than a JPA/JDBC repository.
3. For clustered deployments, generated code includes a `KafkaStreams.metadataForKey()` redirect to the correct instance.

**Feasibility:** Low-Medium — standalone REST layer over Interactive Queries requires multi-instance routing and host discovery.  
**Effort:** High — requires integration with the HTTP layer and network-level coordination.  
**Compatibility:** Medium — the HTTP layer currently assumes a Spring Data repository; plugging in a store requires new abstractions.  
**Value:** High — enables zero-database read queries in event-sourced systems; best combined with Approach 2.

---

### Summary Table

| Approach | Feasibility | Effort | Compatibility | Value |
|---|---|---|---|---|
| 1 — Annotation-Driven Topology | High | Medium | High | High |
| 2 — CQRS Projections via KTable | Medium | High | Medium | Very High |
| 3 — Process Managers / Sagas | Medium | Very High | High | High |
| 4 — Windowed Event Consumers | High | Low-Medium | High | Medium |
| 5 — Interactive Queries REST | Low-Medium | High | Medium | High |

**Recommended starting point:** Approach 1, delivering core annotation-driven topology generation. Approaches 2 and 4 can be implemented as follow-up tasks once the base infrastructure is in place.
<!-- SECTION:NOTES:END -->
