---
id: TASK-374
title: Prefab Streams on the in-memory test bus
status: Done
assignee: []
created_date: '2026-11-24 09:00'
labels:
  - streams
  - test
updated_date: '2026-11-24 17:00'
dependencies:
  - TASK-175
priority: medium
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`@IntegrationTest` runs in memory by default (TASK-175), but Prefab Streams topologies are not wired to the in-memory
event bus: the streams backends (`prefab-streams-kafka`, `-pubsub`, `-sns-sqs`, `-postgres`, `-mongodb`) need their
broker, so the showcase variant `examples/showcase-inmemory` declares `STREAMS` and `KAFKA_STREAMS_BREAKOUT` as
unsupported, and Streams tests need `@IntegrationTest(containers = true)`.

Run Prefab Streams topologies on the in-memory bus, so applications with streams get container-free integration tests.

Starting points:
- `streams-engine` already contains `InMemoryTopologyTestHarness`, an in-process topology runner used by topology unit
  tests, and the in-process engine with pluggable state repositories (TASK-362).
- The in-memory bus (`be.appify.prefab.test.inmemory.messaging`) delivers synchronously after commit, which suits a
  synchronous engine; the open question is how a topology's source and sink topics map to bus topics, and how
  repartition and changelog topics are handled.
- `KAFKA_STREAMS_BREAKOUT` operators (Kafka Streams specific) need a decision: unsupported in memory, or an emulation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Analysis and design recorded in this task: how topology sources and sinks bind to the in-memory bus, state stores, windowing and time
- [x] #2 With `prefab.test.containers=false`, the showcase Streams slice (`STREAMS`) passes in `examples/showcase-inmemory`, and the capability is removed from its capabilities file
- [x] #3 A decision is made and documented for `KAFKA_STREAMS_BREAKOUT`
- [x] #4 Documentation in `backlog/docs/` (testing section of `feature-guides.md`, `modules.md`) describes Streams in memory
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Analysis (see decision-012):
- The engine already has everything a bus binding needs: `StreamSource`/`StreamSink` SPIs, named per-source
  subscriptions and an in-memory `StreamStateRepository`. Only the binding to `InMemoryEventBus` was missing.
- Source: a bus subscription on the `EventRegistry` topic of the event type. The key is `Keyed.key()`, the event time
  is taken from a stream-published header or the moment of delivery, the message ID from the header or a fresh UUID.
  Several event types on one topic are filtered by value type and by the `prefab-event-type` header for generic
  bindings. The source is ordered: the bus is synchronous and in publication order.
- Sink: `InMemoryEventBus.publish(event, topic, headers)` (new) encodes with the platform codec and keeps the engine's
  headers next to the codec's. Output topics named with `to("topic")` become `EventRegistry` aliases, as for Pub/Sub.
- Repartition/changelog topics: not needed (engine state lives in the repository, `repartition()` is a no-op).
- State and time: `InMemoryStreamStateRepository` (new `clear()`), reset before each test; delivery time as event time.
- `KAFKA_STREAMS_BREAKOUT`: stays unsupported (decision-012 §6). The engine refuses it at start with a message.
- Tombstones cannot travel over the bus; the sink says so (decision-012 §7).

Implementation:
- `prefab-test`: package `be.appify.prefab.test.inmemory.streams` (`InMemoryStreamsAutoConfiguration`,
  `InMemoryBusStreamSource`, `InMemoryBusStreamSink`, `EventRegistryStreamTopicResolver`, `InMemoryStreamsLifecycle`,
  `StreamStateReset`, `InMemoryStreamsTestExecutionListener`). `prefab-streams-engine` and `prefab-streams-kafka` are
  `provided` dependencies (the latter so that the auto-configuration filter test can resolve the excluded
  `StreamsConfiguration`). `InMemoryAutoConfigurationFilter` drops the Kafka Streams auto-configuration in memory mode.
- `showcase-inmemory`: depends on `prefab-streams` and `prefab-streams-engine`, `showcase.streams.skip=false`, `STREAMS`
  removed from the capabilities file. It passes without Docker; the topology tests of the showcase run on the
  engine harness, and the new `InMemoryOrderSalesAnalyticsIntegrationTest` places orders over REST and reads the
  revenue from the bus.
- Docs: feature-guides 7.20 ("Prefab Streams in memory") and 7.15, modules, configuration, troubleshooting, cheat
  sheet, testing recipe, examples/README (matrix, coverage text).
- Benchmark: new task `streams-in-memory` (new modules `streams`, `streams-engine`); `validate` passes.

Verification: `prefab-test` (268 tests incl. 8 new), `prefab-streams-engine`, `prefab-streams-kafka`,
`showcase-inmemory` (Docker-free), and `benchmark` tests. Tests of modules that need Docker (Postgres, MongoDB,
Pub/Sub, LocalStack, Kafka containers) could not run in this environment.
<!-- SECTION:NOTES:END -->
