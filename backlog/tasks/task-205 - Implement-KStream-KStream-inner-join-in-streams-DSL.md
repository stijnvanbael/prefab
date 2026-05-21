---
id: TASK-205
title: Implement KStream-KStream inner join in streams DSL
status: To Do
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-21 06:38'
labels:
  - feature
  - streams
  - kafka
  - join
milestone: m-2
dependencies:
  - TASK-204
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 28.99169921875
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add Kafka-backed KStream-KStream inner join support with explicit window configuration and runnable coverage in the streams example.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams DSL supports KStream-KStream inner join composition
- [ ] #2 Kafka backend maps join semantics to native Kafka Streams windowed inner join operations
- [ ] #3 `examples/streams` includes a runnable KStream-KStream inner join example
- [ ] #4 Tests cover matching keys, non-matching keys, and out-of-window events for the join
<!-- AC:END -->
