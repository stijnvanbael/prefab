---
id: decision-005
title: Compose frontend contracts in handwritten application adapters
date: '2026-11-17'
status: accepted (revised, see "Revision: declarative frontend models")
related_tasks:
  - TASK-320
  - TASK-319
  - TASK-324
  - TASK-170
  - TASK-326
  - TASK-327
  - TASK-328
---

## Revision: declarative frontend models

The first version of this ADR concluded that handwritten adapters were enough and that no follow-up was
justified. That was too weak for an out-of-the-box, declarative framework: the prototypes below needed ~150
lines of handwritten DTO, mapping, sorting and batching code. Handwritten adapters remain the **escape
hatch**, not the default. The recommendation is revised as follows.

### Options assessed

| # | Option | Boilerplate removed | Risk / cost | Verdict |
|---|---|---|---|---|
| 1 | Aggregate-bound view: `@ViewOf(Aggregate.class)` on a record (field subset, renames, `@Computed`), generating mapper, service method, endpoint, test client and mother | DTO mapping, controller, paging, sorting, tenant/security reuse | Presentation stays outside the aggregate; view only references aggregate fields so it cannot drift silently (compile error on unknown field). Needs a new annotation and processor writer | **Recommended first slice** (TASK-326) |
| 2 | `Reference<T>` expansion in a view (`@Expand` field resolved with one batched lookup) | Hand-written joins and N+1 avoidance | Needs a batch `findAllById` on generated repositories; backend limits (non-SQL) must be explicit. Overlaps TASK-319 | Build on option 1 (TASK-327) |
| 3 | Declarative composed command (several `@Create`/`@Update` steps in one `@Transactional`) | Orchestration service and request mapping | Easy to over-promise atomicity; Prefab events are not an outbox; sagas need real design | Defer: design-only follow-up (TASK-328) |
| 4 | Column projections (fetch only view fields) | Over-fetching | Optimisation; needs option 1 first | Later optimisation of option 1 |
| 5 | Handwritten in-process adapter (this spike's prototypes) | None | Boilerplate, tenant/security easy to forget in mixins | Escape hatch, documented |
| 6 | Separate BFF service | None | Network boundary, token propagation, no shared transaction | Only for independent ownership/scaling |

`@ReadModel` (TASK-170) is complementary, not a substitute: it marks an event-populated aggregate as read-only,
whereas `@ViewOf` shapes the API contract of any aggregate. A view of a `@ReadModel` aggregate is the expected
common case for cross-aggregate screens (TASK-319).

### Why a view tied to the aggregate root

- The aggregate stays the single source of truth; view fields are validated against it at compile time.
- Generated services already apply tenant filtering, paging and `@Filter`; a generated view can reuse them
  instead of each hand-written mixin re-implementing authorization predicates.
- Multiple views per aggregate allow several frontends/versions without touching the domain (also helps
  TASK-321 versioned contracts).
- Plugin-only prototyping (via `PrefabPlugin.writeController`/`writeService`) is feasible before changing core.

### Illustrative client code (proposed API, not implemented)

Sketches of what an application developer would write with `@ViewOf` (TASK-326) and reference expansion
(TASK-327). Names (`@ViewOf`, `@From`, `@Expand`, `@Aggregate`-style `@Sum`) are placeholders; the point is the
amount of code compared with the handwritten prototypes. Everything else (mapper, service, endpoint, test
client, mother, tenant/security filtering) is generated.

**1. Field mapping: subset, rename, computed**

```java
@ViewOf(Product.class)
@GetList
public record ProductListItem(
        String id,
        @From("name") String title,          // rename
        @Computed String label) {            // computed from view fields only

    static String label(String title) { return title.toUpperCase(); }
}
```

**2. Aggregation: counts and sums over a related aggregate, computed in the query (no per-item loads)**

```java
@ViewOf(Channel.class)
@GetList
public record ChannelDashboardItem(
        String id,
        String name,
        @From("summary.totalMessages") int totalMessages,
        @Count(of = Message.class, by = "channel") long messageCount) {}
```

**3. Join: combine two aggregates through a `Reference`**

```java
@ViewOf(ChannelSummary.class)
@GetList
public record ChannelWithUnread(
        String id,
        String name,
        @Join(UserStatus.class, on = "channel", filter = "user = :currentUser")
        @From("unreadMessages.size") int unread) {}   // :currentUser derived from the authenticated principal
```

**4. Enrichment: expand a `Reference` into a nested view with one batched lookup per page**

```java
@ViewOf(Message.class)
@GetList
public record MessageItem(
        String id,
        String text,
        @Expand UserSummary sender) {}               // Reference<User> resolved via findAllById

@ViewOf(User.class)
public record UserSummary(String id, String name) {}
```

Missing references yield `null` (or the documented placeholder), never an exception; unsupported backends
fail at compile time. Anything beyond these shapes (external calls, multi-step commands) falls back to a
handwritten adapter such as `ChannelDashboardService`.

### Revised decision

- Introduce a declarative frontend view model (TASK-326) as the preferred BFF building block; add reference
  expansion (TASK-327); design composed commands separately (TASK-328).
- The handwritten decisions below still apply to anything the declarative model cannot express.


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
- Do not add a separate BFF module. (Original text said "or annotation"; superseded by the revision above:
  a view annotation is the preferred route.) Generated services are internal in-process Java collaborators,
  not a versioned network contract.
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

Follow-up stories: TASK-326 (aggregate-bound `@ViewOf`), TASK-327 (reference expansion in views), TASK-328 (composed-command design). Reliable event publication/outbox semantics remain a separate infrastructure concern and are not promised by this decision.
