---
id: TASK-295
title: Generated getList overload with fluent query builder
status: To Do
assignee: []
created_date: '2026-10-15 13:07'
labels:
  - rest
  - getlist
  - dsl
dependencies:
  - TASK-178
  - TASK-292
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
For every aggregate annotated with `@GetList`, generate a `<Aggregate>Query` class with a fluent builder, and a `getList(<Aggregate>Query)` overload on the generated service. This lets application code query aggregates programmatically without having to pass long positional parameter lists, and without depending on the HTTP layer.

Example:

```java
alertService.getList(AlertQuery.newQuery()
    .page(1)
    .pageSize(20)
    .sort(Sort.by(Direction.DESCENDING, "executionTimestamp"))
    .executionTimestampGreaterThan(Instant.now().minus(Duration.ofDays(30)))
    .status(Status.OPEN));
```

The builder exposes one method per `@Filter` parameter, using the same naming as the REST request parameters (including the range suffixes such as `GreaterThan`, `LessThanOrEqual`), plus `page`, `pageSize` and `sort`. Methods take the real field type rather than a string. Unset filters are ignored. The overload delegates to the existing getList logic, so tenant, parent, default sorting and range filters behave identically.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An `<Aggregate>Query` class with a static `newQuery()` and fluent builder methods is generated for each `@GetList` aggregate
- [ ] #2 The builder has `page`, `pageSize` and `sort` methods, with sensible defaults when omitted
- [ ] #3 One builder method per `@Filter` (equality, contains, range operators), typed with the field type and named like the REST parameter
- [ ] #4 The generated service has a `getList(<Aggregate>Query)` overload returning the same page type as the existing method
- [ ] #5 Filters not set on the builder are ignored; combinations of filters give the same results as the equivalent REST request
- [ ] #6 Works for aggregates with a tenant or parent, and respects default sorting (TASK-292) when no sort is given
- [ ] #7 Existing generated `getList` signature and REST behaviour are unchanged
- [ ] #8 Processor tests cover the generated query class and overload; an example integration test exercises it end to end
- [ ] #9 Documented in the annotation reference / developer guide
<!-- AC:END -->
