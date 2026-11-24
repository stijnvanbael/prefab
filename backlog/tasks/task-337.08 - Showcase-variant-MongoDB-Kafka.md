---
id: TASK-337.08
title: 'Showcase variant: MongoDB + Kafka'
status: In Progress
assignee: []
created_date: '2026-11-20 19:47'
labels:
  - examples
  - mongodb
dependencies:
  - TASK-337.05
  - TASK-337.06
  - TASK-337.07
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 179000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add `examples/showcase-mongodb`: pom (prefab-mongodb instead of prefab-postgres), application.yml, test config and `showcase-capabilities.yml`. Replaces `examples/mongodb`. Includes the streams and Avro slices (Kafka).

Run the full showcase suite. Every failing feature is either fixed in Prefab (if small) or declared as an unsupported capability with a new backlog task. Pick up TASK-153 (flaky CategoryStatsIntegrationTest) if the equivalent test is flaky here.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Module contains no Java sources
- [ ] #2 Full showcase suite passes or skips with declared gaps, each linked to a backlog task
- [ ] #3 Coverage matrix updated
<!-- AC:END -->

## Analysis

`examples/showcase-mongodb` (pom, `application.yml`, `application-test.yml`, `showcase-capabilities.yml`, the streams, Kafka
Streams breakout and Avro slices, the `.run` configuration and the coverage-matrix column) was created incrementally by
TASK-337.01 to TASK-337.07. What remained was the full run of the shared suite on MongoDB + Kafka.

This environment has no Docker daemon, so the Testcontainers suite can only run in CI. A local run with
`-Dprefab.test.containers=false` is the only way to execute anything here, and it exposed two in-memory problems
(not showcase gaps): the NPE in `InMemoryAutoConfigurationFilter` (fixed here, with a test) and the generated
`MongoIndexConfiguration` requiring a `MongoTemplate` that in-memory mode excludes (TASK-372).
