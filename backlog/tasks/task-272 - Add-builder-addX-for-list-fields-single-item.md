---
id: TASK-272
title: Add builder addX methods for list fields
status: To Do
assignee: []
created_date: '2026-08-06 11:07'
labels:
  - annotation-processor
  - builder
dependencies: []
priority: medium
---

## Description

Extend generated builders so list fields expose an `addX(...)` method that appends a single item, in addition to existing methods that set or replace the whole list.

The goal is to improve builder ergonomics for incremental list construction while preserving current list setter behaviour.

## Acceptance Criteria

- [ ] #1 For each list field `x`, generated builders include an `addX(item)` method that appends one item to the field list
- [ ] #2 `addX(...)` participates in fluent chaining and returns the builder type
- [ ] #3 `addX(...)` handles initialisation when the list has not been set yet, without requiring a prior full-list setter call
- [ ] #4 Existing list setter methods and generated constructor/build behaviour remain backward compatible
- [ ] #5 Regression tests cover generated source shape and runtime behaviour for repeated `addX(...)` calls and mixed `withX(...)` + `addX(...)` usage

## Analysis

- `BuilderWriter` currently focuses on generating setter-style builder methods.
- List-field metadata is already available during generation and can be used to conditionally emit `addX(...)` methods only for list-typed fields.
- Method naming should follow the existing configured setter prefix conventions where applicable, while `addX` remains explicit and predictable.

