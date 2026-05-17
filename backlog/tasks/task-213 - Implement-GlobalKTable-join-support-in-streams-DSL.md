---
id: TASK-213
title: Implement GlobalKTable join support in streams DSL
status: To Do
assignee: []
created_date: '2026-05-17 09:37'
labels:
  - feature
  - streams
  - kafka
  - join
milestone: m-4
dependencies:
  - TASK-202
  - TASK-203
  - TASK-206
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add streams DSL support for KStream-GlobalKTable joins with explicit key-mapping and joiner callbacks, wired to Kafka Streams GlobalKTable semantics.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams DSL exposes a GlobalKTable join operator with key selector and value joiner callbacks
- [ ] #2 Kafka backend maps the operator to native KStream-GlobalKTable join topology nodes
- [ ] #3 Integration tests validate join results when stream and table source partitions differ
- [ ] #4 Developer guide documents GlobalKTable join usage and operational implications
<!-- AC:END -->
