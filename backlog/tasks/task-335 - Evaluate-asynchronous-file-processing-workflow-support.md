---
id: TASK-335
title: Evaluate asynchronous file-processing workflow support
status: To Do
assignee: []
created_date: '2026-11-18 10:39'
labels:
  - spike
  - files
  - architecture
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Assess a document conversion workflow that accepts a file, runs background processing and provides status and a downloadable result. Compare an aggregate plus existing event handlers, binary fields and download support with the planned job progress API in TASK-157; coordinate resumability with TASK-330 and exports with TASK-107. TASK-309 and decision-006 identify a possible worker lifecycle gap, not proof that a new architecture is needed.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Demonstrate the current aggregate, event and binary-file approach for a bounded conversion workflow and identify exactly which lifecycle or integration steps remain handwritten.
- [ ] #2 Specify submission, status transitions, failure, retry or cancellation, artifact storage and retention, tenant/row authorization and size limits; distinguish job status from worker execution.
- [ ] #3 Exercise successful conversion and download, failed processing, duplicate delivery and unauthorized access with tests or a reproducible prototype; state persistence and transaction limits explicitly.
- [ ] #4 Record a go/no-go recommendation for any shared worker or generated support, its relation to TASK-157, TASK-330 and TASK-107, and bounded implementation slices only if justified.
<!-- AC:END -->
