---
id: TASK-337.03
title: 'Showcase catalog: REST CRUD, validation, list endpoints and search'
status: To Do
assignee: []
created_date: '2026-11-20 19:47'
labels:
  - examples
  - rest
dependencies:
  - TASK-337.02
parent_task_id: TASK-337
priority: high
type: task
ordinal: 174000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Shop catalog (products, categories) in `examples/showcase` covering the REST side of Prefab. Replaces the scope of TASK-043 and TASK-044 and the product/invoice/article/shape parts of the old kafka and mongodb examples.

Cover: `@Create`, `@Update`, `@Delete`, `@GetById`, validation (Jakarta + `@FileSize`/`@ContentType`), `@GetList` with `@Filter` (equality, range, full text via `@Text`), paging and `@SortOrder`, `@Autocomplete`, `@Parent` (nested resource, e.g. product variants), polymorphic aggregates via sealed interfaces, `@Download` (product image), audit fields (`@CreatedAt`, `@LastModifiedBy`, ...), `@Versioned` optimistic locking, `@RepositoryMixin`.

Each feature gets an integration test; tests that depend on persistence-specific support are marked with `@RequiresCapability` (TASK-337.02).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Catalog demonstrates every listed REST feature with an integration test
- [ ] #2 Tests for persistence-specific features declare the capability they need
- [ ] #3 Generated test clients are used in tests, no hand-written HTTP calls
<!-- AC:END -->
