---
id: TASK-267
title: Duplicate migration generated after renaming a copied generated SQL migration
status: Done
assignee: []
created_date: '2026-07-23 12:14'
labels:
  - annotation-processor
  - dbmigration
  - bug
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
### Steps to reproduce

1. Start with an `@Aggregate` annotated with `@DbMigration`. The annotation processor generates
   `V1__generated.sql` in `target/generated-sources/…/db/migration/`.
2. Copy that file to `src/main/resources/db/migration/` and rename it — e.g. to
   `V1__initial_schema.sql` — so that the migration is now version-controlled and owned by the user.
3. Delete (or leave absent) the `V1__generated.sql` artefact.
4. Recompile.

**Expected:** the processor detects that the schema described by `V1__initial_schema.sql` is already
fully applied and emits no new migration (or an empty one).

**Actual:** the processor generates `V2__generated.sql` containing the exact same `CREATE TABLE` DDL
as `V1__initial_schema.sql`, causing Flyway to fail at startup with a duplicate-object / table-already-
exists error.

### Root cause

`DbMigrationWriter.existingGeneratedMigrations()` probes the classpath exclusively for files whose name
matches `V{n}__generated.sql`. Any migration with a different suffix — including a renamed copy of a
previously generated file — is ignored when reconstructing the current database state. As a result the
processor believes the schema does not yet exist and regenerates it from scratch.

`latestMigrationVersion()` *does* scan all `V{n}__*.sql` filenames in the migration directory to pick
the correct next version number, but this does not help because the state-diff logic compares the
desired schema against only the content of `__generated` files.

### Fix direction

`currentDatabaseState()` should read **all** `V{n}__*.sql` files present in the migration directory
(regardless of the name suffix after `__`) when reconstructing the "already applied" table state, not
only those ending in `__generated`. The name after the version prefix is user-controlled and must be
treated as irrelevant to the state comparison.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Copying a generated migration to `src/main/resources/db/migration/` and renaming it (e.g. `V1__generated.sql` → `V1__initial_schema.sql`) then recompiling produces no new migration containing the same DDL
- [ ] #2 If the schema genuinely changes after the rename (e.g. a new field is added to the aggregate), the processor still generates a new incremental migration with only the delta DDL
- [ ] #3 The version number of any newly generated migration is always higher than the highest existing `V{n}__` file, regardless of the suffix used in earlier migrations
- [ ] #4 The fix is covered by a unit / compilation test that places a renamed migration on the classpath and asserts no duplicate DDL is produced
- [ ] #5 Existing tests for `DbMigrationWriter` continue to pass unchanged
- [x] #1 Copying a generated migration to `src/main/resources/db/migration/` and renaming it (e.g. `V1__generated.sql` → `V1__initial_schema.sql`) then recompiling produces no new migration containing the same DDL
- [x] #2 If the schema genuinely changes after the rename (e.g. a new field is added to the aggregate), the processor still generates a new incremental migration with only the delta DDL
- [x] #3 The version number of any newly generated migration is always higher than the highest existing `V{n}__` file, regardless of the suffix used in earlier migrations
- [x] #4 The fix is covered by a unit / compilation test that places a renamed migration on the classpath and asserts no duplicate DDL is produced
- [x] #5 Existing tests for `DbMigrationWriter` continue to pass unchanged
<!-- AC:END -->

## Implementation Notes

- 2026-07-23: Started investigation.
- Confirmed root cause in `annotation-processor/src/main/java/be/appify/prefab/processor/dbmigration/DbMigrationWriter.java`:
  `currentDatabaseState()` reads only `existingGeneratedMigrations()` (`V{n}__generated.sql`), while
  `latestMigrationVersion()` already inspects all `V{n}__*.sql`.
- Plan: replace generated-only state loading with scan of all migration files in the migration directory, ordered by
  version and parsed into current table state; add regression tests for renamed migrations and incremental delta after
  rename.
- Implemented `existingMigrations(...)` to read all `V{n}__*.sql` files from the discovered migration directory,
  ordered by version and filename before parsing.
- `currentDatabaseState()` now reconstructs table state from all migration suffixes, not only `__generated` files.
- Added regression coverage in `DbMigrationWriterTest`:
  - renamed migration on classpath (`V1__initial_schema.sql`) does not generate duplicate DDL
  - schema delta after rename still generates an incremental migration with version higher than existing migrations
- Added changed-schema fixture at
  `annotation-processor/src/test/resources/dbmigration/indexed_renamed/source/Product.java`.
- Verification:
  - `mvn -pl annotation-processor -Dtest=DbMigrationWriterTest test`
  - `mvn -pl annotation-processor test`

