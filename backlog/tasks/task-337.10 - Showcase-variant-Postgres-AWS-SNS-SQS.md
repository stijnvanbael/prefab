---
id: TASK-337.10
title: 'Showcase variant: Postgres + AWS SNS/SQS'
status: To Do
assignee: []
created_date: '2026-11-20 19:47'
updated_date: '2026-11-20 19:48'
labels:
  - examples
  - sns-sqs
dependencies:
  - TASK-337.05
  - TASK-337.06
  - TASK-337.07
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 181000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add `examples/showcase-sns-sqs`: pom (prefab-sns-sqs instead of prefab-kafka), application.yml, `application-local.yml` for LocalStack (keep the README guidance of the old example), test config and `showcase-capabilities.yml`. Replaces `examples/sns-sqs`. Includes neither the streams nor the Avro slice (yet).

Run the full showcase suite. Known gaps to declare: STREAMS (no SNS/SQS streams backend), ORDERED_DELIVERY (`@PartitioningKey` is ignored — standard topics, no FIFO/message group ID), AVRO (payloads not self-describing, TASK-275). Every other failing feature is fixed (if small) or declared with a new backlog task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Module contains no Java sources
- [ ] #2 Full showcase suite passes or skips with declared gaps, each linked to a backlog task
- [ ] #3 Coverage matrix updated
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Gap task IDs to put in showcase-capabilities.yml: STREAMS → TASK-342, ORDERED_DELIVERY → TASK-340, AVRO → TASK-275.

Update from TASK-342: the SNS/SQS streams backend (`prefab-streams-sns-sqs`) exists, so this variant should include the streams slice and not declare STREAMS. Add `prefab-streams`, `prefab-streams-sns-sqs` and `prefab-streams-postgres` and set `showcase.streams.skip=false`; leave `showcase.kafka.streams.skip` true (the Kafka-native breakout topology stays out). The slice compiled and its topology tests passed on these dependencies in a scratch variant. `@PartitioningKey` events are now FIFO (TASK-340), so re-check whether ORDERED_DELIVERY is still a gap.
<!-- SECTION:NOTES:END -->
