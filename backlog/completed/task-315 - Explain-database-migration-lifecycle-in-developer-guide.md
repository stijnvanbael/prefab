---
id: TASK-315
title: Document the database migration lifecycle and customization
status: Done
assignee: []
created_date: '2026-11-16 15:43'
labels: [documentation, persistence]
dependencies: []
priority: high
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Provide a dedicated guide section explaining how Prefab derives and evolves database migrations, how generated and manual migrations coexist, and how developers safely review and deploy them. Existing generated-artifact examples and troubleshooting fragments do not explain the full lifecycle.

Related: TASK-148 (guide), TASK-075 (foreign-key metadata), completed TASK-267 (manual migration replay) and TASK-293 (rename/remove SQL). Describe PostgreSQL/Flyway and MongoDB behavior separately and accurately.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Explain baseline reconstruction, model diffing, naming/versioning/output locations, compile-time generation versus startup execution, and migration history/checksum behavior from current code.
- [x] #2 Include reproducible examples for add/rename/remove/type/default/nullability/index changes, nested values, foreign keys and manual data migrations.
- [x] #3 Document ownership and regeneration rules, deployment/recovery limitations, review of destructive SQL and why already-applied migrations must not be edited.
- [x] #4 Explain test-only reset/checksum behavior separately from production migration recovery; never recommend production schema deletion.
- [x] #5 Link the section from the guide index, generated artifacts and troubleshooting, and verify snippets against current behavior.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Trace generation/replay and runtime migration entry points, run a small schema-evolution example, then write lifecycle guidance with explicit backend limitations.
<!-- SECTION:PLAN:END -->

## Analysis
<!-- SECTION:ANALYSIS:BEGIN -->
The current guide had only a brief generated-migration warning and no lifecycle walkthrough. Source tracing confirmed PostgreSQL replays all versioned SQL files it finds at compile time (not database/Flyway history), emits a versioned generated delta, and relies on Flyway at startup; its schema parser has limits, and default-only changes are not emitted. MongoDB generation is limited to recognizable collection/field renames and has no Prefab runtime executor or applied-history tracking. The imported test Flyway strategy can clean/retry only when the explicit test property is enabled and only checks the last applied migration.
<!-- SECTION:ANALYSIS:END -->

## Implementation Notes

- Added `backlog/docs/database-migrations.md` as the canonical lifecycle guide, including PostgreSQL schema replay/diff behavior, migration examples, Flyway startup/checksum behavior, MongoDB rename-only script generation, and test-only reset boundaries.
- Corrected the generated SQL location/name in `generated-artefacts.md`; linked the canonical guide from the developer-guide index, generated-artefacts page, and troubleshooting.
- Added a direct link to the lifecycle guide from the README persistence capability entry.
- Verified migration behavior against `DbMigrationWriter`, `MongoMigrationWriter`, the test Flyway strategy, and migration processor tests.
- Validation: `mvn --% -pl annotation-processor -am -Dtest=DbMigrationWriterTest,MongoMigrationWriterTest -Dsurefire.failIfNoSpecifiedTests=false test` (78 tests passed).
