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
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 180000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add `examples/showcase-pubsub`: pom (prefab-pubsub instead of prefab-kafka), application.yml (GCP project id, message ordering), test config and `showcase-capabilities.yml`. Replaces `examples/pubsub`. Includes neither the streams nor the Avro slice (yet).

Run the full showcase suite. Known gaps to declare: STREAMS (no Pub/Sub streams backend), AVRO (payloads not self-describing, TASK-275). Every other failing feature is fixed (if small) or declared with a new backlog task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Module contains no Java sources
- [ ] #2 Full showcase suite passes or skips with declared gaps, each linked to a backlog task
- [ ] #3 Coverage matrix updated
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Gap task IDs to put in showcase-capabilities.yml: STREAMS → TASK-341, AVRO → TASK-275.
<!-- SECTION:NOTES:END -->
