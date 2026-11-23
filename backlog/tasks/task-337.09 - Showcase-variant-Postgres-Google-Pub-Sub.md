---
id: TASK-337.09
title: 'Showcase variant: Postgres + Google Pub/Sub'
status: To Do
assignee: []
created_date: '2026-11-20 19:47'
updated_date: '2026-11-20 19:48'
labels:
  - examples
  - pubsub
dependencies:
  - TASK-337.05
  - TASK-337.06
  - TASK-337.07
  - TASK-341
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 180000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add `examples/showcase-pubsub`: pom (prefab-pubsub instead of prefab-kafka), application.yml (GCP project id, message ordering), test config and `showcase-capabilities.yml`. Replaces `examples/pubsub`. Include the common streams slice using the Pub/Sub backend from TASK-341 and Postgres state; do not include the Kafka-only breakout implementation. The Avro slice remains out of scope.

Run the full showcase suite, including the backend-neutral streams tests. Declare AVRO as a gap (payloads not self-describing, TASK-275), and declare only specific unsupported streams operators rather than a general STREAMS gap. Every other failing feature is fixed (if small) or declared with a new backlog task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Module contains no Java sources
- [ ] #2 Full showcase suite passes or skips with declared gaps, each linked to a backlog task
- [ ] #3 Coverage matrix updated
- [ ] #4 Showcase streams slice compiles and its tests pass in showcase-pubsub, or unsupported operators are skipped with a declared gap
- [ ] #5 showcase-pubsub does not declare STREAMS as a gap; only specific unsupported operators are declared
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
The showcase-specific criteria formerly in TASK-341 are verified here, after the backend has been merged.
Declare AVRO with TASK-275, and document Kafka-only breakout and any use of unsupported scheduling
(TASK-363) as specific gaps where applicable.
<!-- SECTION:NOTES:END -->
