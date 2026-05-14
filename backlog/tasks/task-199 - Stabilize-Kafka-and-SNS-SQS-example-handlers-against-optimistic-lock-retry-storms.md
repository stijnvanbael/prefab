---
id: TASK-199
title: >-
  Stabilize Kafka and SNS/SQS example handlers against optimistic-lock retry
  storms
status: Done
assignee: []
created_date: '2026-05-14 08:11'
updated_date: '2026-05-14 08:45'
labels:
  - kafka
  - sns-sqs
  - reliability
dependencies: []
references:
  - examples/kafka/src/main/java
  - examples/sns-sqs/src/main/java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Apply the same hot-key ordering and handler design hardening used for Pub/Sub to Kafka and SNS/SQS examples where message fan-out can concurrently update the same aggregate rows and produce OptimisticLockingFailureException retry storms.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Kafka example event partitioning/order configuration ensures same-aggregate updates serialize and avoids concurrent write races in integration tests.
- [x] #2 SNS/SQS example event partitioning/grouping configuration ensures same-aggregate updates serialize and avoids concurrent write races in integration tests.
- [x] #3 Targeted integration tests for kafka and sns-sqs pass and show no retry-exhaustion errors in consumer threads during stress-style runs.
- [x] #4 Developer docs note platform-specific ordering/grouping strategy and trade-offs.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-05-14: Applied deterministic hot-key hardening to Kafka and SNS/SQS examples by setting Channel, UserStatus, and ChannelSummary handlers to concurrency=1.
2026-05-14: Made ChannelSummary create-or-update flows idempotent with deterministic summary IDs derived from channel references.
2026-05-14: Increased Kafka and SNS/SQS ChannelSummary integration scenarios to 20 iterations and verified no RetryException exhaustion in stress-style runs.
2026-05-14: Documented Kafka/PubSub/SNS-SQS ordering and concurrency trade-offs in backlog/docs/feature-guides.md.
<!-- SECTION:NOTES:END -->
