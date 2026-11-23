---
id: TASK-360
title: In-process Prefab Streams engine with in-memory test harness
status: To Do
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
- [ ] #1 prefab-streams-engine depends only on prefab-streams and prefab-core, with no broker SDK
- [ ] #2 Every operator marked supported in decision-008 runs through the in-memory topology test harness
- [ ] #3 The existing backend-neutral topology tests from streams-kafka pass on the engine harness, except tests that are documented as Kafka-specific (breakout, stream-time differences)
- [ ] #4 Duplicate input messages (same message ID) do not change state or produce output twice
- [ ] #5 breakout and schedule fail at topology start with a clear message
- [ ] #6 Kafka key bytes are unchanged after moving the key encoding to prefab-streams
<!-- AC:END -->
