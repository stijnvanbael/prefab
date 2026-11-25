---
id: TASK-390
title: Validate PostgreSQL migration schema transitions at compile time
status: To Do
assignee: []
created_date: '2026-11-25 09:51'
labels:
  - persistence
  - migrations
dependencies:
  - TASK-383
references:
  - backlog/docs/database-migrations.md
priority: medium
type: feature
ordinal: 215000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
As a developer changing a PostgreSQL schema managed by Prefab, I want compilation to identify invalid schema
changes in versioned migrations, so I can fix the offending statement before Flyway runs and before Prefab generates
a new migration from an incorrect schema.

TASK-383 added a check for dropping a nonexistent column. Other invalid transitions can still overwrite or be
silently ignored by the replay model: for example, adding a duplicate column, renaming a table to an occupied name,
dropping an unknown index, or creating a foreign key that references a missing column.

Extend the annotation processor's existing PostgreSQL migration replay. Check supported DDL against the schema
produced by earlier statements, including earlier statements in the same migration. Keep this database-free and
limited to schema facts the processor can establish from the replayed history. TASK-388 separately tracks reconstructing
inline foreign keys for migration generation; reuse that metadata where relevant rather than duplicating the fix.

Out of scope: validating DML or existing data, executing migrations, guaranteeing arbitrary PostgreSQL SQL will run,
and changing MongoDB migration behavior.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Compilation rejects duplicate table, column, index, and named constraint definitions covered by replay, and table or column renames to an already occupied name.
- [ ] #2 Compilation rejects unguarded drops of missing tables, columns, indexes, and supported named constraints. Where PostgreSQL supports `IF EXISTS` or `IF NOT EXISTS`, a guard that makes an operation a no-op preserves the prior schema instead of failing or replacing the existing definition.
- [ ] #3 For supported index and key declarations, compilation rejects references to missing tables or columns, including both the local and referenced sides of foreign keys. A reference established by an earlier statement is accepted.
- [ ] #4 Diagnostics identify the migration filename, offending statement, affected object, and reason. Processor tests cover each failure category and valid counterparts, including guarded no-ops, same-file ordering, and the existing missing-column check.
- [ ] #5 The database migration guide lists the supported checks and their limits, including what happens when Prefab cannot model a statement. Update the relevant agent guidance and benchmark coverage for the changed compile-time behavior.
<!-- AC:END -->
