---
id: TASK-339
title: >-
  Spike: design a Prefab Streams backend for brokers without native stream
  processing
status: To Do
assignee: []
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
- [ ] #1 ADR in backlog/decisions with the chosen approach and rejected alternatives
- [ ] #2 Operator support matrix per broker for a first version
- [ ] #3 Implementation tasks for Pub/Sub and SNS/SQS updated with the chosen design
<!-- AC:END -->
