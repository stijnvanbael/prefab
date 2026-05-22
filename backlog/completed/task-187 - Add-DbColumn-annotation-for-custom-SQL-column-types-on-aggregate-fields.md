---
id: TASK-187
title: Add @DbColumn annotation for custom SQL column types on aggregate fields
status: Done
assignee: []
created_date: '2026-05-10 09:24'
updated_date: '2026-05-21 06:21'
labels:
  - feature
  - annotation-processor
dependencies: []
references:
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/backlog/completed/support-custom-db-column-type.md
priority: high
ordinal: 37200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a new @DbColumn field annotation that allows aggregate fields to opt into explicit SQL column DDL types and optional converter auto-registration. This is needed to support custom PostgreSQL extension types (for example pgvector vector(N), PostGIS geometry, hstore) and to avoid compile-time failures for otherwise unsupported Java field types when DbMigration processing evaluates field mappings.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `@DbColumn` annotation exists in `prefab-core` with runtime retention for aggregate field/record-component usage.
- [x] #2 When `@DbColumn` is present on a record component, annotation processing does not fail with unsupported built-in type validation for that component.
- [x] #3 When `@DbMigration` is enabled, generated SQL uses `@DbColumn.type()` as the column type in the generated Flyway migration.
- [x] #4 When `@DbColumn.converter()` is specified (non-void), the converter class is auto-registered through Prefab JDBC conversion infrastructure at startup.
- [x] #5 Compilation fails with a clear error if `@DbColumn.type()` is blank.
- [x] #6 `@DbColumn` works for `float[]`, `Float[]`, `byte[]`, and user-defined record/class field types.
- [x] #7 Developer guide docs are updated with `@DbColumn` reference and a pgvector-oriented usage example.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Next: run broader module test selection (or full reactor tests) and decide whether TASK-187 should be marked Done or kept In Progress until pgvector runtime integration coverage is added.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Started implementation in commit `a46f4c46` with core annotation/API, annotation-processor handling, postgres converter wiring, tests, and developer guide updates.

Added `@DbColumn` annotation and `DbColumnConverterContributor` interface in `core` and wired contributors into `PrefabJdbcConfiguration.userConverters()` in `postgres`.

Extended db migration generation so `@DbColumn(type=...)` emits custom SQL type and blank `type()` emits a compile-time error on the annotated field.

Added processor support for converter contributor generation via `DbColumnConverterContributorWriter` and guarded unresolved converter types to avoid processor crashes.

Expanded db migration tests with fixtures for converter registration, `Float[]`, `byte[]`, and custom record field mapping (`EmbeddingVariants`).

Validation run: `mvn -pl annotation-processor -Dtest=DbMigrationWriterTest test` and `mvn -pl core,postgres -DskipTests compile`.

Validated against Maestro integration flows after upgrading to `prefab 0.7.8-SNAPSHOT`: `@DbColumn(type="vector(1536)", converter=FloatArrayToVectorConverter.class)` on `MemoryEntry.embedding` now compiles/runs and persists pgvector correctly.

Confirmed downstream fix by running targeted tests: `MemoryEntryIntegrationTest#createMemoryEntry_shouldPersistAndBeRetrievable` and `WorkItemIntegrationTest#failWorkItem_shouldTransitionToFailedWithReason` passed after regeneration.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Shipped `@DbColumn` support through Prefab 0.7.8-SNAPSHOT and verified runtime behavior in a consumer project (Maestro) with pgvector-backed `float[]` embedding persistence.
<!-- SECTION:FINAL_SUMMARY:END -->
