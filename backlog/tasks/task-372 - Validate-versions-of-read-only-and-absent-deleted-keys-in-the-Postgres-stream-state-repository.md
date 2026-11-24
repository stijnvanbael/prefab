---
id: TASK-372
title: >-
  Validate versions of read-only and absent-deleted keys in the Postgres stream
  state repository
status: To Do
assignee: []
created_date: '2026-11-24 06:16'
labels:
  - streams
  - postgres
dependencies: []
priority: medium
type: bug
ordinal: 202000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while fixing TASK-371. `PostgresStreamStateRepository` only checks versions of keys it writes. A key that a message only read is not validated at commit, so a decision based on a value changed concurrently can commit (write skew). A delete of a key observed as absent (expected version 0) succeeds without a check, even if another transaction inserted it meanwhile. Decide whether the engine's operators rely on these guarantees and, if so, validate them (e.g. `SELECT ... FOR SHARE` / version comparison) in the commit.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Analysis records which operators depend on read-only or absent-delete consistency
- [ ] #2 If needed, the commit raises OptimisticConflictException when such a key changed concurrently, with a test reproducing it
- [ ] #3 Contract test covers the behaviour so other repositories match
<!-- AC:END -->
