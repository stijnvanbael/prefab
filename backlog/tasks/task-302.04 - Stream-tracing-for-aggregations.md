---
id: TASK-302.04
title: Stream tracing for aggregations
status: Done
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
- [x] #1 An aggregate result correlates to all messages folded into it.
- [x] #2 The cap is enforced with oldest-first eviction.
- [x] #3 Windowed aggregates only correlate to messages in the emitted window.
- [x] #4 Existing aggregate state continues to work after enabling tracing.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
With tracing enabled groupBy wraps values in TracedValue (trace-wrap) and the repartition topic and aggregate/windowed stores use TracedValueSerde.writing; origins fold with MessageTracing.union (capped, oldest evicted) and are unwrapped into the correlation header (trace-unwrap). Store and repartition names are unchanged; legacy state reads as no origins. Cap behaviour is unit tested on MessageTracing.union (the topology test extension uses the default cap). Limitation: the processor based PrefabStream.aggregate(groupBy, fn, predicate) keeps its Aggregation store schema and only inherits the current record's correlation; tracked in TASK-304.
<!-- SECTION:NOTES:END -->
