---
id: TASK-320
title: Spike backend-for-frontend composition with Prefab
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [spike, architecture]
dependencies: []
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Investigate how a Prefab application can serve frontend-specific contracts and composed operations without forcing screen/view models into aggregate write models. Prefer supported controller/service/query extension points over a speculative BFF module.

Related: TASK-319 (read projections), TASK-127 (exposure separation) and TASK-324 (extension-point audit). No frontend implementation or new annotation is promised by this spike.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Prototype one screen-oriented composed read and one multi-step command using existing Prefab-generated services and handwritten adapters.
- [ ] #2 Specify authentication propagation, tenant isolation, timeouts, partial failures, transaction/compensation boundaries and avoidance of N+1 calls.
- [ ] #3 Compare an in-process adapter and separately deployed BFF, including coupling, deployment and contract evolution costs.
- [ ] #4 Record an ADR recommending existing patterns or a justified minimal extension, with runnable examples and follow-up stories only for demonstrated gaps.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Select a concrete frontend workflow, prototype composition through current extension points, measure complexity and document the recommended boundary.
<!-- SECTION:PLAN:END -->
