---
id: TASK-386
title: Share test containers between contexts when Testcontainers reuse is disabled
status: To Do
assignee: []
created_date: '2026-11-25 09:00'
labels:
  - test
  - bug
dependencies: []
references:
  - TASK-376
  - TASK-356
priority: medium
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while checking the other container setups for TASK-376. Postgres, Kafka, Schema Registry, LocalStack and MongoDB
share one container per name and rely on Testcontainers reuse (`withReuse(true)`) to give a second Spring context in the
same JVM the container of the first. Reuse only works when `TESTCONTAINERS_REUSE_ENABLE=true` (or
`testcontainers.reuse.enable=true` in `~/.testcontainers.properties`). Without it `withReuse(true)` is ignored, the second
context tries to create a container with the already used name, and `removeStaleContainer` deliberately keeps the running
one, so Docker answers 409 (name conflict). The benchmark disables reuse (TASK-343, TASK-356), and so does any developer
who has not enabled it.

The failure is loud, not a hang, but it makes two cached containers contexts in one module impossible without reuse.
This was reasoned from the Testcontainers start path and could not be run where it was found (no Docker daemon).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Reproduce with two cached containers contexts and `TESTCONTAINERS_REUSE_ENABLE=false`, covered by a test
- [ ] #2 Two cached containers contexts in one JVM start without a 409 and share the container, with and without reuse
- [ ] #3 The fix never removes a container that another cached context still uses
<!-- AC:END -->
