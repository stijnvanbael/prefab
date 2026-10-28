---
id: TASK-300
title: Backend-neutral, low-ceremony streams topology test API
status: To Do
assignee: []
created_date: '2026-10-28 15:45'
labels:
  - streams
  - testing
dependencies: []
references:
  - streams/src/test/java/be/appify/prefab/streams/kafka/KafkaPrefabStreamsTopologyTest.java
  - test/src/main/java/be/appify/prefab/test/streams/kafka/KafkaTopologyTestBootstrap.java
  - streams/src/main/java/be/appify/prefab/streams/TopologyElement.java
  - streams/src/main/java/be/appify/prefab/streams/StreamDefinition.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Topology tests are unwieldy (`KafkaPrefabStreamsTopologyTest` is ~1700 lines, ~50 tests) and leak Kafka Streams: `TestInputTopic`/`TestOutputTopic`, `Topology.describe().toString()`, `nativeTopology()`, `KeyValueStore`, raw byte topics. Every test repeats bootstrap, build, run, input, output, pipe and assert. `TopologyElement.nativeTopology()` and `StreamDefinition` expose `org.apache.kafka.streams.Topology` from the backend-neutral `streams` package, and the bootstrap is duplicated in `streams/src/test` and `test/src/main`.

Goal: tests are written against a backend-neutral API with minimal setup, so the same suite can run against any `StreamBackend`.

### Target test shape

```java
@ExtendWith(TopologyTestExtension.class)
class FilterTest {
    @TestStreams PrefabStreams streams;
    @TestStreams TopologySession topology;

    @Test
    void filter_shouldDropRecordsNotMatchingPredicate() {
        streams.from(IncomingOrder.class)
                .filter(order -> order.customer().id().startsWith("A"))
                .to(ProcessedOrder.class);

        topology.input(IncomingOrder.class)
                .send(new IncomingOrder(orderId("o-1"), customer("Alice")))
                .send(new IncomingOrder(orderId("o-2"), customer("Bob")))
                .send(new IncomingOrder(orderId("o-3"), customer("Anna")));

        assertThat(topology.output(ProcessedOrder.class).values()).hasSize(2);
    }

    @Test
    void windowedAggregate_shouldUseStore() {
        countOrdersPerCustomer(streams, AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ZERO));

        assertThat(topology.inspect()).hasStore("windowed-aggregate-incoming-order-order-count");
    }

    @Test
    void windowedAggregate_withHoppingWindow_shouldEmitForEveryOverlappingWindow() {
        countOrdersPerCustomer(streams, hopping10s5s());

        topology.input(IncomingOrder.class)
                .send(order("o-1", "alice"), at(7_000))
                .send(order("o-2", "alice"), at(8_000));

        assertThat(topology.output(OrderCount.class).values())
                .containsExactlyInAnyOrder(count("alice", 1), count("alice", 1), count("alice", 2), count("alice", 2));
    }
}
```

Key points of the example:
- No base class and no explicit bootstrap. `TopologyTestExtension` is a JUnit 5 extension (`TestInstancePostProcessor` + `AfterEachCallback`; no Spring dependency). It injects fields annotated `@TestStreams` (`PrefabStreams` and `TopologySession`) with a fresh harness per test. The same types can also be injected as test method parameters (`ParameterResolver`).
- No try-with-resources: the `TopologySession` is lazy. The driver is created on the first `input()`/`output()`/`inspect()` call from everything declared on the injected `streams`. The harness is kept as an `AutoCloseable` resource in the per-test `ExtensionContext.Store`, so JUnit closes it after each test, also on failure.
- Declaring more topology after the session has started fails fast with a clear message.
- `Input.send(value[, Instant])` is fluent and takes the key from `Keyed.key()`; `at(millis)` is a shared helper.
- `Output.values()` reads all pending records; no `TestOutputTopic`.
- `topology.inspect()` returns a neutral `TopologyInspection` with AssertJ-style assertions (`hasStep`, `hasStore`, `hasGlobalStores(n)`, `hasRepartition`), replacing `describe().toString().contains(..)`.
- Tests needing a non-default app id or custom config use `@RegisterExtension` with `TopologyTestExtension.withAppId("...")`.
- Fixtures (`IncomingOrder`, `OrderCount`, `order(..)`, `count(..)`) live in a shared `TopologyFixtures`.
- No `org.apache.kafka` import in tests that are not Kafka-specific.
- Fixtures (`IncomingOrder`, `OrderCount`, `order(..)`, `count(..)`) live in a shared `TopologyFixtures`.
- No `org.apache.kafka` import in tests that are not Kafka-specific.
- Where the application context is used (Spring Boot example tests), the listener can additionally be used together with `@SpringBootTest`; to be verified during implementation.
### Scope
1. Neutral API in the `test` module: `TopologyTestHarness`, `TopologySession`, `Input`, `Output`, `RawOutput`, `TopologyInspection`; Kafka implementation wraps `TopologyTestDriver`. Remove duplicate bootstrap in `streams/src/test`.
2. `TopologyTestExtension` and `@TestStreams` (JUnit 5 field and parameter injection, lazy session start, automatic close via the `ExtensionContext` store), plus `TopologyFixtures`.
3. Remove Kafka types from `TopologyElement` / `StreamDefinition`; native access moves to a Kafka sub-interface (breaking change to be called out).
4. Split `KafkaPrefabStreamsTopologyTest` by operator; parameterize the repeated "stable step names" tests; keep Kafka-only tests (breakout adapters with `KStream`, wire-format byte checks, `StreamsConfigurationBootstrapTest`) in `streams/.../kafka`.
5. Backend-neutral tests form an abstract contract suite reusable by other backends.
6. Migrate `SalesTotalsTopologyTest` and `MeterDataTopologyTest`; update documentation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Neutral test API exposes no `org.apache.kafka` types in its public signatures.
- [ ] #2 A typical topology test needs no base class, no explicit harness bootstrap, no try-with-resources and no manual key duplication when piping input; harness and session are injected and closed by a JUnit 5 extension (`TopologyTestExtension`) without a Spring dependency.
- [ ] #3 Topology structure assertions use `TopologyInspection` instead of `describe().toString()`.
- [ ] #4 `TopologyElement` and `StreamDefinition` in the `streams` package no longer reference Kafka types.
- [ ] #5 `KafkaPrefabStreamsTopologyTest` is split into focused classes; repeated naming tests are parameterized; no test coverage is lost.
- [ ] #6 Kafka-specific tests remain in the kafka package and are clearly separated from the backend-neutral contract tests.
- [ ] #7 Example topology tests are migrated and the full Maven build passes.
- [ ] #8 Developer documentation describes the new test API.
<!-- AC:END -->
