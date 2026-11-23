---
id: TASK-337.03
title: 'Showcase catalog: REST CRUD, validation, list endpoints and search'
status: Done
assignee:
  - '@claude'
created_date: '2026-11-20 19:47'
updated_date: '2026-11-21 18:30'
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
- [x] #1 Catalog demonstrates every listed REST feature with an integration test
- [x] #2 Tests for persistence-specific features declare the capability they need
- [x] #3 Generated test clients are used in tests, no hand-written HTTP calls
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Catalog domain in `examples/showcase/src/main/java/.../catalog`: `Category` (autocomplete, default sort), `Product` (CRUD, validation, equality/range/full-text filters, `@Text`, image upload + download, `AuditInfo`, `@Version`), `ProductVariant` (`@Parent`), `Promotion` (sealed interface), `ProductRepositoryMixin`.
2. One integration test class per feature, using only the generated clients for HTTP; `@RequiresCapability` on range filter, full-text, polymorphic and binary download tests.
3. Run the tests on `showcase` (Postgres + Kafka) and `showcase-mongodb`; fix what Prefab gets wrong, with a regression test in the module that owns the bug, or file a task when the fix is broader.
4. Feature table in `examples/README.md`.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
**Catalog** (`be.appify.prefab.example.showcase.catalog`): `Category`, `Product`, `ProductVariant`, `Promotion` (`PercentageOff`, `AmountOff`) and `ProductRepositoryMixin`. Tests: `ProductIntegrationTest` (CRUD), `ProductValidationIntegrationTest`, `ProductListIntegrationTest` (filters, paging, sorting), `CategoryIntegrationTest`, `AutocompleteIntegrationTest`, `ProductVariantIntegrationTest`, `PromotionIntegrationTest`, `ProductImageIntegrationTest`, `ProductAuditIntegrationTest`, `ProductOptimisticLockingIntegrationTest`, `ProductRepositoryMixinIntegrationTest`. All 57 showcase tests pass on `showcase` and `showcase-mongodb` (Docker was available in this session). `examples/README.md` maps each feature to its source and test.

Choices and deviations from the description:
- "`@Versioned` optimistic locking": `@Versioned` versions component types nested in events. Optimistic locking is Spring Data's `@Version`; the test shows the version increment and that saving a stale copy through the generated repository fails with `OptimisticLockingFailureException`.
- "full text via `@Text`": `@Text` only maps a column to `TEXT`. Full text is `@Filter(operator = FULL_TEXT)` over name (weight A) and description; `@Text` is on the description.
- Validation errors are asserted through the generated clients: they fail with `Status expected:<...> but was:<400>` when the API rejects a request.
- `@RepositoryMixin` and optimistic locking are shown on the generated repository, not over HTTP.
- With `@GetList(sorted = ...)` a full-text search is ordered by that default, not by relevance; the ranking test asks for `sort=relevance`.
- No capability gaps: MongoDB supports everything the catalog uses after the fixes below, so the coverage matrix is unchanged.

**Prefab bugs found and fixed** (each with a regression test, in separate `fix:` commits):
1. Processor: a primitive with a type-use constraint (`@Positive int`) in a polymorphic aggregate crashed the JDBC reading converter ("not a valid name"): primitive names came from `TypeMirror.toString()`.
2. Processor: `@Update` taking a `Binary` did not compile; the update service now maps request parameters through the plugins (`StorageService`), like create.
3. Processor: the generated test client of a multipart `@Update` returned a value from a `void` method and always sent POST.
4. Core: the default `AuditContextProvider` was never registered: `AuditConfiguration` was in the package `PrefabCoreConfiguration` component-scans, so its `@ConditionalOnBean` ran too early. Moved to `be.appify.prefab.core.audit`.
5. Core/processor: a weighted full-text index (`setweight(...) || setweight(...)`) was invalid SQL for PostgreSQL; the shared tsvector expression is parenthesised and the migration parser reads it back.
6. Processor: list endpoints of polymorphic aggregates returned elements without `type`; subtype responses now carry it as an existing property.
7. Core/processor: `@ContentType`/`@FileSize` were never enforced on uploads; the controller's `MultipartFile` parameter now carries them, with new `MultipartFile` validators. `FileSizeValidator` no longer throws on null.
8. MongoDB: an aggregate with a `Binary` field failed to start; the `File` converters moved from prefab-postgres to `be.appify.prefab.core.binary` and are registered for MongoDB, plus a BSON binary reading converter.
9. prefab-test: the MongoDB listener dropped collections before each test, which dropped the text index too; it now removes documents.

**Follow-up tasks**: TASK-008 (described now: child get/update/delete ignore the parent id in the path, wrong `Location` header; the showcase leaves out a wrong-parent test until then), TASK-351 (`@Text` parameters still limited to 255 characters: `Product.description` sets `@Size(max = 2000)` explicitly), TASK-352 (`createdAt`/`lastModifiedAt` differ by microseconds on create; the audit test uses `isCloseTo`), TASK-353 (`@Download` without `@GetById` does not compile).
<!-- SECTION:NOTES:END -->
