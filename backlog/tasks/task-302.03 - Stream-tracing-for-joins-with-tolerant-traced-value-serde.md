---
id: TASK-302.03
title: Stream tracing for joins with tolerant traced value serde
status: To Do
assignee: []
created_date: '2026-10-30 13:40'
labels:
  - streams
  - tracing
dependencies:
  - TASK-302.01
parent_task_id: TASK-302
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Slice 3 of TASK-302. Internal `TracedValue` and `TracedValueSerde` with the magic-prefixed envelope and a tolerant reader (always installed, envelope written only when tracing is enabled). `join` wraps both sides and merges origin sets. Includes migration tests (legacy bytes, JSON and Avro) and the topology-name stability test.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A joined message correlates to both contributing messages.
- [ ] #2 Legacy bytes read as "no origins"; envelope bytes stay readable when tracing is disabled.
- [ ] #3 Store and internal topic names are identical with tracing off and on.
- [ ] #4 With tracing disabled the written payload bytes are unchanged.
<!-- AC:END -->
