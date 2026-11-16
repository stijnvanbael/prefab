---
id: TASK-319
title: Support cross-aggregate read joins and projections without merging write boundaries
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [persistence, feature]
dependencies: []
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Provide a documented, supported way to expose read-only DTO projections combining data from several aggregates. Preserve independent aggregate write/transaction boundaries and compare synchronous repository joins with asynchronously maintained read models.

Related: TASK-170 (`@ReadModel` intent), archived TASK-034 (empty projection epic), existing `@RepositoryMixin` and streams join tasks. Streams joins do not themselves deliver database-backed query projections; avoid duplicating their DSL.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Define ownership, result types and extension points for read-only projections; use existing repository/query facilities before adding new annotations.
- [ ] #2 Demonstrate a two-aggregate PostgreSQL projection with filters, stable ordering and pagination without N+1 loading.
- [ ] #3 Apply tenant and row-authorization restrictions before join/pagination, including counts; unauthorized joined data never leaks.
- [ ] #4 Document consistency, missing-related-record and cardinality semantics and when an event-driven read model is preferable.
- [ ] #5 Compile/runtime tests verify mapping, pagination/counts and isolation; unsupported backends have explicit limits rather than broken generated queries.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Evaluate repository mixins and read-model examples, specify one bounded projection contract, implement a tested query/API slice and document the write-boundary trade-off.
<!-- SECTION:PLAN:END -->
