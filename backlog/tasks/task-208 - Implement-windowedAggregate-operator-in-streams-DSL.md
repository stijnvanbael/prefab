---
id: TASK-208
title: Implement windowedAggregate operator in streams DSL
status: To Do
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-17 09:18'
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
- [ ] #1 Streams DSL supports `windowedAggregate` as a dedicated operator
- [ ] #2 Kafka backend maps windowed aggregation to native Kafka Streams windowed state stores
- [ ] #3 `examples/streams` includes a runnable windowed aggregation scenario
- [ ] #4 Tests verify in-window aggregation and no cross-window aggregation leakage
<!-- AC:END -->
