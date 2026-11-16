---
id: TASK-310
title: Make IntelliJ IDEA incremental compilation reliable without compiler-setting workarounds
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [bug, dx, annotation-processor]
dependencies: []
priority: high
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Investigate and fix Prefab generation failures or stale output during IntelliJ IDEA incremental compilation using its normal annotation-processing support. Resolve processor lifecycle, dependency tracking, metadata and generated-file ownership problems rather than requiring Maven delegation, cache invalidation or compiler-setting changes.

Related: completed TASK-168 (compilation performance) and TASK-307 (multi-round generated records). The README currently recommends compiler delegation; only remove this guidance when the supported native workflow is demonstrated.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Capture reproducible IntelliJ/compiler/JDK versions and scenarios for editing, adding, renaming and deleting model components, nested types and generated event inputs across modules.
- [ ] #2 Diagnose each failure with processor-round and file-ownership evidence; fix affected generation/cache/dependency behavior without changing compiler settings or disabling incremental compilation.
- [ ] #3 Incremental output matches a clean Maven build for supported scenarios and never silently retains obsolete artifacts or loses annotations.
- [ ] #4 Add automated lifecycle/generation regressions and a repeatable native IntelliJ verification procedure; describe any external IDE limitation honestly.
- [ ] #5 Update IDE guidance with the verified workflow; do not advertise universal support from a single successful rebuild.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Create a minimal reproducible multi-module fixture, trace originating elements and processor state over rounds, fix the demonstrated causes, and compare incremental and clean generated outputs.
<!-- SECTION:PLAN:END -->
