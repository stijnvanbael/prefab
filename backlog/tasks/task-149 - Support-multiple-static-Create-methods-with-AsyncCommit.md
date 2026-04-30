---
id: TASK-149
title: Support multiple static @Create methods with @AsyncCommit
status: Done
assignee: []
created_date: '2026-04-30 09:52'
updated_date: '2026-04-30 13:50'
labels:
  - annotation-processor
  - async-commit
dependencies: []
priority: medium
ordinal: 4000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
An aggregate annotated with `@AsyncCommit` (or with method-level `@AsyncCommit`) should be able to declare more than
one public static `@Create` factory method. Currently the processor logs an error and discards all but the first
factory when multiple such methods are detected.

Each factory method generates its own named REST endpoint, service method, and request record — using the factory
method name as the operation name (consistent with how `@Update` methods work).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Remove the compile-time error that rejects aggregates with more than one async `@Create` static factory method
- [ ] #2 Each async `@Create` factory generates a dedicated controller method named after the factory method
- [ ] #3 Each async `@Create` factory generates a dedicated service method named after the factory method
- [ ] #4 Each async `@Create` factory with body parameters generates a dedicated request record named after the factory
  method (e.g. `PlaceOrderRequest`)
- [ ] #5 Existing single-factory behaviour is preserved: the factory method name is used for both the controller and
  service method
- [ ] #6 A test aggregate with two `@Create @AsyncCommit` factory methods compiles successfully and generates one
  controller method and one service method per factory
<!-- AC:END -->
