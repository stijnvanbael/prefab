---
id: TASK-362
title: MongoDB state repository for the in-process streams engine
status: Done
assignee: []
created_date: '2026-11-21 10:00'
labels:
  - streams
  - mongodb
  - coverage-gap
dependencies:
  - TASK-360
  - TASK-361
priority: low
type: feature
ordinal: 199000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement `StreamStateRepository` (TASK-360) on MongoDB with `MongoTemplate`, so Pub/Sub and SNS/SQS applications on MongoDB can use Prefab Streams (decision-008). Use one collection per table of the Postgres design (TASK-361), with version fields for optimistic concurrency and indexes for the cleanup queries. Multi-document transactions need a replica set. Fail fast at startup when the client is connected to a standalone server, and document that the Testcontainers `MongoDBContainer` already runs as a single-node replica set.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The StreamStateRepository contract suite from TASK-360 passes against MongoDB (Testcontainers)
- [x] #2 Startup fails with a clear message on a MongoDB deployment without transactions
- [x] #3 The decision-008 operator matrix is updated: state on MongoDB is supported
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- New module `streams-mongodb` (`prefab-streams-mongodb`), mirroring `streams-postgres`: `MongoStreamStateRepository`, `MongoStreamSchema` (collections, indexes, standalone check), `MongoStreamsStateConfiguration` (auto-configuration on `MongoTemplate`), `MongoStreamsProperties` (`prefab.streams.state.initialize-schema`), `MongoTransactionsUnavailableException` and a `FailureAnalyzer`.
- `StreamStateCodec` and `JsonStreamStateCodec` moved from `streams-postgres` to `streams-engine` (package `be.appify.prefab.streams.engine`) so both repositories share them.
- One private `TransactionTemplate` over a `MongoTransactionManager` (deliberately not a bean). Reads happen outside the transaction; `commit()` applies inbox insert, version-guarded state writes and outbox inserts in one transaction. Duplicate keys, write conflicts and `TransientTransactionError` map to `OptimisticConflictException`.
- Outbox `seq` is `max(nowMicros, maxExistingSeq(topic) + 1)`. The relay lease is claimed with a `findAndModify` upsert; a duplicate key means another owner holds it.
- Startup check: `hello` must report `setName` or `msg=isdbgrid`, otherwise `MongoTransactionsUnavailableException` is thrown (tested against a plain `mongo:9` container).
- Testcontainers 2 `MongoDBContainer` needs `withReplicaSet()` explicitly; it is not a replica set by default.
- `PubSubStreamsConfiguration` and `SnsSqsStreamsConfiguration` now also run after `MongoStreamsStateConfiguration`.
- Tests: `StreamStateRepositoryContractTest` plus concurrent increments, lease takeover, outbox ordering, idempotent init and the standalone startup failure. Docs and decision-008 matrix updated.
<!-- SECTION:NOTES:END -->
