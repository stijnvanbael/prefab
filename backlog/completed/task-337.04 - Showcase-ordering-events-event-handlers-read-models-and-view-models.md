---
id: TASK-337.04
title: 'Showcase ordering: events, event handlers, read models and view models'
status: Done
assignee:
  - '@claude'
created_date: '2026-11-20 19:47'
updated_date: '2026-11-23 07:20'
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
- [x] #1 No event names a messaging platform; all derive it from the classpath
- [x] #2 Each listed event feature is demonstrated with an integration test
- [x] #3 Broker-specific tests declare the capability they need
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Domain slices in `examples/showcase`: `customer` (`Customer`, `CustomerRegistered` v2 with `@EventMigration`, `CustomerDeactivated`, `CustomerOrderHistory` projection), `ordering` (`Order`, sealed `OrderEvent`, `OrderOverview` view), `inventory` (`StockLevel`), `invoicing` (`Invoice`, `InvoicePaid`).
2. One integration test per feature through the generated clients, with a `Shop` test helper that sets up customers and stocked products.
3. Run on `showcase` (Postgres + Kafka) and `showcase-mongodb`; file a task for framework gaps.
4. Feature table in `examples/README.md`.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
All 79 showcase tests pass on `showcase` and on `showcase-mongodb`, without a dead-lettered message. The feature table is in `examples/README.md`.

Choices and deviations from the description:
- **Platform**: no event names one (`EventPlatformTest` scans the showcase and fails otherwise). Topics are literals (`showcase.order`, ...), so variants need no topic configuration.
- **Sealed hierarchy**: only `OrderEvent`. `CustomerRegistered` is a standalone event because migrations are keyed by the concrete event type, and `@EventSnapshot` is Avro only, so the JSON event uses a hand-written snapshot record (`CustomerRegisteredV1`) as in decision-003. The migration is tested without a broker with `EventMigrator`, so the test runs on every variant.
- **`@ByReference` fetches the aggregate by id**, so it only fits `Invoice` (event carries the invoice id). Stock, history and invoice-by-order use `@Multicast` with a repository mixin query. Both order events are consumed by one generated consumer per aggregate, which is what makes per-key ordering observable: the history test places and cancels orders back to back and needs the cancellation to arrive after the placement (`ORDERED_DELIVERY`).
- **`@AsyncCommit`** is on the `Invoice.pay` update method, not on the aggregate: on the aggregate every handler needs an `@EventId` on its event.
- **Multicast with no match fails**: the generated handler throws "No aggregates found" and the message is retried and dead-lettered. That made my first tests (orders for customers without a projection) fill the DLT, so the `Shop` helper waits for the customer's history. Deactivating a customer without orders would dead-letter too, so that case is not exercised. Filed as TASK-365.
- **`@TenantId` and a dead-letter test are not included.** Tenancy needs a request-scoped tenant for every showcase test (the column is `NOT NULL`). `@EventHandlerConfig` (concurrency, retry limit) is applied on `StockLevel` and exercised by its happy path; asserting the DLT contents is broker specific and no `Capability` exists for it yet.
- Event baselines (`prefab/event-baseline`) are not committed; they are a release-time artefact and the other modules do not commit them either.
- Docker was started manually in this session (`dockerd`) to run the Testcontainers-based tests.
<!-- SECTION:NOTES:END -->
