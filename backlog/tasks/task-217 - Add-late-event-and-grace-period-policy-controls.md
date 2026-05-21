---
id: TASK-217
title: Add late-event and grace-period policy controls
status: To Do
assignee: []
created_date: '2026-05-17 09:38'
updated_date: '2026-05-21 06:22'
labels:
  - feature
  - streams
  - kafka
  - windowing
milestone: m-5
dependencies:
  - TASK-208
  - TASK-211
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 168000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add DSL-level controls for late-event handling and grace periods with global defaults and per-operator overrides for windowed operations.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams DSL allows configuring grace periods and late-event handling policies for windowed operators
- [ ] #2 Global default policies can be overridden at operator level
- [ ] #3 Integration tests cover boundary behavior for on-time events, late-within-grace events, and too-late events
- [ ] #4 Developer guide documents supported policy modes and expected behavior matrix
<!-- AC:END -->
