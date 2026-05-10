---
id: TASK-187
title: Add @DbColumn annotation for custom SQL column types on aggregate fields
status: In Progress
assignee: []
created_date: '2026-05-10 09:24'
updated_date: '2026-05-10 10:18'
labels:
  - feature
  - annotation-processor
dependencies: []
references:
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/backlog/completed/support-custom-db-column-type.md
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a new @DbColumn field annotation that allows aggregate fields to opt into explicit SQL column DDL types and optional converter auto-registration. This is needed to support custom PostgreSQL extension types (for example pgvector vector(N), PostGIS geometry, hstore) and to avoid compile-time failures for otherwise unsupported Java field types when DbMigration processing evaluates field mappings.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `@DbColumn` annotation exists in `prefab-core` with runtime retention for aggregate field/record-component usage.
- [ ] #2 When `@DbColumn` is present on a record component, annotation processing does not fail with unsupported built-in type validation for that component.
- [ ] #3 When `@DbMigration` is enabled, generated SQL uses `@DbColumn.type()` as the column type in the generated Flyway migration.
- [ ] #4 When `@DbColumn.converter()` is specified (non-void), the converter class is auto-registered through Prefab JDBC conversion infrastructure at startup.
- [ ] #5 Compilation fails with a clear error if `@DbColumn.type()` is blank.
- [ ] #6 `@DbColumn` works for `float[]`, `Float[]`, `byte[]`, and user-defined record/class field types.
- [ ] #7 Developer guide docs are updated with `@DbColumn` reference and a pgvector-oriented usage example.
<!-- AC:END -->
