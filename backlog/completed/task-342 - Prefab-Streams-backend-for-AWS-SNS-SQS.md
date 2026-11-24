---
id: TASK-342
title: Prefab Streams backend for AWS SNS/SQS
status: Done
assignee: []
updated_date: '2026-11-23 15:30'
created_date: '2026-11-20 19:48'
labels:
  - streams
  - sns-sqs
  - coverage-gap
dependencies:
  - TASK-339
  - TASK-340
  - TASK-360
  - TASK-361
priority: medium
type: feature
ordinal: 187000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement a `prefab-streams-sns-sqs` backend so the backend-neutral Prefab Streams DSL runs on AWS SNS/SQS, following the design chosen in TASK-339. Today `StreamBackend` only knows KAFKA, so the showcase-sns-sqs variant (TASK-337.10) declares STREAMS as a gap pointing at this task.

Keyed operators (`groupBy`, `aggregate`, `join`) need per-key ordering, which SNS/SQS only provides through FIFO message groups (TASK-340). Scope of a first version is the operator matrix from TASK-339. The backend-neutral topology test API (TASK-300) must work against it, so the showcase streams slice (TASK-337.06) can be included in showcase-sns-sqs without code changes.

## Design (decided in TASK-339, see decision-008)

Build the backend on the in-process, database-backed engine `prefab-streams-engine` (TASK-360), with state on Postgres (TASK-361). This module only adds the SNS/SQS parts:
- `SnsSqsStreamBackend.SNS_SQS` (`StreamBackend.named("sns-sqs")`) and auto-configuration that activates when `SqsUtil` and a `StreamStateRepository` are present.
- `StreamSource`: one SQS queue per source node (`{application}-streams-{step-name}`, sanitized) subscribed to the source topic, and one shared queue per global store, with the existing DLQ redrive policy. FIFO topics and queues are used when the source is FIFO (TASK-340). Extend `SqsSubscriptionRequest` with a metadata-aware consumer (message ID, message attributes, `SentTimestamp`), and delete the message only after the engine's transaction commits. The visibility timeout must exceed the processing time, including the engine's conflict retries.
- `StreamSink` used by the outbox relay: publish to SNS with `MessageGroupId` = record key and `MessageDeduplicationId` = `prefab-message-id` on FIFO topics, plus the attributes `prefab-message-id`, `prefab-event-time` and `prefab-correlation-ids`. That is at most three of SQS's ten message attributes. `to(String)` creates the topic.
- Event time comes from `prefab-event-time`, otherwise from `SentTimestamp`.
- Without FIFO, stateful operators stay correct thanks to the engine's optimistic concurrency, but order-dependent results can vary, and the engine logs a startup warning. FIFO throughput quotas apply, and a failing message blocks its message group until it moves to the DLQ. Document both.

First-version operators (decision-008):
- Supported, provided by the engine:
  - `from`, `to`, `as`, `end`
  - `filter`, `map`, `flatMap`, `branch`, `merge`
  - `process`, also with stores, and `aggregate`
  - `keyBy` / `repartition` (logical, no topics)
  - `groupBy` + `aggregate`, and `windowedAggregate` with grace (stream time per key)
  - Inner `join` with `JoinWindow`, and `deduplicate`
  - Global stores as shared tables, without history from before the first deployment
  - Exactly-once through the inbox and outbox, tracing, and topology tests through the in-memory harness
- Not supported, failing at startup: `breakout` (Kafka-only) and `StreamProcessorContext.schedule` (TASK-363). In the showcase, declare these as specific gaps rather than as STREAMS.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Showcase streams slice compiles and its tests pass in showcase-sns-sqs, or unsupported operators are skipped with a declared gap
- [x] #2 Topology tests run against the SNS/SQS backend through the backend-neutral test API
- [x] #3 showcase-sns-sqs no longer declares STREAMS as a gap (or declares only specific unsupported operators)
- [x] #4 Integration test (LocalStack + Postgres) covers groupBy+aggregate, windowedAggregate, join and a global store end to end, on FIFO topics
- [x] #5 Redelivered messages do not change state or output twice
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Analysis:
- The engine groundwork (named per-source subscriptions through `StreamSource.SourceBinding`, one inbox per source,
  relay request counting, nested-type codec) landed on `main` with the Pub/Sub backend (TASK-341) while this was in
  progress. This branch was rebuilt on top of it and keeps no engine changes: only the SNS/SQS parts remain.
