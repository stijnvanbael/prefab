---
id: TASK-309
title: Spike an alternative target architecture alongside DDD
status: Done
assignee: []
created_date: '2026-11-16 15:43'
labels: [spike, architecture]
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
- [x] #4 Produce an ADR with a go/no-go recommendation, limitations, opt-in compatibility approach and estimated implementation slices; retain DDD behavior as the default.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Select a non-DDD-friendly use case, evaluate candidates, conduct a bounded prototype and record the decision. Create implementation stories only if the evidence supports adoption.
<!-- SECTION:PLAN:END -->

## Progress

Implementation notes:

- Compared aggregate-centric DDD, transaction scripts, vertical slices and resource-centric generation for a
  batch product import, including ownership, transaction boundary, generated/manual artifacts, testing,
  customization, and migration impact.
- Added an isolated, non-reactor prototype at `backlog/prototypes/task-309/`. It generates a feature-owned Spring
  endpoint for an authored workflow; compiler snapshot and workflow behavior tests pass. No production module or
  public API was changed.
- Recorded the comparison, DDD generated-output reference, reusable processor contracts, isolation, limitations,
  and conditional implementation slices in
  [decision-006](../decisions/decision-006%20-%20Alternative-application-architecture-spike.md).
- Recommendation: no-go for a second built-in architecture now. Retain DDD as the only built-in default and use
  existing handwritten application adapters/repository mixins for exceptional workflows. Reconsider after
  TASK-127 only if a concrete use case shows additional generation value.
