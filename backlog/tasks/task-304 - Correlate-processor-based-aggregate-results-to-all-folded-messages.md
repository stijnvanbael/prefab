---
id: TASK-304
title: Correlate processor-based aggregate results to all folded messages
status: To Do
assignee: []
created_date: '2026-10-30 14:25'
labels:
  - streams
  - tracing
dependencies:
  - TASK-302.04
parent_task_id: TASK-302
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`PrefabStream.aggregate(groupBy, aggregation, egressCondition)` is built on `AggregationProcessor` and its `Aggregation` store, which holds values only. With tracing enabled its output only correlates to the message that triggered the emission. Keep the origin IDs per key (capped, oldest evicted) without changing the existing store JSON shape in a breaking way, and apply them to the forwarded record.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The result correlates to all messages folded into the aggregation, capped at max-correlation-ids.
- [ ] #2 Existing Aggregation state stays readable after enabling and disabling tracing.
<!-- AC:END -->
