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
class FilterTest extends TopologyTest {

    @Test
    void filter_shouldDropRecordsNotMatchingPredicate() {
        var topology = streams.from(IncomingOrder.class)
                .filter(order -> order.customer().id().startsWith("A"))
                .to(ProcessedOrder.class);

        try (var session = run(topology)) {
            session.input(IncomingOrder.class)
                    .send(new IncomingOrder(orderId("o-1"), customer("Alice")))
                    .send(new IncomingOrder(orderId("o-2"), customer("Bob")))
                    .send(new IncomingOrder(orderId("o-3"), customer("Anna")));

            assertThat(session.output(ProcessedOrder.class).values()).hasSize(2);
        }
    }

    @Test
    void windowedAggregate_shouldUseStore() {
        var topology = countOrdersPerCustomer(AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ZERO));

        assertThat(inspect(topology)).hasStore("windowed-aggregate-incoming-order-order-count");
    }

    @Test
    void windowedAggregate_withHoppingWindow_shouldEmitForEveryOverlappingWindow() {
        try (var session = run(countOrdersPerCustomer(hopping10s5s()))) {
            session.input(IncomingOrder.class)
                    .send(order("o-1", "alice"), at(7_000))
                    .send(order("o-2", "alice"), at(8_000));

            assertThat(session.output(OrderCount.class).values())
                    .containsExactlyInAnyOrder(count("alice", 1), count("alice", 1), count("alice", 2), count("alice", 2));
        }
    }
}
```

Key points of the example:
- `TopologyTest` base class (or JUnit extension) supplies a fresh `streams` and `run(...)`/`inspect(...)` per test; no `bootstrap()` line.
- `Input.send(value[, Instant])` is fluent and takes the key from `Keyed.key()`; `at(millis)` is a shared helper.
- `Output.values()` reads all pending records; no `TestOutputTopic`.
- `inspect(topology)` returns a neutral `TopologyInspection` with AssertJ-style assertions (`hasStep`, `hasStore`, `hasGlobalStores(n)`, `hasRepartition`), replacing `describe().toString().contains(..)`.
- Fixtures (`IncomingOrder`, `OrderCount`, `order(..)`, `count(..)`) live in a shared `TopologyFixtures`.
- No `org.apache.kafka` import in tests that are not Kafka-specific.

### Scope
1. Neutral API in the `test` module: `TopologyTestHarness`, `TopologySession`, `Input`, `Output`, `RawOutput`, `TopologyInspection`; Kafka implementation wraps `TopologyTestDriver`. Remove duplicate bootstrap in `streams/src/test`.
2. `TopologyTest` base class / JUnit extension and `TopologyFixtures`.
3. Remove Kafka types from `TopologyElement` / `StreamDefinition`; native access moves to a Kafka sub-interface (breaking change to be called out).
4. Split `KafkaPrefabStreamsTopologyTest` by operator; parameterize the repeated "stable step names" tests; keep Kafka-only tests (breakout adapters with `KStream`, wire-format byte checks, `StreamsConfigurationBootstrapTest`) in `streams/.../kafka`.
5. Backend-neutral tests form an abstract contract suite reusable by other backends.
6. Migrate `SalesTotalsTopologyTest` and `MeterDataTopologyTest`; update documentation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Neutral test API exposes no `org.apache.kafka` types in its public signatures.
- [ ] #2 A typical topology test needs no explicit harness bootstrap and no manual key duplication when piping input.
- [ ] #3 Topology structure assertions use `TopologyInspection` instead of `describe().toString()`.
- [ ] #4 `TopologyElement` and `StreamDefinition` in the `streams` package no longer reference Kafka types.
- [ ] #5 `KafkaPrefabStreamsTopologyTest` is split into focused classes; repeated naming tests are parameterized; no test coverage is lost.
- [ ] #6 Kafka-specific tests remain in the kafka package and are clearly separated from the backend-neutral contract tests.
- [ ] #7 Example topology tests are migrated and the full Maven build passes.
- [ ] #8 Developer documentation describes the new test API.
<!-- AC:END -->
