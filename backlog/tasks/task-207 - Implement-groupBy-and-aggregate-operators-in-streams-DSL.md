---
id: TASK-207
title: Implement groupBy and aggregate operators in streams DSL
status: To Do
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-21 06:39'
labels:
  - feature
  - streams
  - kafka
  - aggregate
milestone: m-3
dependencies:
  - TASK-206
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 7.2479248046875
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add non-windowed grouping and aggregation operators for Kafka-backed streams DSL pipelines, with tests and example usage.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams DSL supports `groupBy` followed by non-windowed `aggregate`
- [ ] #2 Kafka backend maps grouped aggregate semantics to native Kafka Streams grouped stateful operations
- [ ] #3 `examples/streams` includes a runnable non-windowed aggregate pipeline
- [ ] #4 Tests verify aggregate state evolution for multiple events on the same key
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Execution sequencing note: complete TASK-207 before TASK-214 and TASK-216 to establish stable grouped-state semantics.

This task defines baseline grouping and aggregate contracts consumed by repartition naming and custom process/store wiring.
<!-- SECTION:NOTES:END -->
