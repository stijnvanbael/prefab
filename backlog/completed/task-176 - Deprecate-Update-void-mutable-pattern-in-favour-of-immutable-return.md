---
id: TASK-176
title: Deprecate @Update void mutable pattern in favour of immutable return
status: Done
assignee: []
created_date: '2026-05-08 16:38'
updated_date: '2026-11-24 10:20'
labels: []
dependencies: []
priority: medium
ordinal: 157000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The `@Update void` mutable pattern contradicts Java records' immutability contract and relies on implicit field replacement in the generated service. Deprecate this pattern in favour of the immutable `@Update` that returns a new aggregate instance, and emit a compile-time warning when the mutable form is detected.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The legacy @Update void pattern is documented as deprecated, with Javadoc explaining the preferred immutable alternative; a compiler warning replaces return-type-specific @Deprecated marking
- [x] #2 A compile-time WARNING (not error) is emitted when @Update void is used
- [x] #3 Developer Guide updated to recommend the immutable return pattern as the single best practice
- [x] #4 Deprecation removal is scheduled for the next major version
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Plan

- Document the legacy void update form as deprecated, recommending aggregate-returning methods and scheduling removal for the next major version.
- Emit one method-associated compiler warning per void `@Update`, including paired create-or-update and polymorphic operations, without changing generated behavior.
- Cover warning severity, location, repeat processing, and immutable alternatives with processor tests; update the developer guide and annotation reference.
- Java cannot deprecate only one return-type-specific use of an annotation: do not mark `@Update` itself as deprecated, since immutable updates remain supported.
- Preserve existing async-commit behavior; void async updates also receive the requested warning, and their migration implications must be documented.
- Docker images are unavailable in this environment, so container-backed verification is deferred to CI.

## Implementation Notes

- The processor reports a method-associated warning for every void `@Update`, without changing generated behavior, including upserts, polymorphic aggregates and async commits.
- The `@Update` Javadoc and developer guide deprecate the legacy form and recommend marking existing methods `@Deprecated(forRemoval = true)` with replacement Javadoc. Java cannot automatically apply `@Deprecated` to existing user methods based on return type; the annotation itself remains supported and is not deprecated. The compiler warning is accepted as the alternative to return-type-specific `@Deprecated` marking.
- Compilation tests cover warning severity and location, immutable and async migration, standalone annotation processing, upserts and polymorphic updates.
- The full reactor build passed with tests skipped after rebasing. Targeted update processor tests passed.
- The broader local processor suite had 569 passing tests and one unrelated Windows-only URI/path failure in `AgentContextPluginTest`, recorded as TASK-370. The user confirmed CI is green.
