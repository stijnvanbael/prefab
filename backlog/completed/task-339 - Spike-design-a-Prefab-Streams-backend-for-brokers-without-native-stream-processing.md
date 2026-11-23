---
id: TASK-339
title: >-
  Spike: design a Prefab Streams backend for brokers without native stream
  processing
status: Done
assignee:
  - claude
updated_date: '2026-11-21 10:00'
created_date: '2026-11-20 19:48'
labels:
  - streams
  - coverage-gap
  - pubsub
  - sns-sqs
dependencies:
  - TASK-338
priority: medium
type: spike
ordinal: 184000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab Streams only has a Kafka backend (`StreamBackend.KAFKA`, `KafkaPrefabStreams`). Google Pub/Sub and AWS SNS/SQS have no stream-processing engine like Kafka Streams, so applications on those brokers cannot use the DSL at all.

Decide how to support them, and record the decision as an ADR. Options to evaluate:
- An in-process backend: subscriptions as sources, topics as sinks, state stores, windows and join buffers persisted in the application's database (Postgres/MongoDB), with per-key ordering from Pub/Sub ordering keys or SQS FIFO message groups.
- Delegating to a managed engine (Google Dataflow / Apache Beam, AWS Kinesis Data Analytics / Managed Flink).
- Per-operator feasibility: stateless ops, `groupBy`+`aggregate`, `windowedAggregate` with grace, stream-stream `join`, global stores, exactly-once/dedup (TASK-215), tracing.

Output: an ADR and implementation tasks per broker with the operators each can support in a first version. The two broker tasks (Pub/Sub, SNS/SQS) depend on this spike.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 ADR in backlog/decisions with the chosen approach and rejected alternatives
- [x] #2 Operator support matrix per broker for a first version
- [x] #3 Implementation tasks for Pub/Sub and SNS/SQS updated with the chosen design
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Decision: backlog/decisions/decision-008 (accepted). The two brokers share one in-process, database-backed engine instead of a managed engine.

Analysis:
- Kafka Streams relies on partition ownership, changelog topics, repartition topics, stream time per partition, transactions and replayable topics for global stores. Pub/Sub and SNS/SQS have none of these. They do offer ordering per key (Pub/Sub ordering keys, already set by `GenericPubSubPublisher`; SQS FIFO message groups, TASK-340), at-least-once delivery and dead-lettering through `PubSubUtil` and `SqsUtil`.
- Every Prefab application on these brokers has a transactional database, so state, inbox and outbox can commit atomically with the processing of a message. That gives exactly-once processing relative to the database (TASK-215 semantics) without broker transactions.
- Optimistic versioning of state rows makes the store correct on any broker and with any number of instances. Broker ordering only decides order. Because of that, `keyBy`, `repartition` and `groupBy` can stay logical, with no topics.
- Stream time is tracked per key, not per partition, to avoid a hot row. This differs from Kafka, and the difference is documented.
- Global stores become shared tables. Neither broker can replay a topic, so a store holds no history from before its first deployment.
- `breakout` cannot be supported (no native API). Wall-clock punctuation needs a single-instance lease and is deferred.
- Rejected: Dataflow/Beam, Managed Flink, running Kafka alongside, local state per instance, home-grown partitioning, physical repartition topics, pessimistic locks, publishing without an outbox, a shared stream-time row, and engine tables in the application's Flyway sequence.

Output:
- The operator support matrix per broker is in decision-008.
- New tasks: TASK-351 (broker-neutral `prefab-streams-engine` with in-memory topology test harness), TASK-352 (Postgres state, inbox and outbox), TASK-353 (MongoDB state, follow-up) and TASK-354 (`StreamProcessorContext.schedule`, follow-up).
- TASK-341 (Pub/Sub) and TASK-342 (SNS/SQS) now describe the broker-specific design, carry the operator list, and depend on TASK-351 and TASK-352. TASK-342 still depends on TASK-340.
- TASK-337.06 notes that the showcase's `breakout` topology must stay Kafka-only.
<!-- SECTION:NOTES:END -->
