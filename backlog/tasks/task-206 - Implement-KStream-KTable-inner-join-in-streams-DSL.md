---
id: TASK-206
title: Implement KStream-KTable inner join in streams DSL
status: To Do
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-17 09:18'
labels:
  - feature
  - streams
  - kafka
  - join
milestone: m-2
dependencies:
  - TASK-205
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 20600
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add Kafka-backed KStream-KTable inner join support for stream-table enrichment and validate behavior in examples/tests.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams DSL supports KStream-KTable inner join composition
- [ ] #2 Kafka backend maps join semantics to native Kafka Streams KStream-KTable join operations
- [ ] #3 `examples/streams` includes a runnable KStream-KTable enrichment example
- [ ] #4 Tests validate enrichment when table rows exist and no emission when table keys are absent
<!-- AC:END -->
