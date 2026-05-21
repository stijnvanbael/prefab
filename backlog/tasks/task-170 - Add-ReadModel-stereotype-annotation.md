---
id: TASK-170
title: Add @ReadModel stereotype annotation
status: To Do
assignee: []
created_date: '2026-05-08 16:37'
updated_date: '2026-05-21 06:22'
labels: []
dependencies: []
priority: low
ordinal: 151000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Introduce `@ReadModel` as a stereotype annotation for aggregates that are populated exclusively via `@EventHandler` methods and only expose read endpoints (`@GetById`, `@GetList`). The annotation should impose compile-time restrictions: no `@Create`, `@Update`, or `@Delete` methods allowed. This makes intent explicit and improves discoverability without adding new generated artefacts.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 @ReadModel can be placed on an @Aggregate record
- [ ] #2 Compile-time error is raised if @Create, @Update, or @Delete is also present on a @ReadModel aggregate
- [ ] #3 @ReadModel is documented in the Developer Guide with an example using @EventHandler to populate the model
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
