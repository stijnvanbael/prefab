---
id: TASK-342
title: Prefab Streams backend for AWS SNS/SQS
status: To Do
assignee: []
created_date: '2026-11-20 19:48'
labels:
  - streams
  - sns-sqs
  - coverage-gap
dependencies:
  - TASK-339
  - TASK-340
priority: medium
type: feature
ordinal: 187000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement a `prefab-streams-sns-sqs` backend so the backend-neutral Prefab Streams DSL runs on AWS SNS/SQS, following the design chosen in TASK-339. Today `StreamBackend` only knows KAFKA, so the showcase-sns-sqs variant (TASK-337.10) declares STREAMS as a gap pointing at this task.

Keyed operators (`groupBy`, `aggregate`, `join`) need per-key ordering, which SNS/SQS only provides through FIFO message groups (TASK-340). Scope of a first version is the operator matrix from TASK-339. The backend-neutral topology test API (TASK-300) must work against it, so the showcase streams slice (TASK-337.06) can be included in showcase-sns-sqs without code changes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Showcase streams slice compiles and its tests pass in showcase-sns-sqs, or unsupported operators are skipped with a declared gap
- [ ] #2 Topology tests run against the SNS/SQS backend through the backend-neutral test API
- [ ] #3 showcase-sns-sqs no longer declares STREAMS as a gap (or declares only specific unsupported operators)
<!-- AC:END -->
