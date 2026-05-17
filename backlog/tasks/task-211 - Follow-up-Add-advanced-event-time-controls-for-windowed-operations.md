---
id: TASK-211
title: 'Follow-up: Add advanced event-time controls for windowed operations'
status: To Do
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
- [ ] #1 Streams DSL exposes configurable window and grace settings for supported windowed operations
- [ ] #2 Kafka backend applies configured event-time settings to join and windowed aggregation topologies
- [ ] #3 `examples/streams` demonstrates late-event behavior under different grace configurations
- [ ] #4 Tests verify expected behavior for on-time events, late events within grace, and events beyond grace
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Execution sequencing note for event-time lane: implement TASK-211 after TASK-208 and before TASK-217.

Preferred order in this lane: TASK-208 -> TASK-211 -> TASK-217.

Parallelization: event-time policy test fixtures can be prepared early, but final operator policy wiring should wait until TASK-211 semantics are stable.
<!-- SECTION:NOTES:END -->
