---
id: TASK-317
title: Support resumable code-driven backfills of new database fields
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [persistence, feature]
dependencies: []
priority: high
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Provide a supported way to backfill newly introduced PostgreSQL fields using Java business transformations too complex for simple SQL defaults. Separate long-running backfills from startup schema migration and normal application request processing.

Related: TASK-157 (long-running jobs), TASK-316 (expand-contract) and TASK-315 (migration docs). Reuse existing execution/checkpoint contracts where suitable; do not invent an unrelated scheduler.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Specify an explicit operator-triggered backfill contract with stable identity, bounded batches, checkpoints, progress and completion/failure reporting.
- [ ] #2 Backfills are restartable/idempotent and define concurrency, transaction boundaries, retries and protection against overwriting newer live writes.
- [ ] #3 Support a representative multi-field or cross-record Java transformation with tenant isolation and parameterized database access.
- [ ] #4 Tests cover interruption/resume, duplicate execution, poison records, concurrent writers and deployment compatibility; no silent skip or false completion occurs.
- [ ] #5 Document operational controls, dry-run/validation expectations, completion criteria for constraint tightening and backend-specific limits.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Assess reusable job/Flyway extension points, define checkpoint and concurrency semantics, implement one explicit backfill example and prove safe restart before generalizing.
<!-- SECTION:PLAN:END -->
