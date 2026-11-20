---
id: TASK-341
title: Prefab Streams backend for Google Pub/Sub
status: To Do
assignee: []
created_date: '2026-11-20 19:48'
labels:
  - streams
  - pubsub
  - coverage-gap
dependencies:
  - TASK-339
priority: medium
type: feature
ordinal: 186000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement a `prefab-streams-pubsub` backend so the backend-neutral Prefab Streams DSL runs on Google Pub/Sub, following the design chosen in TASK-339. Today `StreamBackend` only knows KAFKA, so the showcase-pubsub variant (TASK-337.09) declares STREAMS as a gap pointing at this task.

Scope of a first version is the operator matrix from TASK-339. The backend-neutral topology test API (TestStreams / TopologyTestHarness, TASK-300) must work against it, so the showcase streams slice (TASK-337.06) can be included in showcase-pubsub without code changes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Showcase streams slice compiles and its tests pass in showcase-pubsub, or unsupported operators are skipped with a declared gap
- [ ] #2 Topology tests run against the Pub/Sub backend through the backend-neutral test API
- [ ] #3 showcase-pubsub no longer declares STREAMS as a gap (or declares only specific unsupported operators)
<!-- AC:END -->
