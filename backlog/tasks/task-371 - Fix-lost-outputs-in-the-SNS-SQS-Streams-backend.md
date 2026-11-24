---
id: TASK-371
title: Fix lost outputs in the SNS/SQS Streams backend
status: To Do
assignee: []
created_date: '2026-11-24 05:40'
labels:
  - streams
  - sns-sqs
dependencies: []
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
- [ ] #1 The root cause is identified and recorded in the implementation notes, with a test that reproduces it deterministically
- [ ] #2 Every effect of an input message (state changes, join entries, outbox records) commits together or not at all, also when sources run concurrently or a conflict is retried
- [ ] #3 `SnsSqsStreamsIntegrationTest` passes in 20 consecutive runs (e.g. a loop in CI or locally with Docker)
- [ ] #4 If the cause is shared with the in-process engine or the Postgres state repository, the Pub/Sub backend is checked for the same problem
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
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
