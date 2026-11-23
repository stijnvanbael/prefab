---
id: TASK-175.05
title: Make in-memory the default and prove it on the showcase
status: To Do
assignee: []
created_date: '2026-11-23 11:00'
labels:
  - test
dependencies:
  - TASK-175.02
  - TASK-175.03
  - TASK-175.04
parent_task_id: TASK-175
priority: high
type: task
ordinal: 156500
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Flip `prefab.test.containers` to default `false`, migrate this repository and document the in-memory backend (TASK-175 rollout, decision-011).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `@IntegrationTest` runs in memory by default
- [ ] #2 Tests that call native `@Query` mixins (`examples/kafka`, `pubsub`, `sns-sqs`) use `containers = true`; the reference showcase and existing variants set `prefab.test.containers: true`
- [ ] #3 A configuration-only `examples/showcase-inmemory` variant passes with Docker unavailable; gaps (at least Prefab Streams) are declared in its capabilities file with a reason and task
- [ ] #4 Wall-clock time of the in-memory variant versus the reference showcase is recorded in TASK-175
- [ ] #5 `modules.md`, `feature-guides.md`, `configuration.md`, `annotation-reference.md`, `troubleshooting.md` and `examples/README.md` describe the in-memory backend, its semantics, gaps and the upgrade behaviour change
- [ ] #6 A follow-up task exists for Prefab Streams on the in-memory bus
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
