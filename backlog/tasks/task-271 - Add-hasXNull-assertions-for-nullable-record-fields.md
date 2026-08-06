---
id: TASK-271
title: Add hasXNull assertions for nullable record fields
status: To Do
assignee: []
created_date: '2026-08-06 11:03'
labels:
  - annotation-processor
  - assertions
dependencies: []
priority: medium
---

## Description

Extend assertion generation so nullable record fields expose a `hasXNull()` assertion in addition to the existing `hasXSatisfying(...)` assertion.

This should improve readability for null checks while keeping nested assertions available for non-null values.

## Acceptance Criteria

- [ ] #1 For nullable record fields, generated assertion classes include `hasXNull()` methods with the same naming convention as `hasXSatisfying(...)`
- [ ] #2 `hasXNull()` fails with a clear assertion message when the field value is not `null`
- [ ] #3 Existing `hasXSatisfying(...)` generation and behaviour remain unchanged for nullable fields
- [ ] #4 Regression tests cover generated source shape and runtime assertion behaviour for `hasXNull()` on nullable fields

## Analysis

- `AssertionWriter` currently emits `hasXSatisfying(...)` for nested assertions on record fields.
- Nullability metadata already exists in model generation and should be reused to conditionally generate `hasXNull()` only where applicable.
- Generated API should remain fluent and consistent with existing AssertJ-style custom assertions.

