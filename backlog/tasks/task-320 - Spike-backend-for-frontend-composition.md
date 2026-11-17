---
id: TASK-320
title: Spike backend-for-frontend composition with Prefab
status: In Progress
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

## Analysis
Prefab already generates REST controllers and per-aggregate services, and supports handwritten Spring components, repository mixins and `PrefabPlugin` hooks. Generated `getById`/`getList` operations include tenant filtering only when an aggregate declares `@TenantId`; the default `TenantContextProvider` returns `null`, so applications requiring isolation must provide a real tenant context. The security module provides JWT resource-server authentication and method security.

The initial prototype direction is to keep screen DTOs and orchestration in handwritten application adapters, exercising the existing generated services and repository query hooks. The Kafka example's `ChannelSummary` and `UserStatus` are candidate read-side models. Transaction behavior across generated service calls, event publication, and the PostgreSQL persistence adapter still needs verification before the command prototype can make a boundary guarantee.

Generated aggregate services are Spring `@Transactional` beans; the PostgreSQL example's query and projection model can support an in-process dashboard adapter. Aggregate constructors may publish events immediately, before persistence, and the configured dispatcher sends to external infrastructure directly. Therefore the command prototype will compose multiple generated steps on one aggregate without external event effects (product creation plus tags); it will not claim atomicity across aggregate/event-broker boundaries. The dashboard prototype will page generated channel summaries and batch-read one user's status rows, keeping query count constant rather than loading per-channel details.

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
