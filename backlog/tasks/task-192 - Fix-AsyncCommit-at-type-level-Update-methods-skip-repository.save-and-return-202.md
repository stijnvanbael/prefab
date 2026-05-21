---
id: TASK-192
title: >-
  Fix @AsyncCommit at type level: @Update methods skip repository.save() and
  return 202
status: Done
assignee: []
created_date: '2026-05-11 09:16'
updated_date: '2026-05-21 06:21'
labels:
  - bug
  - annotation-processor
  - async-commit
  - 'reported-by:maestro'
dependencies: []
priority: high
ordinal: 34200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem Statement

When `@AsyncCommit` is placed at the **aggregate type level**, the Prefab annotation processor incorrectly applies async-commit semantics to **all** `@Update` methods — even those that should be synchronous. This manifests as two distinct bugs:

1. The generated controller returns `202 Accepted` instead of `200 OK` for `@Update` methods.
2. The generated service does **not** call `repository.save()` for `@Update` methods, so changes are silently lost.

### Current workaround (Maestro)

Moved `@AsyncCommit` from the type level to the specific `@Create` method only.

## Expected Behaviour

| Placement | Affected methods | Expected |
|-----------|-----------------|----------|
| `@AsyncCommit` on `TYPE` | `@Create` methods | Async: return 202, no save |
| `@AsyncCommit` on `TYPE` | `@Update` methods | **Not** affected — synchronous: 200 + save |
| `@AsyncCommit` on specific `@Create` | That method only | Async: return 202, no save |

`@AsyncCommit` at the type level should be shorthand for placing it on every `@Create` method, **not** on `@Update` methods.

## Proposed Fix

In the code generation for `@Update` controller and service methods, only apply async-commit semantics when `@AsyncCommit` is present on the **method itself**, not when it is present only at the type level. Alternatively, emit a compile-time warning when `@AsyncCommit` is placed at type level and recommend method-level placement.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 @AsyncCommit at type level does NOT change the return code or save behaviour of @Update methods
- [ ] #2 @Update methods on an @AsyncCommit aggregate still call repository.save() and return 200 OK
- [ ] #3 @Update void methods annotated directly with @AsyncCommit still return 202 Accepted
- [ ] #4 Processor emits a warning (or error) if @AsyncCommit is placed at type level — recommend method-level placement
- [ ] #5 Integration test: PUT /aggregates/{id} returns 200 and persists the change when the aggregate has @AsyncCommit @Create
- [ ] #6 Workaround of moving @AsyncCommit to method level in consuming projects can be retained as the idiomatic style
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fixed in Prefab 0.8.0 (no tracked task existed at the time). Confirmed resolved: Task.java and Capability.java both use @AsyncCommit at type level; their @Update methods continue to call repository.save() and return 200 OK correctly.
<!-- SECTION:NOTES:END -->
