---
id: TASK-216
title: Implement process(...) with pluggable StateStore support
status: In Progress
assignee: []
created_date: '2026-05-17 09:38'
updated_date: '2026-05-22 18:17'
labels:
  - feature
  - streams
  - kafka
  - state-store
milestone: m-3
dependencies:
  - TASK-202
  - TASK-203
  - TASK-207
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a typed `process(...)` operator that allows custom processor logic with one or more pluggable StateStore definitions.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams DSL exposes typed `process(...)` with optional store definitions
- [ ] #2 Kafka backend materializes declared state stores and binds them to processor nodes
- [ ] #3 Processor context provides store access for read/write operations during processing
- [ ] #4 Tests validate successful store-backed processing and fail-fast behavior for invalid store bindings
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Execution sequencing note: implement TASK-216 after TASK-214 to reduce churn in process/store API contracts.

TASK-216 should consume established keyBy/repartition naming conventions from TASK-214 and grouped-state baseline from TASK-207.

Safe parallel work before coding: draft processor API tests and store fixture contracts in `examples/streams`.
<!-- SECTION:NOTES:END -->
