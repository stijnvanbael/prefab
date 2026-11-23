---
id: TASK-309
title: Spike an alternative target architecture alongside DDD
status: Done
assignee: []
created_date: '2026-11-16 15:43'
updated_date: '2026-11-18 10:40'
labels:
  - spike
  - architecture
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Investigate whether Prefab should support an additional generated application architecture for use cases poorly served by aggregate-centric DDD. Compare alternatives such as transaction scripts, vertical slices and resource-centric applications without replacing the existing DDD default.

Related: TASK-127 (pluggable exposure strategies) and TASK-180 (provider SPIs). This is an evidence-producing spike, not a commitment to new annotations or a second framework.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Compare at least three approaches against concrete use cases, ownership/transaction boundaries, generated artifacts, testability, customization and migration cost.
- [x] #2 Prototype the strongest candidate on one representative workflow outside production modules and compare its user-authored code and generated output with the DDD version.
- [x] #3 Identify reusable processor/runtime contracts and isolation required to prevent architecture-specific behavior leaking into DDD projects.
- [x] #4 Produce an ADR with an adoption recommendation, limitations, opt-in compatibility approach and estimated implementation slices; retain DDD behavior as the default.
- [x] #5 Screen at least ten materially different use cases against the existing Prefab extension points and identify which architecture families warrant deeper prototypes.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Select a non-DDD-friendly use case, evaluate candidates, conduct a bounded prototype and record the decision. Create implementation stories only if the evidence supports adoption.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Deferred follow-ups created after the fourteen-case screen: TASK-329 aggregate-free operations, TASK-330 resumable bulk jobs, TASK-331 faceted search, TASK-332 incoming webhooks, TASK-333 durable orchestration, TASK-334 legacy schemas and TASK-335 asynchronous file processing. Existing TASK-319/326/327 cover projections/views; TASK-105/107/157/317 cover related bulk, export, job status and backfill capabilities. These are low-priority assessments, not approved production implementations.
<!-- SECTION:NOTES:END -->

## Progress

Implementation notes:

- Scope expanded after review: the single batch-import case is not enough evidence for the ADR's no-go.
  The prior recommendation is provisional pending a broad use-case screen; it must not be treated as a final
  decision. Screen at least ten distinct workflows against actual Prefab capabilities, distinguishing cases
  already served by aggregates, extensions, event handlers or streams from genuine gaps.
- Compared aggregate-centric DDD, transaction scripts, vertical slices and resource-centric generation for a
  batch product import, including ownership, transaction boundary, generated/manual artifacts, testing,
  customization, and migration impact.
- Added an isolated, non-reactor prototype at `backlog/prototypes/task-309/`. It generates a feature-owned Spring
  endpoint for an authored workflow; compiler snapshot and workflow behavior tests pass. No production module or
  public API was changed.
- Recorded the comparison, DDD generated-output reference, reusable processor contracts, isolation, limitations,
  and conditional implementation slices in
  [decision-006](../decisions/decision-006%20-%20Alternative-application-architecture-spike.md).
- Expanded the ADR screen to fourteen use cases, with current Prefab coverage, architectural fit, and a
  prioritization for distinct follow-up proof points. This invalidates the earlier broad no-go: adoption is now
  deferred pending comparative evidence for operation-only services, projections, resumable jobs and durable
  external workflows. DDD remains the default; no production API or module change is recommended by this screen.
- The screen is complete; comparative prototypes are possible follow-up work rather than a claim that the
  architecture question is fully settled.
