---
id: TASK-372
title: >-
  Validate versions of read-only and absent-deleted keys in the Postgres stream
  state repository
status: In Progress
assignee: []
created_date: '2026-11-24T06:16:00Z'
updated_date: '2026-11-24T15:08:26Z'
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

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Operator analysis: the engine uses observed state to make decisions, not only to supply values for writes. `deduplicate` suppresses a message on a read-only branch when its identity is present; windowed aggregation reads stream-time state before expiry/acceptance decisions; joins can drop an old message after reading join state; and user processors can read `Store`/`GlobalStore` without writing. Aggregate and join/window success paths also read then update state. Therefore every observed version must be validated at commit, including absent reads and absent-key deletes. Implementation plan: validate all observed versions within each backend's commit transaction; serialize Postgres commits by deterministic transaction-scoped advisory locks for observed state keys (including absent keys), then compare versions while row-locking existing rows. Extend the shared repository contract with stale read-only and absent-delete scenarios, and update each repository implementation to preserve the contract.
<!-- SECTION:NOTES:END -->
