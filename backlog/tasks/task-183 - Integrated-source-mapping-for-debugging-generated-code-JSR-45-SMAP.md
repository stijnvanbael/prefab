---
id: TASK-183
title: Integrated source mapping for debugging generated code (JSR-45 SMAP)
status: To Do
assignee: []
created_date: '2026-05-08'
updated_date: '2026-05-21 06:22'
labels: []
dependencies: []
priority: low
ordinal: 147000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Debugging generated code is difficult because the IDE treats it as a secondary source. Enhance the
annotation processor to emit JSR-45 SMAP (Source Debug Extension) metadata so that a developer can
set a breakpoint on a Prefab annotation (e.g. `@GetById`, `@Create`) in the domain record and have
the debugger step directly into the corresponding generated controller or service logic. This
eliminates the black-box feeling by linking the high-level declaration to its low-level execution
during a debug session.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The annotation processor emits JSR-45 SMAP entries that map generated class line numbers back to the originating annotation in the domain record
- [ ] #2 Setting a breakpoint on @GetById, @Create, @Update, or @Delete in the domain record causes the debugger to stop at the corresponding line in the generated controller/service
- [ ] #3 The feature works in IntelliJ IDEA with the standard Java debugger (no plugin required)
- [ ] #4 SMAP generation does not affect production runtime behaviour or class loading performance
- [ ] #5 Developer Guide documents how to use the source-mapped debugging experience
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
