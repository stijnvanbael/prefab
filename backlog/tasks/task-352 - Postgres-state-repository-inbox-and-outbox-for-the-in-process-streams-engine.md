---
id: TASK-352
title: 'Postgres state repository, inbox and outbox for the in-process streams engine'
status: To Do
assignee: []
created_date: '2026-11-21 10:00'
labels:
  - streams
  - postgres
  - coverage-gap
dependencies:
  - TASK-351
priority: medium
type: feature
ordinal: 198000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement `StreamStateRepository` (TASK-351) on Postgres with `JdbcTemplate`, following decision-008:
- The `prefab_stream_store`, `prefab_stream_window`, `prefab_stream_join_buffer`, `prefab_stream_inbox`, `prefab_stream_outbox` and `prefab_stream_lease` tables. Every state row has a version column for optimistic concurrency. Values are stored as `bytea`, written with the broker serializers. Keys use the shared key encoding.
- Schema initialization like Spring Batch's (`prefab.streams.state.initialize-schema=always|never`, default `always` for embedded and Testcontainers databases). The DDL is published in the developer guide for teams that manage their own schema. Do not use the application's Flyway `V{n}` sequence.
- Use the Spring `PlatformTransactionManager` for the per-message transaction. On a version conflict (unique violation or a stale-version update count of 0), throw a dedicated exception that the engine retries.
- Outbox relay lease per sink topic, with expiry and takeover, and cleanup queries for closed windows, join buffers, deduplication identities and inbox rows.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The StreamStateRepository contract suite from TASK-351 passes against Postgres (Testcontainers)
- [ ] #2 Concurrent updates to the same key from two engine instances lose no update (integration test)
- [ ] #3 An output row is published even when the instance crashes between commit and publish (relay takes over after the lease expires)
- [ ] #4 The developer guide documents the tables, schema initialization, retention properties and the DDL
<!-- AC:END -->
