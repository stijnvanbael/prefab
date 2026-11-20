---
id: TASK-337.08
title: 'Showcase variant: MongoDB + Kafka'
status: To Do
assignee: []
created_date: '2026-11-20 19:47'
labels:
  - examples
  - mongodb
dependencies:
  - TASK-337.05
  - TASK-337.06
  - TASK-337.07
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 179000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add `examples/showcase-mongodb`: pom (prefab-mongodb instead of prefab-postgres), application.yml, test config and `showcase-capabilities.yml`. Replaces `examples/mongodb`. Includes the streams and Avro slices (Kafka).

Run the full showcase suite. Every failing feature is either fixed in Prefab (if small) or declared as an unsupported capability with a new backlog task. Pick up TASK-153 (flaky CategoryStatsIntegrationTest) if the equivalent test is flaky here.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Module contains no Java sources
- [ ] #2 Full showcase suite passes or skips with declared gaps, each linked to a backlog task
- [ ] #3 Coverage matrix updated
<!-- AC:END -->
