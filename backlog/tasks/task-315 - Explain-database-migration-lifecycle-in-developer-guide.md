---
id: TASK-315
title: Document the database migration lifecycle and customization
status: To Do
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
- [ ] #1 Explain baseline reconstruction, model diffing, naming/versioning/output locations, compile-time generation versus startup execution, and migration history/checksum behavior from current code.
- [ ] #2 Include reproducible examples for add/rename/remove/type/default/nullability/index changes, nested values, foreign keys and manual data migrations.
- [ ] #3 Document ownership and regeneration rules, deployment/recovery limitations, review of destructive SQL and why already-applied migrations must not be edited.
- [ ] #4 Explain test-only reset/checksum behavior separately from production migration recovery; never recommend production schema deletion.
- [ ] #5 Link the section from the guide index, generated artifacts and troubleshooting, and verify snippets against current behavior.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Trace generation/replay and runtime migration entry points, run a small schema-evolution example, then write lifecycle guidance with explicit backend limitations.
<!-- SECTION:PLAN:END -->
