---
id: TASK-247
title: Complete key generic propagation in streams module
status: In Progress
assignee: []
created_date: '2026-06-05 05:14'
labels:
  - streams
  - generics
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Finish the migration that adds explicit key generic parameters to Prefab Streams APIs and all usages so the project compiles cleanly.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All Prefab Streams interfaces and implementations consistently use explicit key generic parameters.
- [ ] #2 All call sites in the repository are updated to match the new generic signatures.
- [ ] #3 The project compiles successfully for the affected modules.
- [ ] #4 Relevant developer documentation is updated if public API signatures changed.
<!-- AC:END -->
