---
id: TASK-129
title: >-
  Support resolving @Aggregate-typed parameters in @Create and @Update generated
  service methods
status: Done
assignee: []
created_date: '2026-04-18 11:29'
updated_date: '2026-04-30 06:04'
labels:
  - annotation-processor
  - rest
dependencies: []
priority: high
ordinal: 6000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When resolveReadOnly() was removed from Reference, aggregate records that need related entity data in their @Create constructors or @Update methods have no framework-supported way to fetch it. An AggregateParameterPlugin should detect parameters of @Aggregate-annotated types and automatically resolve them from their repository in the generated service.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A @Create constructor parameter of an @Aggregate-annotated type is resolved from its repository using the ID from the request body
- [x] #2 An @Update method parameter of an @Aggregate-annotated type is pre-fetched from the aggregate's Reference field before the method is called
- [x] #3 The corresponding repository is automatically injected as a service dependency when aggregate-typed parameters are detected
- [x] #4 Aggregate-typed @Update parameters are excluded from the generated request record
- [x] #5 Existing generated code for non-aggregate parameters is unaffected
<!-- AC:END -->
