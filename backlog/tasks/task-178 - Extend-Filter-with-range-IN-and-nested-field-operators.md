---
id: TASK-178
title: 'Extend @Filter with range, IN, and nested field operators'
status: To Do
assignee: []
created_date: '2026-05-08 16:38'
updated_date: '2026-05-21 06:22'
labels: []
dependencies: []
priority: medium
ordinal: 159000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Extend `@Filter` beyond the current string-match operators to support range comparisons, collection membership (`IN`), and nested field paths. This reduces the need to reach for `@RepositoryMixin` for common query patterns.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 @Filter supports IN operator (matches any value in a provided list)
- [x] #2 @Filter supports GT, GTE, LT, LTE operators for numeric and date fields
- [ ] #3 @Filter supports nested field filtering via dot notation (e.g. @Filter("address.city"))
- [ ] #4 All new operators are tested with PostgreSQL and MongoDB backends
- [ ] #5 Developer Guide updated with all new operators and examples
<!-- AC:END -->

## Implementation Notes

### GT/GTE/LT/LTE (done)
- Query by Example (`ExampleMatcher`) has no range matchers and one value per property, so ranges cannot use it. Spring Data JDBC repositories also cannot take `Criteria`/`Specification`.
- Solution: core `RangeQueryExecutor` + `RangeCondition`; `JdbcRangeQueryExecutor` (postgres, maps the Example with `RelationalExampleMapper` and ANDs `Criteria` via `JdbcAggregateOperations.findAll(Query, Class, Pageable)`) and `MongoRangeQueryExecutor` (mongodb). Generated service only uses it when a range operator is present; otherwise output is unchanged.
- Parameters are named `<field>GreaterThan|GreaterThanOrEqual|LessThan|LessThanOrEqual` and typed as the field type; non-comparable field types are a compile error.
- Tests: `RangeFilterTest` (processor), `JdbcRangeQueryExecutorTest` (Postgres Testcontainers), `InvoiceRangeFilterIntegrationTest` in kafka and mongodb examples (Mongo/Kafka images were unavailable locally, not yet run).
- Remaining: IN, nested field paths, MongoDB verification, Developer Guide.

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
