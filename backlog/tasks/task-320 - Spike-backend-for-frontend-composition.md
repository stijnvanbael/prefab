---
id: TASK-320
title: Spike backend-for-frontend composition with Prefab
status: Done
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
<!-- SECTION:ANALYSIS:BEGIN -->
Prefab already generates REST controllers and per-aggregate services, and supports handwritten Spring components, repository mixins and `PrefabPlugin` hooks. Generated `getById`/`getList` operations include tenant filtering only when an aggregate declares `@TenantId`; the default `TenantContextProvider` returns `null`, so applications requiring isolation must provide a real tenant context. The security module provides JWT resource-server authentication and method security.

Generated aggregate services are Spring `@Transactional` beans. Aggregate constructors may publish events immediately, before persistence, and the configured dispatcher sends to external infrastructure directly, so a database transaction cannot guarantee atomic broker publication. The selected prototypes keep screen DTOs and orchestration handwritten: the dashboard pages `ChannelSummary` and batch-reads a user's `UserStatus`; the command composes Product creation and tag updates, which have no external event effects and share one local transaction.
<!-- SECTION:ANALYSIS:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Prototype one screen-oriented composed read and one multi-step command using existing Prefab-generated services and handwritten adapters.
- [x] #2 Specify authentication propagation, tenant isolation, timeouts, partial failures, transaction/compensation boundaries and avoidance of N+1 calls.
- [x] #3 Compare an in-process adapter and separately deployed BFF, including coupling, deployment and contract evolution costs.
- [x] #4 Record an ADR recommending existing patterns or a justified minimal extension, with runnable examples and follow-up stories only for demonstrated gaps.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Select a concrete frontend workflow, prototype composition through current extension points, measure complexity and document the recommended boundary.
<!-- SECTION:PLAN:END -->

## Revision (review feedback)

The first conclusion ("handwritten adapters, no follow-ups") did not assess an aggregate-bound frontend model
or other boilerplate reductions, which is at odds with Prefab being a declarative framework. decision-005 was
revised with an options table; handwritten adapters are now the escape hatch. Follow-ups created: TASK-326
(`@ViewOf`), TASK-327 (reference expansion), TASK-328 (composed-command design). Related: TASK-170, TASK-319.

## Implementation Notes

- Added `ChannelDashboardService` and `ChannelDashboardItem` in `examples/kafka`: the generated `ChannelSummaryService` provides a stable paged summary; a handwritten repository mixin provides one user-scoped status lookup. The adapter returns an explicit status-availability flag for eventually consistent read models and makes no per-channel repository calls.
- Added `ProductCommandService` and `CreateTaggedProductCommand`: generated create and tag operations compose under one local transaction for the non-event-publishing Product aggregate.
- Added focused unit tests for the composed read, page sorting/metadata, projection availability, command ordering and failure handling.
- Recorded the recommendation and the auth/tenant, timeout, partial-failure, transaction/compensation, query-growth and deployment trade-offs in [decision-005](../decisions/decision-005%20-%20Backend-for-frontend-composition.md); linked the pattern from the extension-point guide. No new BFF module, annotation, or follow-up story was justified by the prototypes.
- Targeted validation passed: `mvn -pl :kafka-example -am "-Dtest=ChannelDashboardServiceTest,ProductCommandServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` (5 tests).
- The full dependent test suite was attempted; its Kafka example integration contexts could not acquire the Testcontainers `confluentinc/cp-schema-registry:8.4.0` image. Those integration tests did not reach their assertions; the focused prototype tests and reactor compilation passed.
