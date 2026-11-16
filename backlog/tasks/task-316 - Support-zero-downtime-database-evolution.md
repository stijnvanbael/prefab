---
id: TASK-316
title: Support safe expand-contract database evolution for rolling deployments
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [persistence, feature]
dependencies: []
priority: high
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Make PostgreSQL schema evolution compatible with old and new Prefab application versions running concurrently. Define an opt-in expand-contract workflow rather than claiming arbitrary DDL can be zero downtime or silently rewriting existing migrations.

Related: TASK-315 (migration lifecycle), TASK-317 (backfills) and TASK-318 (advanced indexes). MongoDB evolution remains separately documented/out of scope for the first implementation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Define phases and compatibility rules for additive columns, rename/type replacements, nullability changes and eventual drops, including explicit operator approval of contract steps.
- [ ] #2 Destructive/incompatible changes in safe mode produce actionable diagnostics or an explicit reviewed plan instead of immediate destructive SQL.
- [ ] #3 Document lock/statement budgets, transactional versus nontransactional DDL, concurrent index creation and application read/write compatibility requirements.
- [ ] #4 An executable rolling-upgrade scenario proves old/new instances remain usable during expansion and backfill; contraction runs only after the old version is retired.
- [ ] #5 Explain recovery/rollback boundaries and unsupported transformations; existing behavior remains unchanged unless safe mode is enabled.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Classify current schema diffs by compatibility, specify safe-mode contracts, implement a thin additive/replacement workflow and validate with two application versions against real PostgreSQL.
<!-- SECTION:PLAN:END -->
