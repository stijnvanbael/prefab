---
id: TASK-302.04
title: Stream tracing for aggregations
status: To Do
assignee: []
created_date: '2026-10-30 13:40'
labels:
  - streams
  - tracing
dependencies:
  - TASK-302.03
parent_task_id: TASK-302
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Slice 4 of TASK-302. `aggregate` and `windowedAggregate` keep a running union of origin sets in the state store (cap `prefab.streams.tracing.max-correlation-ids`, oldest evicted first) and emit it as correlation IDs. The groupBy repartition topic uses the tolerant serde.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An aggregate result correlates to all messages folded into it.
- [ ] #2 The cap is enforced with oldest-first eviction.
- [ ] #3 Windowed aggregates only correlate to messages in the emitted window.
- [ ] #4 Existing aggregate state continues to work after enabling tracing.
<!-- AC:END -->
