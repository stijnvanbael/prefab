---
id: TASK-211
title: 'Follow-up: Add advanced event-time controls for windowed operations'
status: Done
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-17 09:41'
labels:
  - feature
  - streams
  - kafka
  - windowing
  - follow-up
milestone: m-5
dependencies:
  - TASK-208
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: medium
ordinal: 21100
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Introduce advanced event-time controls for stream joins and windowed aggregates, including grace tuning and late-event behavior validation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams DSL exposes configurable window and grace settings for supported windowed operations
- [x] #2 Kafka backend applies configured event-time settings to join and windowed aggregation topologies
- [x] #3 `examples/streams` demonstrates late-event behavior under different grace configurations
- [x] #4 Tests verify expected behavior for on-time events, late events within grace, and events beyond grace
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Execution sequencing note for event-time lane: implement TASK-211 after TASK-208 and before TASK-217.

Preferred order in this lane: TASK-208 -> TASK-211 -> TASK-217.

Parallelization: event-time policy test fixtures can be prepared early, but final operator policy wiring should wait until TASK-211 semantics are stable.

Implemented: grace is already configurable on `AggregationWindow` and `JoinWindow` and applied by the Kafka backend (`TimeWindows.ofSizeAndGrace`, `JoinWindows.ofTimeDifferenceAndGrace`). Added tests for on-time, late-within-grace and beyond-grace events for windowed aggregates and joins (note: Kafka tracks stream time per join side, so lateness of a join input is relative to its own side). `examples/streams` `SalesTotalsTopology` accepts a grace parameter; tests demonstrate 5-minute vs zero grace.
<!-- SECTION:NOTES:END -->
