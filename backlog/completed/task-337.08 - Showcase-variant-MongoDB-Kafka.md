---
id: TASK-337.08
title: 'Showcase variant: MongoDB + Kafka'
status: Done
assignee: []
created_date: '2026-11-20 19:47'
updated_date: '2026-11-24 06:30'
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
- [x] #1 Module contains no Java sources
- [x] #2 Full showcase suite passes or skips with declared gaps, each linked to a backlog task
- [x] #3 Coverage matrix updated
<!-- AC:END -->

## Analysis

`examples/showcase-mongodb` (pom, `application.yml`, `application-test.yml`, `showcase-capabilities.yml`, the streams, Kafka
Streams breakout and Avro slices, the `.run` configuration and the coverage-matrix column) was created incrementally by
TASK-337.01 to TASK-337.07. What remained was the full run of the shared suite on MongoDB + Kafka.

This environment has no Docker daemon, so the Testcontainers suite can only run in CI. A local run with
`-Dprefab.test.containers=false` is the only way to execute anything here, and it exposed two in-memory problems
(not showcase gaps): the NPE in `InMemoryAutoConfigurationFilter` (fixed here, with a test) and the generated
`MongoIndexConfiguration` requiring a `MongoTemplate` that in-memory mode excludes (TASK-372).

## Implementation Notes

The module itself needed no change: it was complete after TASK-337.07. CI (Docker, Testcontainers) ran the full shared suite
on MongoDB + Kafka: 95 tests, 0 failures, 0 skipped, including the streams, Kafka Streams breakout and Avro slices. So the
variant declares no unsupported capabilities (`showcase-capabilities.yml` stays empty) and the coverage matrix in
`examples/README.md` already shows every capability as supported for `showcase-mongodb`, which `CoverageMatrixTest` verifies.

Found on the way, because this environment has no Docker and the suite was run in memory (`-Dprefab.test.containers=false`):
`InMemoryAutoConfigurationFilter` threw an NPE on the `null` entries Spring Boot leaves for candidates that an earlier filter
dropped (fixed, with a test). The generated `MongoIndexConfiguration` then fails in memory mode because it needs a
`MongoTemplate`; tracked as TASK-372 and a prerequisite for making in-memory the default for MongoDB (TASK-175.05).
No flaky equivalent of TASK-153 showed up in the showcase run (a single CI run, so no proof); that task stays open.
