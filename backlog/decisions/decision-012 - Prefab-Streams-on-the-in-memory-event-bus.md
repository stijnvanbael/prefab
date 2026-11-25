---
id: decision-012
title: Prefab Streams on the in-memory event bus
date: '2026-11-24'
status: accepted
related_tasks:
  - TASK-374
  - TASK-175
  - TASK-342
---

## Context

`@IntegrationTest` runs in memory by default (decision-011), but Prefab Streams topologies needed a broker, so the
in-memory showcase variant listed `STREAMS` as unsupported and applications with streams needed Testcontainers.
Decision-008 introduced the in-process engine (`prefab-streams-engine`) with source, sink and state SPIs. Its test
harness already runs topologies synchronously in memory; what is missing is the binding to the application's events.

## Decision

1. **Run the in-process engine on the in-memory bus.** `prefab-test` gets `InMemoryStreamsAutoConfiguration`, active in
   in-memory mode when `prefab-streams-engine` is on the classpath. `prefab-streams-engine` is a `provided`
   dependency of `prefab-test`, like the other optional integrations, and the listener that resets stream state only
   touches a type of `prefab-test` itself, so applications without streams do not load engine classes.
2. **Sources subscribe to bus topics, sinks publish on them.** A source is a bus subscription on the topic that the
   `EventRegistry` gives the event type, so a topology sees the events that aggregates publish and the generated
   listeners see the output of a topology. The sink goes through `InMemoryEventBus.publish(event, topic, headers)`,
   which encodes with the platform serializer and carries the engine's message identity and event time as transport
   headers. A topic named by `to("topic")` is registered as an alias in the `EventRegistry`, as the Pub/Sub backend
   does.
3. **Topics.** One bus topic per source and sink topic. Repartition and changelog topics do not exist: the engine keeps
   its state in the repository and `repartition()` is a no-op, in every backend.
4. **State and time.** State, inbox and outbox are in an `InMemoryStreamStateRepository`, cleared before each test like
   the aggregates (`StreamStateReset`). The bus has no timestamps: an application event gets the moment of delivery as
   its event time; a message published by a stream keeps its event time. Windows and joins therefore work on delivery
   time for application events, which suits tests that run in a few milliseconds. Tests that need controlled time use
   `TopologyTestExtension`, which lets the test choose event times.
5. **Ordering and acknowledgement.** The bus delivers synchronously and in publication order, so sources are reported as
   ordered. A source requires the engine to acknowledge, as the other backends do. A failing handler is recorded by the
   bus and fails the test.
6. **`KAFKA_STREAMS_BREAKOUT` stays unsupported in memory.** `breakout` hands over the Kafka Streams API
   (`KStream` and `StreamsBuilder`), so emulating it means embedding Kafka Streams with a mock broker, which is the
   dependency this work removes. The engine already refuses `breakout` at start with a clear message, and
   `showcase-inmemory` keeps the capability in its gap file. Tests of a breakout topology use
   `@IntegrationTest(containers = true)` with `prefab-streams-kafka`.
7. **Tombstones are not carried.** The bus transports events and no application event is a tombstone, so sources never
   receive one and the sink refuses a record without a value with a message. A global store that must see deletions
   is tested with `TopologyTestExtension`, which can send tombstones.
8. **No Kafka Streams in memory.** The Kafka Streams auto-configuration (`StreamsConfiguration`) is dropped in
   in-memory mode, like the other broker auto-configurations, so an application that has `prefab-streams-kafka` for
   production runs its topologies on the engine in memory tests.

## Consequences

- Applications with streams get container-free integration tests by adding `prefab-streams-engine` in test scope.
- A topology that behaves differently on Kafka Streams and on the engine (per-partition stream time, repartitioning
  semantics, breakout) is verified on containers. The engine is the same code that runs on Pub/Sub and SNS/SQS, so the
  in-memory run exercises production code of those backends and an approximation of Kafka's.
- Event time for application events is delivery time. Showcase windows (hourly, with a grace period) are insensitive
  to this; time-sensitive logic belongs in topology tests.
