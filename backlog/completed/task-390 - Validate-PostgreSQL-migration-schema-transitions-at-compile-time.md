---
id: TASK-390
title: Validate PostgreSQL migration schema transitions at compile time
status: Done
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

## Analysis

Extend the PostgreSQL replay state as the authority for supported table, column, index, and named-constraint
definitions. Validate each parsed DDL operation against that state before applying it, retaining the state on
guarded no-ops. Keep diagnostics tied to the migration file and statement, and test successful ordering and
guarded cases alongside failures. Reuse replayed key metadata for foreign-key references, and document unsupported
or unmodelled statements as outside the compile-time guarantee.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Compilation rejects duplicate table, column, index, and named constraint definitions covered by replay, and table or column renames to an already occupied name.
- [x] #2 Compilation rejects unguarded drops of missing tables, columns, indexes, and supported named constraints. Where PostgreSQL supports `IF EXISTS` or `IF NOT EXISTS`, a guard that makes an operation a no-op preserves the prior schema instead of failing or replacing the existing definition.
- [x] #3 For supported index and key declarations, compilation rejects references to missing tables or columns, including both the local and referenced sides of foreign keys. A reference established by an earlier statement is accepted.
- [x] #4 Diagnostics identify the migration filename, offending statement, affected object, and reason. Processor tests cover each failure category and valid counterparts, including guarded no-ops, same-file ordering, and the existing missing-column check.
- [x] #5 The database migration guide lists the supported checks and their limits, including what happens when Prefab cannot model a statement. Update the relevant agent guidance and benchmark coverage for the changed compile-time behavior.
<!-- AC:END -->

## Implementation Notes

Extended database-free PostgreSQL migration replay to reject invalid supported DDL transitions with migration-specific diagnostics. Replay now preserves schema state for guarded no-ops, validates table/column/index/constraint names and foreign-key references, and fails explicitly for unsupported schema statements. Added parser and processor diagnostic regression coverage, updated the migration guide and agent cheat sheet, and added the `persistence-fix-duplicate-migration-table` benchmark.

Verification: `mvn -pl annotation-processor "-Dtest=SqlStatementParserTest,ProcessorFailureDiagnosticsTest" test` passed (38 tests). Migration-writer round-trip tests, benchmark catalog/workspace tests, and direct benchmark reference/starter acceptance checks also passed; the benchmark CLI and full benchmark module suite have unrelated Windows-specific harness failures.
