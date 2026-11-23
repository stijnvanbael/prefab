---
id: TASK-342
title: Prefab Streams backend for AWS SNS/SQS
status: To Do
assignee: []
updated_date: '2026-11-21 10:00'
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
- [ ] #1 Showcase streams slice compiles and its tests pass in showcase-sns-sqs, or unsupported operators are skipped with a declared gap
- [ ] #2 Topology tests run against the SNS/SQS backend through the backend-neutral test API
- [ ] #3 showcase-sns-sqs no longer declares STREAMS as a gap (or declares only specific unsupported operators)
- [ ] #4 Integration test (LocalStack + Postgres) covers groupBy+aggregate, windowedAggregate, join and a global store end to end, on FIFO topics
- [ ] #5 Redelivered messages do not change state or output twice
<!-- AC:END -->
