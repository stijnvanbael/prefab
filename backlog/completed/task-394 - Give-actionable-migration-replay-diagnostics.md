---
id: TASK-394
title: Give actionable diagnostics for migration replay failures
status: To Do
assignee: []
created_date: '2026-11-26 11:31'
labels:
  - bug
  - annotation-processor
  - migrations
dependencies: []
references:
  - TASK-310
  - TASK-395
  - TASK-390
  - backlog/docs/database-migrations.md
priority: medium
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
During native IntelliJ verification in TASK-310, Prefab reported that it could not interpret a generated
`V2__generated.sql` migration and suggested rewriting its SQL to use supported statements or moving hand-written
SQL out of the `V<n>__*.sql` naming convention. The statement was a supported `ALTER TABLE ... ADD CONSTRAINT`:
replay failed because `customer_order_history_entry` had not been created in the migrations available to the
processor. In that run, the generated V1 migration was missing from the output. The suggestion misdiagnoses a
missing prerequisite as unsupported syntax and could encourage removing a migration from Flyway's discovery.

Distinguish a supported statement that is invalid against the replayed migration history from SQL that cannot be
parsed or modeled. Give guidance appropriate to the actual failure, without claiming to know why an earlier
migration or table is missing. TASK-310 tracks the separate incremental-build problem that led to this particular
missing generated migration; TASK-395 follows up the remaining Avro-only JPS scenario; TASK-390 tracks broader
validation of migration transitions.
<!-- SECTION:DESCRIPTION:END -->

## Analysis

<!-- SECTION:ANALYSIS:BEGIN -->
`SqlStatementParser.applyAlter` reports `Found ALTER TABLE on table not previously created` when the table is not
in the replayed state. `DbMigrationWriter.parseSql` currently wraps every `MigrationSqlException` with the same
unsupported-SQL explanation and suggested fix. Classify failures using their actual cause rather than matching
arbitrary exception message text.
<!-- SECTION:ANALYSIS:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 For a supported `ALTER TABLE` whose target is absent from replayed history, the compiler diagnostic identifies the migration, statement, table and missing prerequisite, and suggests checking earlier migrations and their order without asserting an unproven cause.
- [ ] #2 Supported statements that fail other replay-state checks receive a relevant explanation rather than a blanket unsupported-SQL suggestion; genuinely unparseable or unsupported SQL retains distinct, actionable guidance.
- [ ] #3 Diagnostics preserve the original cause and do not recommend renaming or moving a migration outside Flyway's versioned naming convention as a generic remedy.
- [ ] #4 Automated processor tests cover the reported missing-table case, a different invalid replay transition, and a genuinely unsupported or unparseable statement, alongside a valid ordered migration history.
- [ ] #5 Update the database migration troubleshooting guidance to reflect the resulting diagnostic categories and safe remedies.
<!-- AC:END -->
