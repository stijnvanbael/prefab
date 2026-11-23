---
id: TASK-175.02
title: In-memory repositories on Spring Data KeyValue
status: To Do
assignee: []
created_date: '2026-11-23 11:00'
labels:
  - test
dependencies:
  - TASK-175.01
parent_task_id: TASK-175
priority: high
type: task
ordinal: 156200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Back every generated repository with Spring Data KeyValue (`MapKeyValueAdapter`) in in-memory mode (TASK-175 design §3, decision-011). Spike: KeyValue 4.1.3 has derived queries, paging, sorting and a settable `KeySpaceResolver`, but no Query by Example, no optimistic locking, and its configuration extension only claims `KeyValueRepository`/`@KeySpace` types in strict mode.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Every generated repository and `@RepositoryMixin` interface gets an in-memory bean, also with JDBC or MongoDB on the classpath (own registrar with `KeyValueRepositoryFactoryBean`)
- [ ] #2 Derived queries, paging and sorting work, including on `Reference<T>` properties
- [ ] #3 Query by Example supports the matchers the generated list services use (`exact`, `contains`, `startsWith`, `endsWith`, `regex`, `ignoreCase`, ignored paths)
- [ ] #4 `@Version` is checked and incremented on records; a stale version raises `OptimisticLockingFailureException`, so the generated service returns `409 Conflict`
- [ ] #5 All implementations of a sealed aggregate share the sealed root's keyspace
- [ ] #6 In-memory `RangeQueryExecutor` and `FullTextQueryExecutor` beans exist; full-text matching and relevance ordering are documented as an approximation
- [ ] #7 The processor emits runtime autocomplete metadata on generated `autocompleteBy*` methods and the in-memory backend honours scan mode and match strategy
- [ ] #8 A method with a native JDBC `@Query` or Mongo `@Query`/`@Aggregation` logs a WARN at startup and throws `UnsupportedOperationException` naming the method and `@IntegrationTest(containers = true)` when called
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
