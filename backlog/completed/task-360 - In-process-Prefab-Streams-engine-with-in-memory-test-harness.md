---
id: TASK-360
title: In-process Prefab Streams engine with in-memory test harness
status: Done
assignee: []
created_date: '2026-11-21 10:00'
labels:
  - streams
  - coverage-gap
dependencies:
  - TASK-339
priority: medium
type: feature
ordinal: 197000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a broker-neutral module `prefab-streams-engine` (package `be.appify.prefab.streams.engine`). It implements the Prefab Streams DSL by building an operator graph and interpreting it in the application process, as decided in decision-008 (TASK-339). It is the shared base of the Pub/Sub (TASK-341) and SNS/SQS (TASK-342) backends.

Scope:
- SPIs:
  - `StreamSource` / `StreamSink`: subscribe with a per-message callback carrying message ID, headers and event time, and publish with key, headers and event time.
  - `StreamStateRepository`: key-value stores, window stores, join buffers, inbox, outbox and relay lease, all with optimistic versioning.
- Processing loop: one transaction per input message (inbox check, operator graph, state writes, outbox writes), commit, then acknowledge. On a version conflict the message is retried a bounded number of times, then the exception goes to the broker's retry.
- Operators, following the first-version matrix in decision-008:
  - Stateless: `filter`, `map`, `flatMap`, `branch`, `merge`, `as`, `to`, `end`, `process`.
  - Stateful `process` with `Store`s, and `aggregate`.
  - `keyBy`, `repartition` and `groupBy` as logical re-keying, without topics.
  - `groupBy` + `aggregate`, and `windowedAggregate` with grace, using stream time per key.
  - Inner `join` with `JoinWindow`, `deduplicate`, and global stores as shared tables.
  - Tracing headers, with correlation IDs cut to a configurable maximum attribute size.
- `breakout` with an adapter for another backend, and `StreamProcessorContext.schedule`, fail at startup with a message naming TASK-363.
- Outbox relay with a lease per sink topic, and scheduled cleanup of closed windows, join buffers, deduplication identities and inbox rows (`prefab.streams.inbox-retention`).
- Move the shape-based key encoding (single-field record → plain string, otherwise JSON) from `prefab-streams-kafka` to `prefab-streams`, so both backends share it without changing Kafka's bytes.
- In-memory `StreamStateRepository`, source and sink, plus a `TopologyTestHarness.Factory` registration, so `TopologyTestHarness` / `@TestStreams` tests run without containers.
- A `StreamStateRepository` contract test suite that TASK-361 and TASK-362 reuse.
- An engine-specific startup warning when a stateful operator reads from an unordered source.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 prefab-streams-engine depends only on prefab-streams and prefab-core, with no broker SDK
- [x] #2 Every operator marked supported in decision-008 runs through the in-memory topology test harness
- [x] #3 The existing backend-neutral topology tests from streams-kafka pass on the engine harness, except tests that are documented as Kafka-specific (breakout, stream-time differences)
- [x] #4 Duplicate input messages (same message ID) do not change state or produce output twice
- [x] #5 breakout and schedule fail at topology start with a clear message
- [x] #6 Kafka key bytes are unchanged after moving the key encoding to prefab-streams
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Analysis:
- Decision-008 is accepted and defines the runtime contract; this module owns graph interpretation and in-memory test doubles, while database-backed state and broker adapters are separate tasks.
- `streams-kafka` contains backend-neutral topology tests plus a Kafka-only key serde implementation. The key encoder must move without changing its byte representation, and `TopologyTestHarness` discovers implementations through `ServiceLoader`.
- Implementation must preserve the DSL's declarative operator types and support startup-time rejection for native breakout and wall-clock scheduling.
- Extracted the key shape encoding to `prefab-streams.StreamKeyEncoding`; the Kafka serde now delegates to it. Existing Kafka key serde tests and new backend-neutral round-trip tests pass.
- Implemented the broker-neutral runtime, transactional state/inbox/outbox SPI, in-memory repository/source/sink, service-loaded test harness, and a reusable repository contract test artifact.
- Added explicit harness selection so shared Kafka topology tests can run against `in-process`; 84 backend-neutral topology tests passed. The cross-key late-join test and Kafka key-byte tests remain backend-specific as documented.
- Separated wall-clock inbox/lease cleanup from operator-keyed event-time state expiry so historical windows and deduplication identities are not removed prematurely; added an outbox retry regression test.
- Final validation: the engine module test suite and repository contract tests pass, the contract test JAR is packaged, the shared in-process topology suite passes (84 tests), and the shared key encoding plus Kafka serde tests pass.
<!-- SECTION:NOTES:END -->
