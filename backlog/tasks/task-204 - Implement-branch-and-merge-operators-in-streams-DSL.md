---
id: TASK-204
title: Implement branch and merge operators in streams DSL
status: To Do
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-17 09:18'
labels:
  - feature
  - streams
  - kafka
milestone: m-1
dependencies:
  - TASK-203
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 20400
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add DSL support for stream branching and fan-in merging on the Kafka backend, including example pipelines and tests.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams DSL exposes `branch` and `merge` operators for Kafka pipelines
- [ ] #2 Kafka backend maps branching and merging behavior to native Kafka Streams topology operations
- [ ] #3 `examples/streams` includes a runnable branch-and-merge scenario with multiple output paths
- [ ] #4 Tests validate records are routed to expected branches and merged output preserves expected content
<!-- AC:END -->
