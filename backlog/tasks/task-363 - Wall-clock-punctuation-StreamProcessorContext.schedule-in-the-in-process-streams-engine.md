---
id: TASK-363
title: Wall-clock punctuation (StreamProcessorContext.schedule) in the in-process streams engine
status: To Do
assignee: []
created_date: '2026-11-21 10:00'
labels:
  - streams
  - coverage-gap
dependencies:
  - TASK-360
  - TASK-361
priority: low
type: feature
ordinal: 200000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`StreamProcessorContext.schedule(interval, task)` runs a wall-clock punctuation on Kafka once per task (partition), over that partition's stores. In the in-process engine (decision-008), state is shared by all instances, so running the task on every instance would duplicate its output. The first engine version rejects `schedule` at startup.

Support it by running each scheduled task on one instance at a time, behind a database lease per processor. Each run happens in one transaction, and its output goes through the outbox. Document the semantic difference: one run over all keys instead of one run per partition.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A scheduled task runs on exactly one instance per interval, proven with two engine instances
- [ ] #2 Output of a scheduled task is published through the outbox
- [ ] #3 The topology test harness can advance wall-clock time to trigger scheduled tasks
- [ ] #4 The decision-008 operator matrix is updated
<!-- AC:END -->
