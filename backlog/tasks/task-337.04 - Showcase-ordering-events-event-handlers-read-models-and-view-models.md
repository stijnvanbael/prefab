---
id: TASK-337.04
title: 'Showcase ordering: events, event handlers, read models and view models'
status: To Do
assignee: []
created_date: '2026-11-20 19:47'
labels:
  - examples
  - events
dependencies:
  - TASK-337.03
parent_task_id: TASK-337
priority: high
type: task
ordinal: 175000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Customers, orders and invoices in `examples/showcase`, covering the event-driven side of Prefab. Replaces the chat domain (channel/message/user) of the old kafka, pubsub and sns-sqs examples.

Cover: `@Event` with `Platform.DERIVED` (no broker named in source), `@PartitioningKey`, sealed event hierarchies, `@EventHandler` updating other aggregates (order placed → stock reserved, invoice created), projections/read models (customer order history) with repository mixins, `@Multicast`, `@AsyncCommit`, `@EventHandlerConfig` (concurrency, retries/DLT), event versioning (`@EventMigration`/`@EventSnapshot`), aggregate-bound view models (`@ViewOf`), `@TenantId` if it fits naturally.

Tests that depend on broker-specific behaviour (ordering per key, DLT) declare their capability (TASK-337.02).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 No event names a messaging platform; all derive it from the classpath
- [ ] #2 Each listed event feature is demonstrated with an integration test
- [ ] #3 Broker-specific tests declare the capability they need
<!-- AC:END -->
