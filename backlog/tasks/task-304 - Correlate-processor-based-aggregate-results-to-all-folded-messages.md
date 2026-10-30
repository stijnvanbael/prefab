---
id: TASK-304
title: Correlate processor-based aggregate results to all folded messages
status: Done
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
- [x] #1 The result correlates to all messages folded into the aggregation, capped at max-correlation-ids.
- [x] #2 Existing Aggregation state stays readable after enabling and disabling tracing.
<!-- AC:END -->

## Implementation Notes

`Aggregation` gained an optional `correlationIds` component (omitted from JSON when empty, null read as empty), so state stays byte-identical without tracing and legacy state stays readable. `AggregationProcessor` merges the record's correlation IDs (capped via the new `StreamProcessorContext.maxCorrelationIds()`) and forwards them on the result. Tests: `AggregationTest`, `MessageTracingAggregationTopologyTest`.
