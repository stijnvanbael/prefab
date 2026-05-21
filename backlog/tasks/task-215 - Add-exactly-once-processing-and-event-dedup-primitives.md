---
id: TASK-215
title: Add exactly-once processing and event dedup primitives
status: To Do
assignee: []
created_date: '2026-05-17 09:38'
updated_date: '2026-05-21 06:22'
labels:
  - feature
  - streams
  - kafka
  - reliability
milestone: m-6
dependencies:
  - TASK-202
  - TASK-207
  - TASK-211
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 167000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add DSL and runtime controls for Kafka exactly-once processing plus reusable event deduplication primitives for idempotent stream handling.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams configuration supports enabling Kafka Streams exactly-once processing mode where supported
- [ ] #2 Streams DSL exposes a deduplication primitive based on event identity with configurable retention/window semantics
- [ ] #3 Integration tests verify duplicates are suppressed while unique events are emitted
- [ ] #4 Developer guide documents guarantees, prerequisites, and limitations for exactly-once and dedup behavior
<!-- AC:END -->
