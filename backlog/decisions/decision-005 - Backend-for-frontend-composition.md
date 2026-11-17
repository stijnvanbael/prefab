---
id: decision-005
title: Compose frontend contracts in handwritten application adapters
date: '2026-11-17'
status: accepted
related_tasks:
  - TASK-320
  - TASK-319
  - TASK-324
---

## Context

Frontend screens often need a read contract that joins generated aggregate operations or a command that
sequences several domain operations. Adding these screen-specific contracts to aggregate write models
would mix presentation concerns into the domain. A dedicated BFF module or annotation could also duplicate
the existing controller, service and repository extension points.

The Kafka example provides generated Spring services for `ChannelSummary`, `UserStatus` and `Product`, plus
PostgreSQL repository mixins. Generated aggregate services are `@Transactional`. Event dispatch is different:
`SpringDomainEventPublisher` dispatches directly to infrastructure beans, and an aggregate may publish during
construction, before its repository save.

## Decision

- Prefer a handwritten Spring application service in the application that owns the generated services. Keep
  screen DTOs and orchestration out of aggregate records. Inject generated services/repositories; do not
  instantiate or replace generated classes.
- Use a repository mixin for a bounded, purpose-built batch read when generated filters do not express it.
  Keep SQL filtering and tenant/row-authorization predicates in that query, before ordering, pagination and
  counts. Do not fetch one related row per screen item.
- Do not add a BFF module, annotation, or plugin hook for this use case. Generated services are internal
  in-process Java collaborators, not a versioned network contract.
- Start with an in-process adapter. Deploy a separate BFF only when independent ownership, deployment,
  scaling, or aggregation across independently deployed backends justifies the additional network boundary.

## Runnable examples

`examples/kafka/src/main/java/be/appify/prefab/example/kafka/dashboard/ChannelDashboardService.java` composes
a page from the generated `ChannelSummaryService` with one `UserStatusRepository.findByUser(...)` lookup.
It maps the rows in memory to `ChannelDashboardItem`, preserving page metadata, and sorts by `name` then `id`
for stable pagination. The adapter operation count is constant with respect to the number of page items; it
does not issue one status query per channel. If the asynchronous status projection is absent, the item marks
`unreadStatusAvailable` false instead of implying that the count is authoritative. Repository failures are
not converted into successful-looking empty results.

`examples/kafka/src/main/java/be/appify/prefab/example/kafka/product/application/ProductCommandService.java`
composes generated `ProductService.create(...)` and `addTag(...)` calls. Its outer `@Transactional` boundary
joins the generated services' default REQUIRED transaction on the same PostgreSQL transaction manager.
`Product` does not publish events, so the demonstrated writes are covered by that local transaction. An
empty result during the follow-up update raises an exception and rolls back when called through the Spring
proxy. It does not claim atomicity across aggregate roots, databases, or a message broker.

Focused, runnable tests:

```shell
mvn -pl :kafka-example -am "-Dtest=ChannelDashboardServiceTest,ProductCommandServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

The dashboard tests verify stable paging, composed output, missing versus empty projection state, and one
batch repository lookup. The command tests verify generated-operation ordering and explicit failure handling.

## Operational contract

### Authentication and tenant isolation

An in-process adapter remains in the caller's Spring request/security context, but it does not automatically
authorize arbitrary identifiers passed to it. Its HTTP/controller boundary must derive the user identity
from the authenticated principal, enforce row-level access, and not trust a client-supplied `userId`.
`TenantContextProvider` must resolve a tenant from trusted authentication context in production; its default
implementation returns `null`, which disables generated tenant filtering. Aggregates that require tenant
isolation must model `@TenantId`. Custom repository mixins do not inherit generated service filtering:
they must apply tenant and row-authorization constraints in the query itself, before pagination and counts.
The example has no tenant field and is not evidence of tenant isolation.

A separately deployed BFF must authenticate its own caller and propagate identity and tenant claims using
an audience-appropriate bearer token or token exchange, with only the required scopes. Do not forward
unvalidated identity headers or a token whose audience is not accepted by the downstream service.

### Timeouts and partial failures

For in-process calls, bound page sizes at the API boundary and configure database/query timeouts within the
overall request deadline. For a remote BFF, assign each dependency a shorter connect/read deadline than
the overall request, bound concurrency, and avoid unbounded retries. Required dashboard reads fail the
request when a dependency fails; optional sections may be degraded only when the response explicitly marks
them unavailable. A missing asynchronous projection is different from a failed query and must be represented
as such.

### Transactions and compensation

The product prototype is one local database transaction. A larger use case may compose generated operations
under one transaction only when they use the same transaction manager and the desired business invariant is
local. That does not change aggregate boundaries. Prefab's direct event dispatch is not an outbox and is not
atomically committed with a database transaction; event publication can precede persistence. Cross-broker or
cross-service workflows therefore need an explicit outbox/saga and idempotent compensation policy if the
business requires recovery. A separately deployed BFF cannot share the backend database transaction and
must treat a multi-service command as a distributed workflow.

### Query growth and consistency

The dashboard performs one generated paged service operation and one batched status repository operation,
independent of page length. Pagination is stable; it is not a database join, and `ChannelSummary`/`UserStatus`
are event-maintained projections that may lag their source aggregates. If embedded status data becomes too
large, add a query-specific projection/join rather than per-item loads. Counts and authorization must use the
same tenant and row restrictions as the returned rows.

## Deployment and contract trade-offs

| Boundary | Coupling and contract evolution | Deployment and runtime cost |
|---|---|---|
| In-process adapter (recommended default) | Coupled to generated Java service/repository types, but a handwritten screen DTO separates the frontend response from aggregate response records. One deployable owns API evolution. | No network hop or token forwarding between adapter and services; local transaction can span calls using one manager. Scaling and release cadence remain shared. |
| Separately deployed BFF | Backend services must expose versioned HTTP/event contracts. Independent frontend response evolution is possible, but mapping and compatibility work move across a network boundary. | Independent deploy/scale and technology choice; adds authentication propagation, deadlines, retries, partial-failure policy, observability and distributed-command recovery. No shared transaction. |

No separate follow-up story is justified by these prototypes: current handwritten services and repository mixins cover the demonstrated composition needs. Reliable event publication/outbox semantics are a separate infrastructure concern and are not promised by this BFF decision.
