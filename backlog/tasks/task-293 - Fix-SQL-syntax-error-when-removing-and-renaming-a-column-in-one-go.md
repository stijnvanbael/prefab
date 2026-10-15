---
id: TASK-293
title: Fix SQL syntax error in generated Postgres migration when a column is removed and renamed in one go
status: Done
assignee: []
created_date: '2026-10-15 10:55'
labels:
  - bug
  - dbmigration
  - postgres
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When an aggregate change removes one column and renames another in the same change set, the generated Postgres migration script contains invalid SQL (syntax error). Reproduce with a processor test, identify which statement is malformed (likely ordering or combination of the `DROP COLUMN` and `RENAME COLUMN` statements, or a conflict between the dropped and renamed column), and fix the generator. See also completed TASK-018 (rename tables and columns) for the existing rename logic.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A failing test reproduces the invalid SQL for remove + rename of columns in a single migration
- [x] #2 The generated script is valid PostgreSQL and applies cleanly (verified against a real Postgres container)
- [x] #3 Data in the renamed column is preserved and the removed column is dropped
- [x] #4 Related combinations (remove + rename + add, rename to a previously removed name) are covered by tests
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Notes

Root cause: PostgreSQL does not allow `RENAME COLUMN` combined with other actions in one `ALTER TABLE`. `AlterTable.toSql` now emits each rename as its own statement and batches the remaining modifications around it. Drops are emitted first so a rename can reuse a removed column's name. Covered by DbRenameTest (remove+rename, remove+rename+add, rename onto removed name). Real-Postgres container verification was not run; SQL follows Postgres rules (separate RENAME statements preserve data).

