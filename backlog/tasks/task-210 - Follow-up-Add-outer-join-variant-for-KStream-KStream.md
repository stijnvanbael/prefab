---
id: TASK-210
title: 'Follow-up: Add outer join variant for KStream-KStream'
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
  - TASK-209
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: medium
ordinal: 21000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add KStream-KStream outer join support as a dedicated follow-up after inner and left join capabilities are in place.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams DSL supports KStream-KStream outer join composition
- [ ] #2 Kafka backend emits outputs for left-only, right-only, and matched records
- [ ] #3 `examples/streams` includes a runnable outer join example
- [ ] #4 Tests verify all three output categories and out-of-window handling for outer join semantics
<!-- AC:END -->
