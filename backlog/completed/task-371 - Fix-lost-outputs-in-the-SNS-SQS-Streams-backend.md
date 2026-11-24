---
id: TASK-371
title: Fix lost outputs in the SNS/SQS Streams backend
status: Done
assignee: []
created_date: '2026-11-24 05:40'
updated_date: '2026-11-24 06:17'
labels:
  - streams
  - sns-sqs
dependencies: []
modified_files:
  - >-
    streams-postgres/src/main/java/be/appify/prefab/streams/postgres/PostgresStreamStateRepository.java
  - >-
    streams-postgres/src/test/java/be/appify/prefab/streams/postgres/PostgresStreamStateRepositoryTest.java
  - >-
    streams-postgres/src/test/java/be/appify/prefab/streams/postgres/PostgresTestDatabase.java
  - >-
    streams-sns-sqs/src/test/java/be/appify/prefab/streams/snssqs/SnsSqsStreamsIntegrationTest.java
  - backlog/docs/feature-guides.md
priority: high
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`SnsSqsStreamsIntegrationTest.streamsRunOnFifoTopicsWithStateInPostgres` (module `streams-sns-sqs`, added with TASK-342
in #217) fails intermittently in CI with `Not seen within 90 seconds: all running totals`.

- **Where it failed:**
  - `main` at the #217 merge commit (run 45236689024).
  - Twice on #219 (runs 45275131905 and 45276910177), whose diff does not touch `streams-sns-sqs` or any module it
    depends on.
- **Where it passed:**
  - `main` at #221 (run 45276571629).
  - The re-run of 45276910177, on identical code.

Because the build stops at this module, a failure also hides the results of every later module, so it can block any
pull request.

This looks like a **correctness bug, not a slow test**. Both failure snapshots show the same pattern for sale `s-1`,
the first `books` sale:

- The inbox of the sales source holds 5 messages, so `s-1` was processed and acknowledged.
- The `books` aggregate includes `s-1`: the listener receives `books 15` (5 + 10) after `s-3`.
- But every other effect of `s-1` is missing:
  - its own aggregate output `books 5`;
  - its windowed output `books 5`;
  - its left entry in the join state.
- The join row for key `s-1` holds only the refund on the right side, so `RefundedSale(s-1)` is never emitted.
- The outbox is empty and all queues are drained, so the output was not stuck.

decision-008 promises that state, inbox and outbox commit in one transaction per input message. The observed state
(aggregate committed, outputs and join entry missing) breaks that promise. A production application would silently
lose stream output.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The root cause is identified and recorded in the implementation notes, with a test that reproduces it deterministically
- [x] #2 Every effect of an input message (state changes, join entries, outbox records) commits together or not at all, also when sources run concurrently or a conflict is retried
- [x] #3 `SnsSqsStreamsIntegrationTest` passes in 20 consecutive runs (e.g. a loop in CI or locally with Docker)
- [x] #4 If the cause is shared with the in-process engine or the Postgres state repository, the Pub/Sub backend is checked for the same problem
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Leads (not yet verified; needs Docker for LocalStack and Postgres)

1. **Concurrent sources on one join key.**
   - Sale `s-1` (sales source, FIFO group `books`) and refund `s-1` (refunds source, group `s-1`) run on different
     threads and both read-modify-write the join row `join-2`/`s-1`. See `InProcessPrefabStreams` around the join
     operator, `get` then `put`.
   - `PostgresStreamStateRepository` guards rows with a version (`INSERT … ON CONFLICT DO NOTHING`,
     `UPDATE … WHERE version = ?`) and raises `OptimisticConflictException`. `processMessage` then retries.
   - Check that the retry really re-runs every route, and that nothing outside the transaction keeps the effects of
     the failed attempt or skips the next one. Examples: in-memory operator caches, the window or stream-time
     bookkeeping, `activeTransaction`, outbox records already relayed by `relayOutbox()`.
2. **Retry exhaustion.** After `MAX_CONFLICT_ATTEMPTS`, the conflict propagates into `SqsUtil`'s retry template and
   dead-letter handling. Check what is acknowledged, deleted or committed on that path.
3. **Outbox relay and SNS FIFO deduplication.**
   - The outbox is empty in the snapshot. Check whether the `s-1` outputs were never written or were relayed and then
     dropped.
   - SNS FIFO deduplicates by `MessageDeduplicationId` for 5 minutes, so check how `SnsSqsStreamSink` derives that ID
     for engine output.

### Reproduction ideas

- Publish the sale and the refund for the same key at the same moment, in a loop.
- Or add a deterministic test in `streams-engine` with a `StreamStateRepository` that forces an
  `OptimisticConflictException` on the first commit of a message that feeds several routes (aggregate, windowed
  aggregate, join). Then assert that all outputs and state appear exactly once.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
### Root cause

`SnsSqsStreamsIntegrationTest.startInfrastructure` built the `JdbcTemplate` and the `DataSourceTransactionManager`
from two calls to `postgresDataSource()`, which returns a **new** `PGSimpleDataSource` each time. Spring binds the
transaction's connection to the manager's `DataSource` object, so the template never found it and every statement
autocommitted on its own connection.

`PostgresStreamStateRepository.commit()` writes inbox → state rows → outbox. Sale `s-1` and refund `s-1` run
concurrently and both update join row `s-1`. When the sale's join write lost the version race, the
`OptimisticConflictException` came *after* the inbox row and the aggregate row had already been committed. The
"rollback" undid nothing; the join entry and outbox rows were never written. The engine's retry then found the inbox
identity recorded and skipped the message. That is exactly the failure snapshot: aggregate includes `s-1`, its outputs
and its join entry are missing. Timing-dependent, hence intermittent.

The engine (`InProcessPrefabStreams`) is correct: all operator state goes through the transaction and a retry starts a
fresh transaction that re-runs every route.

### Fix

- `PostgresStreamStateRepository` now refuses a non-atomic setup:
  - constructor: a `DataSourceTransactionManager` whose `DataSource` (proxy-unwrapped) differs from the template's
    throws `IllegalArgumentException`;
  - commit: before writing, if the transaction holds no resource for the template's `DataSource` (other manager
    types), it throws `IllegalStateException`, so nothing is written.
- The integration test shares one `DataSource` between template and transaction manager.
- `backlog/docs/feature-guides.md` documents the requirement.

### Tests

- `PostgresStreamStateRepositoryTest.aConflictOnOneStateRowCommitsNothingOfTheMessage`: deterministic stale join row;
  no inbox, state or outbox rows remain.
- `refusesATransactionManagerForAnotherDataSourceBeforeWritingAnything` and
  `rejectsADataSourceTransactionManagerForAnotherDataSource`: reproduce the miswiring. Before the fix the first got an
  `OptimisticConflictException` after a partial commit and the second constructed fine.
- `SnsSqsStreamsIntegrationTest` passed 20 consecutive local runs (Docker, LocalStack + Postgres).

### Pub/Sub (AC#4)

The cause is in the wiring, guarded now in the shared repository. The Pub/Sub backend has no test or code that builds
a `PostgresStreamStateRepository`; production wiring goes through `PostgresStreamsStateConfiguration`, which uses the
application's beans and is covered by the new guard.

### Follow-up

TASK-372: read-only keys and deletes of keys observed absent are not version-checked at commit.
<!-- SECTION:NOTES:END -->
