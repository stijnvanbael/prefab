---
id: TASK-330
title: Evaluate resumable bulk import and export jobs
status: To Do
assignee: []
created_date: '2026-11-18 10:38'
labels:
  - spike
  - batch
  - architecture
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Assess a bounded, restartable catalogue import and compare it with per-item aggregate operations, synchronous bulk endpoints (TASK-105), job progress APIs (TASK-157), data export (TASK-107) and resumable migration backfills (TASK-317). TASK-309 and decision-006 identify batch processing as a potential gap; do not assume a new job engine is required. Include reconciliation and authorized export as related applicability checks, not separate implementations.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Prototype a representative import outside production modules and compare its authored code, generated output, persistence calls and failure behavior with a DDD baseline and the existing TASK-309 route-only prototype.
- [ ] #2 Specify bounded input and chunk size, checkpoint persistence, local transaction boundaries, row rejection and all-or-nothing versus partial-commit semantics; distinguish restart and concurrent execution.
- [ ] #3 Demonstrate interruption and resume, duplicate delivery, invalid rows and a persistence failure with tests; identify any remaining concurrency or backend limitations explicitly.
- [ ] #4 Evaluate tenant and row authorization, resource limits, status and result retention for import and export, and explain which parts can reuse TASK-105, TASK-107, TASK-157 and TASK-317.
- [ ] #5 Record a go/no-go recommendation and bounded implementation slices only if justified; do not promise exactly-once processing or cross-system atomicity.
<!-- AC:END -->
