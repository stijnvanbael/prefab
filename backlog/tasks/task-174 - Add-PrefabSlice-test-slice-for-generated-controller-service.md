---
id: TASK-174
title: Add @PrefabSlice test slice for generated controller/service
status: To Do
assignee: []
created_date: '2026-05-08 16:37'
updated_date: '2026-11-20 20:14'
labels: []
dependencies: []
priority: high
ordinal: 155000
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

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-11-20 20:14
---
AI-driven development: a fast, container-free test loop matters even more for agents than for humans. Cheap models like Haiku iterate more often, and a slow or Docker-dependent loop is the main cost and failure source in agent runs. The Haiku benchmark (TASK-343) should measure the impact.
---
<!-- COMMENTS:END -->
