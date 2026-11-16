---
id: TASK-318
title: Support advanced PostgreSQL indexes and custom check constraints
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [persistence, feature]
dependencies: []
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Extend declarative persistence metadata and migration replay/diffing to support composite and partial indexes, explicitly configured GIN indexes and custom CHECK constraints. Current `@Indexed` is field-scoped with a `unique` flag; specialized full-text/trigram GIN generation already exists and must be reused, not duplicated.

Related: completed TASK-040, TASK-030 and TASK-246; TASK-075 (constraint ownership) and TASK-316 (online DDL). PostgreSQL is the initial target; unsupported backend combinations must fail clearly.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Define aggregate-level metadata for ordered composite keys, uniqueness, predicates, index method/operator classes and named CHECK expressions without overloading unrelated annotations.
- [ ] #2 Generated migrations create, rename/change and remove these objects deterministically, replay manually authored equivalents, and produce no redundant migration on an unchanged rebuild.
- [ ] #3 Validate referenced columns, duplicate/conflicting names and unsupported methods; SQL expressions are trusted developer metadata, never runtime request fragments.
- [ ] #4 Cover composite unique, partial, JSONB/array GIN and multi-column CHECK examples with real PostgreSQL tests, including expression/predicate changes and failed constraint validation.
- [ ] #5 Preserve specialized search indexes and existing `@Indexed` behavior; document backend limits, naming, lock implications and manual escape hatches.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Inventory index/constraint models and parser limits, specify metadata, extend shared schema diff/replay and test round trips before adding examples and online-DDL guidance.
<!-- SECTION:PLAN:END -->
