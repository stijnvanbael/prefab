---
id: TASK-356
title: Isolate reused test containers by container name
status: To Do
assignee: []
created_date: '2026-11-22 19:45'
labels:
  - test
  - bug
  - agents
dependencies: []
priority: medium
type: bug
ordinal: 201000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`prefab-test` lets applications choose container names (`prefab.test.postgres.container-name`, `...kafka...`, etc.)
and enables Testcontainers reuse. Reuse, however, finds an existing container by a hash of its configuration, and the
name is applied with `withCreateContainerCmdModifier`, which is not part of that hash. Two builds that configure
different container names therefore still reuse the same running container ("Reusing container with ID ..."). When one
of them removes "its" container (by name, via `removeStaleContainer`, or at the end of a run), the other loses its
database or broker mid-test.

Found by the TASK-343 benchmark, which runs two attempts in parallel with per-slot container names: hidden tests failed
with "Connection refused" / "terminating connection due to unexpected postmaster exit". The benchmark now disables
reuse as a workaround. The same happens to developers running builds of two projects, or two worktrees, in parallel.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Containers with different configured names are never reused for each other (for example by adding the name as a label or another hash-relevant setting)
- [ ] #2 Covered by a test with two configurations using different names
- [ ] #3 The benchmark can enable reuse again without cross-slot interference
<!-- AC:END -->