- `SqsUtil` was too thin for the engine: it deleted the message after the consumer returned and passed only the
  event. Added `SqsSubscriptionRequest.withMessageConsumer` (`SqsMessageConsumer`: event, `SqsMessageMetadata`,
  acknowledgement), `withVisibilityTimeout`, `withFifo`, a `SqsSubscription` handle to stop polling,
  `SqsDeserializer.parseNotification`, and system attributes on receive (`SentTimestamp`, `MessageGroupId`). The
  existing consumer path is unchanged.
- New module `streams-sns-sqs` (`prefab-streams-sns-sqs`): `SnsSqsStreamBackend`, `SnsSqsStreamSource`,
  `SnsSqsStreamSink`, `SnsSqsTopicResolver`, `SnsSqsStreamsConfiguration` (needs `SqsUtil` and a
  `StreamStateRepository`), `SnsSqsTopologyTestHarness` (backend `sns-sqs`, in-memory engine) and
  `SnsSqsStreamsProperties`.

Findings and decisions:
- SNS refuses an empty message, so a tombstone is published with body `null` plus `prefab-tombstone=true` and
  `prefab-key` (the key cannot come from the payload). Deviation from decision-008, which says "empty body".
- The sink sets the SNS subject (class name) only for classes known to `EventRegistry`, because the reader throws
  `UnknownEventTypeException` for unregistered subjects, and stream output types are not registered events.
- A non-FIFO SNS topic cannot feed a FIFO queue, so FIFO is decided per topic: `.fifo` suffix or a registered
  partitioning key, the same rule on the source and the sink. `ordered(topic)` uses the same rule, so the engine's
  startup warning for stateful operators on unordered sources is accurate.
- The sink never copies input headers to the output. It writes the three Prefab attributes, the event type and the
  version attributes; at most eight attributes, within SQS's ten.
- The visibility timeout defaults to 2 minutes (`prefab.streams.sns-sqs.visibility-timeout`), above the default
  retry schedule of the dead-letter policy plus processing time.
- Queue names come from the engine's source names (`source-{hash}-{n}`), so they are stable but not readable; they are
  sanitized and shortened to 75 characters with a hash.

Verification:
- Unit tests with an in-memory SNS/SQS (`FakeSnsSqs`) run source, sink and engine together: group-by aggregate,
  windowed aggregate on event time and on `SentTimestamp`, join, global store with tombstone, FIFO group and
  deduplication ids, redelivery of every message (state and output unchanged), failed message not deleted, one queue
  per source and global store, unsupported `breakout`. The same scenarios also run with state encoded by the
  Postgres codec (`SnsSqsStreamsEngineOnPostgresCodecTest`).
- `ConcurrentQueuesOnPostgresTest` runs the queues of a topology on separate threads against real Postgres: a join
  matches whichever side is processed first (150 pairs), and every output is published without waiting for the next
  message. It found the outbox relay race that `main` now fixes.
- The shared `streams-kafka` topology suite runs on the `sns-sqs` harness (`-Dprefab.streams.test.backend=sns-sqs`
  with the module jar on the test classpath): all pass except the documented Kafka-specific ones (Kafka key bytes,
  cross-key late join).
- The showcase streams slice compiles and its four topology tests pass with the SNS/SQS dependency set
  (`prefab-sns-sqs`, `prefab-streams-sns-sqs`, `prefab-streams-postgres`) in a scratch variant of
  `showcase-variant-parent` with `showcase.streams.skip=false`, without source changes. The scratch variant was not
  committed: `examples/showcase-sns-sqs` is TASK-337.10, which also owns its capabilities file. When it lands it must
  not declare STREAMS, and it needs those three dependencies plus `showcase.streams.skip=false`; the Kafka
  breakout stays skipped (`showcase.kafka.streams.skip=true`).
- `SnsSqsStreamsIntegrationTest` (LocalStack + Postgres, FIFO topics) covers group-by aggregate, windowed aggregate,
  join, global store and a redelivered message. The authoring environment has no Docker, so it first ran on CI, where
  it found the codec and relay bugs that `main` has since fixed independently.

Closed by TASK-337.10: `examples/showcase-sns-sqs` runs the streams slice on this backend with the full showcase suite
green on LocalStack and Postgres. It declares only `KAFKA_STREAMS_BREAKOUT` as a streams gap (plus the Avro gaps of
TASK-275), not `STREAMS`.
<!-- SECTION:NOTES:END -->
