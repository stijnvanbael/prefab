---
id: TASK-302.05
title: Stream tracing documentation and example
status: Done
assignee: []
created_date: '2026-10-30 13:40'
labels:
  - streams
  - tracing
  - documentation
dependencies:
  - TASK-302.04
parent_task_id: TASK-302
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Slice 5 of TASK-302. Document enabling, semantics, limitations (`breakout`, cap) and the two-step rolling-upgrade procedure in the streams feature guide; add an example.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Feature guide documents configuration, semantics and the rolling-upgrade procedure.
<!-- AC:END -->

## Implementation Notes

Added feature guide subsection (config, semantics, limitations, two-step rolling upgrade), README section and `SalesTracingTopologyTest` in examples/streams.
