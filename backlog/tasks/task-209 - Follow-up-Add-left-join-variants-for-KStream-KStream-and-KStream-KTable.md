---
id: TASK-209
title: 'Follow-up: Add left join variants for KStream-KStream and KStream-KTable'
status: To Do
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-17 09:18'
labels:
  - feature
  - streams
  - kafka
  - join
  - follow-up
milestone: m-4
dependencies:
  - TASK-205
  - TASK-206
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: medium
ordinal: 20900
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Extend join support with left join variants for both stream-stream and stream-table use cases, reusing the same streams DSL and example module.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams DSL supports left join variants for both KStream-KStream and KStream-KTable joins
- [ ] #2 Kafka backend emits records for unmatched left side according to left join semantics
- [ ] #3 `examples/streams` includes runnable left join examples for both join types
- [ ] #4 Tests validate matched and unmatched-left behavior for each left join variant
<!-- AC:END -->
