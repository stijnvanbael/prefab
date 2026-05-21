---
id: TASK-184
title: Eject command to materialise generated code into src
status: To Do
assignee: []
created_date: '2026-05-08'
updated_date: '2026-05-21 06:22'
labels: []
dependencies: []
priority: medium
ordinal: 148000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Developers fear being trapped by a code generator when it cannot handle an edge case. Implement a
`prefab:eject` Maven plugin goal (analogous to Create React App's `eject` command) that permanently
moves all generated artefacts — controllers, services, repositories, request/response records, and
migration scripts — from `target/generated-sources` into `src/main/java` and `src/main/resources`.

After ejecting, the Prefab annotations are stripped from the domain record (or left as inert markers,
since the processor is removed), the `prefab-annotation-processor` dependency is removed from
`pom.xml`, and the project compiles and runs identically to before without any Prefab involvement.

This gives teams full confidence to adopt Prefab for speed knowing they can always exit cleanly
without a painful rewrite.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `mvn prefab:eject` copies all generated sources from `target/generated-sources` into the matching package under `src/main/java`
- [ ] #2 Generated migration scripts are copied to `src/main/resources/db/migration`
- [ ] #3 The goal removes the `prefab-annotation-processor` dependency from `pom.xml` automatically
- [ ] #4 The goal removes or comments out all Prefab-specific annotations from the domain record so the file still compiles cleanly
- [ ] #5 The project builds and all tests pass after ejecting with no changes required from the developer
- [ ] #6 Files that already exist in `src/main/java` (manual overrides) are never overwritten; a warning is printed instead
- [ ] #7 The goal is idempotent: running it twice produces no additional changes
- [ ] #8 A `--dry-run` flag prints what would be moved/modified without making any changes
- [ ] #9 Developer Guide documents the eject workflow with a step-by-step guide and a note on what it means to eject (one-way operation)
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
