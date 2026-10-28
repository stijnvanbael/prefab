---
id: TASK-208
title: Implement windowedAggregate operator in streams DSL
status: Done
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-17 09:41'
labels:
  - feature
  - streams
  - kafka
  - aggregate
  - windowing
milestone: m-3
dependencies:
  - TASK-207
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 20800
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add windowed aggregation support as a separate capability from non-windowed aggregate, including configurable window behavior and tests.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams DSL supports `windowedAggregate` as a dedicated operator
- [x] #2 Kafka backend maps windowed aggregation to native Kafka Streams windowed state stores
- [x] #3 `examples/streams` includes a runnable windowed aggregation scenario
- [x] #4 Tests verify in-window aggregation and no cross-window aggregation leakage
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Execution sequencing note for event-time lane: complete TASK-208 before TASK-211 and TASK-217.

TASK-208 provides baseline windowed aggregation behavior that later grace/late-event controls must extend without changing core aggregate semantics.

Implemented: `AggregationWindow` (tumbling/hopping + grace) and `PrefabGroupedStream.windowedAggregate(window, resultType, initializer, aggregator)`. Kafka: `KGroupedStream.windowedBy(TimeWindows).aggregate` with a named window `Materialized` store (`windowed-aggregate-<in>-<out>`); `toStream` drops the window from the key, so emissions are keyed by the group key (one update per record and window). Tests in `KafkaPrefabStreamsTopologyTest`; example in `examples/streams` sales topology (hourly totals). Session windows and window-close-only emission (suppress) are out of scope (TASK-211/217).
<!-- SECTION:NOTES:END -->
