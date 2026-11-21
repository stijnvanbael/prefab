---
id: TASK-353
title: MongoDB state repository for the in-process streams engine
status: To Do
assignee: []
created_date: '2026-11-21 10:00'
labels:
  - streams
  - mongodb
  - coverage-gap
dependencies:
  - TASK-351
  - TASK-352
priority: low
type: feature
ordinal: 199000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement `StreamStateRepository` (TASK-351) on MongoDB with `MongoTemplate`, so Pub/Sub and SNS/SQS applications on MongoDB can use Prefab Streams (decision-008). Use one collection per table of the Postgres design (TASK-352), with version fields for optimistic concurrency and indexes for the cleanup queries. Multi-document transactions need a replica set. Fail fast at startup when the client is connected to a standalone server, and document that the Testcontainers `MongoDBContainer` already runs as a single-node replica set.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The StreamStateRepository contract suite from TASK-351 passes against MongoDB (Testcontainers)
- [ ] #2 Startup fails with a clear message on a MongoDB deployment without transactions
- [ ] #3 The decision-008 operator matrix is updated: state on MongoDB is supported
<!-- AC:END -->
