---
id: TASK-133
title: 'Bug: DbMigration uses hardcoded "id" instead of the @Id annotated field name'
status: Done
assignee: []
created_date: '2026-04-20'
updated_date: '2026-04-22 13:38'
labels:
  - bug
  - postgres
  - annotation-processor
dependencies: []
priority: high
ordinal: 3000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The generated Flyway migration script always uses `"id"` as the primary key column name, regardless of what the
`@Id` annotated field is actually called on the aggregate root. This means aggregates whose identity field has a
name other than `id` (e.g. `productId`, `reference`, `key`) will get an incorrect migration script, leading to
runtime errors.

The migration writer must inspect the aggregate's fields, locate the one annotated with `@Id`, and use that
field's name as the primary key column name.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The generated migration script uses the name of the `@Id` annotated field as the primary key column name
- [ ] #2 An aggregate whose `@Id` field is named `id` continues to produce a correct migration (no regression)
- [ ] #3 An aggregate whose `@Id` field has a different name (e.g. `productId`) produces a migration with that name as the primary key column
- [ ] #4 Tests cover both the default `id` case and a custom `@Id` field name
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Locate where the migration writer hardcodes the `"id"` column name
2. Replace the hardcoded value with a lookup of the field annotated with `@Id` on the aggregate
3. Add a test source file with an aggregate whose `@Id` field is not named `id`
4. Add tests to `DbMigrationWriterTest` that assert the correct primary key column name is used
<!-- SECTION:PLAN:END -->
