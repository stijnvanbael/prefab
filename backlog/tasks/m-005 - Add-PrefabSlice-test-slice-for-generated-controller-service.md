---
id: M-005
title: Add @PrefabSlice test slice for generated controller/service
status: To Do
assignee: []
created_date: '2026-05-08 16:37'
labels: []
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Introduce a `@PrefabSlice` test annotation (analogous to Spring Boot's `@WebMvcTest`) that bootstraps only the generated controller and service for a single aggregate with a mocked repository. This enables fast, container-free tests of the generated HTTP layer.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 @PrefabSlice(MyAggregate.class) boots only the generated controller and service for that aggregate
- [ ] #2 The repository is auto-mocked so no database container is needed
- [ ] #3 Tests annotated with @PrefabSlice run in under 2 seconds
- [ ] #4 @PrefabSlice is documented in the Developer Guide with a worked example
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
