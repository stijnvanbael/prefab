---
id: TASK-372
title: >-
  Validate versions of read-only and absent-deleted keys in the Postgres stream
  state repository
status: Done
assignee: []
created_date: '2026-11-24T06:16:00Z'
updated_date: '2026-11-25T05:24:27Z'
labels:
  - streams
  - postgres
dependencies: []
modified_files:
  - backlog/docs/feature-guides.md
  - >-
    streams-engine/src/main/java/be/appify/prefab/streams/engine/InMemoryStreamStateRepository.java
  - >-
    streams-engine/src/main/java/be/appify/prefab/streams/engine/StreamStateRepository.java
  - >-
    streams-engine/src/test/java/be/appify/prefab/streams/engine/StreamStateRepositoryContractTest.java
  - >-
    streams-mongodb/src/main/java/be/appify/prefab/streams/mongodb/MongoStreamStateRepository.java
  - >-
    streams-postgres/src/main/java/be/appify/prefab/streams/postgres/PostgresStreamStateRepository.java
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
- [x] #1 Analysis records which operators depend on read-only or absent-delete consistency
- [x] #2 If needed, the commit raises OptimisticConflictException when such a key changed concurrently, with a test reproducing it
- [x] #3 Contract test covers the behaviour so other repositories match
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Analysis confirmed that state observations affect stream decisions: deduplicate filters a previously-seen identity on a read-only path; windowed aggregation reads per-key stream time before expiry and acceptance decisions; joins read state before lateness/match decisions; and custom processors can use read-only Store/GlobalStore reads. Aggregate, window and join success paths also read then update state. All observed versions are now validated at commit. Postgres takes sorted transaction-scoped advisory locks for every observed key (including missing keys), then compares versions while locking existing rows. MongoDB guards extant reads with an internal transactional write and guards missing keys with a transient insert/delete; the in-memory backend validates its full observed-version map. The SPI and feature-guide docs now describe validation of read-only observations and absent keys.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added commit-time validation for every state key observed during a message, so read-only decisions and deletes of absent keys conflict when state changed concurrently. Added shared contract scenarios for stale read-only values and absent-key deletes, updated in-memory/Postgres/MongoDB implementations, and documented the semantics. Targeted engine/Postgres/MongoDB tests pass and IntelliJ inspections report no issues. The full Maven reactor still hits the known TASK-385 showcase Pub/Sub history timeouts; rerunning that integration-test class alone passes.
<!-- SECTION:FINAL_SUMMARY:END -->
