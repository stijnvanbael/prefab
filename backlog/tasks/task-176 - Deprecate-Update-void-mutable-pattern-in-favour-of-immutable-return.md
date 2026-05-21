---
id: TASK-176
title: Deprecate @Update void mutable pattern in favour of immutable return
status: To Do
assignee: []
created_date: '2026-05-08 16:38'
updated_date: '2026-05-21 06:22'
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
- [ ] #1 @Update void mutable pattern is marked @Deprecated with a Javadoc explaining the preferred immutable alternative
- [ ] #2 A compile-time WARNING (not error) is emitted when @Update void is used
- [ ] #3 Developer Guide updated to recommend the immutable return pattern as the single best practice
- [ ] #4 Deprecation removal is scheduled for the next major version
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
