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

### Client code: code-first, minimal annotations

An earlier sketch used a field-level annotation DSL (`@From`, `@Join`, `@Count`, `@Expand`). It was rejected:
string-typed, limited to simple shapes, and every exception (conditional logic, external data, custom
aggregation) forces a rewrite as handwritten code. The revised principle: **Prefab owns the plumbing, the
developer owns the mapping, in plain typed Java.** One annotation binds a view to its aggregate; the rest is
interfaces the developer implements. The read-side annotation is `@ViewOf`; command adapters implement
`ViewCommands<Aggregate>`.

Prefab generates: controller (paged GET list/by id), service wiring, tenant/security scoping of the source
rows, validation, transaction boundary, OpenAPI, test client and mother. The developer writes the DTO and
the mapping.

**Shorthand: a constructor on the view.** For simple mappings the view itself declares how it is built from
the aggregate, so no assembler class is needed. Prefab detects a constructor taking the aggregate (or a
static `from(Aggregate)` factory) and uses it as the mapper. Existing annotations configure the endpoints.

```java
@ViewOf(Product.class)
@GetById
@GetList
public record ProductListItem(String id, String title, int tagCount) {

    public ProductListItem(Product product) {                                 // mapping shorthand
        this(product.id().id(), product.name(), product.tags().size());
    }
}
```

Shorthand ladder, from least to most code: (1) field names match, nothing to write (generated mapper);
(2) constructor or `from(...)` on the view for per-row mapping; (3) `ViewAssembler` when the mapping needs
the whole page (joins, aggregation, enrichment, external data). A constructor taking the aggregate plus
extra parameters (for example `ProductListItem(Product p, Map<Reference<User>, User> users)`) is
resolved by the assembler, which keeps batch loading explicit.

**Read side: a batch assembler.** It receives an already scoped, paged list of source aggregates, so joins,
aggregation and enrichment are ordinary Java with one lookup per page, never per item.

```java
@ViewOf(ChannelSummary.class)
@GetList
public record ChannelDashboardItem(String id, String name, int totalMessages, int unread) {}

@Component
class ChannelDashboardAssembler implements ViewAssembler<ChannelSummary, ChannelDashboardItem> {
    private final UserStatusRepository statuses;
    private final UserRepository users;

    @Override
    public List<ChannelDashboardItem> assemble(List<ChannelSummary> page, ViewContext ctx) {
        var unread = statuses.findByUser(ctx.currentUser()).stream()          // join
                .collect(toMap(s -> s.channel(), s -> s.unreadMessages().size()));
        return page.stream()
                .map(c -> new ChannelDashboardItem(                           // field mapping + rename
                        c.id().id(), c.name(), c.totalMessages(),             // aggregation is plain Java
                        unread.getOrDefault(c.channel(), 0)))
                .toList();
    }
}
```

Enrichment from other aggregates or external systems is the same shape (`users.findAllById(ids)` once, then
map). When view fields equal aggregate fields no assembler is needed: a default mapper is generated, so
the trivial case stays zero-code. Anything not expressible stays in Java instead of a DSL.

**Write side: view commands translate to generated aggregate operations.** The developer maps the form to
calls on generated services. The existing `@Create`, `@Update` and `@Delete` annotations (path, method,
security) configure the endpoints, exactly as on aggregates; they are placed on the command methods, and
endpoints exist only for annotated methods. Prefab generates validation, the `@Transactional` boundary and
the test client.

```java
@Component
class ProductEditor implements ViewCommands<Product> {
    private final ProductService products;

    @Create
    public Product create(ProductForm form) {                                 // POST /product-editor
        var created = products.create(new CreateProductRequest(form.title(), form.details()));
        form.tags().forEach(t -> products.addTag(created.id(), new AddTagRequest(t)));
        return created;
    }

    @Update(path = "/tags", method = "POST")
    public void addTag(String id, AddTagForm form) {                          // POST /product-editor/{id}/tags
        products.addTag(id, new AddTagRequest(form.tag()));
    }

    @Delete
    public void delete(String id) { products.delete(id); }                    // DELETE /product-editor/{id}
}
```

The same constructor shorthand applies to commands: a form record may declare
`CreateProductRequest toRequest()` (or the command a one-to-one mapping) so a simple `@Create` needs no body:

```java
@ViewOf(Product.class)
@Create                                                                       // delegates to ProductService.create
public record ProductForm(String title, ProductDetails details) {
    public CreateProductRequest toRequest() { return new CreateProductRequest(title, details); }
}
```

**Propagation.** (a) Computed views read live aggregates, so create/update/delete propagate automatically
with no code; this is the default and the recommendation. (b) Deletes of a source aggregate simply remove
the row from the view; dangling references seen by an assembler are handled in the assembler (skip, placeholder
or fail), and the documented default is explicit, not silent. (c) If a view must be materialised for
performance, it is an event-maintained read model (`@EventHandler`, TASK-170/TASK-319), and propagation of
create/update/delete is its event handlers, which Prefab already supports. (d) Cascades and cross-aggregate
invariants stay in the domain (aggregates, events), never in the view. Atomicity limits from "Transactions and
compensation" below still apply to multi-aggregate view commands (TASK-328).

### Internal aggregates behind a BFF

`@Aggregate(exposure = Exposure.INTERNAL)` keeps aggregate services in-process while suppressing aggregate
HTTP endpoints. Operation-level `exposure` can override the aggregate default:

```java
@Aggregate(exposure = Exposure.INTERNAL)          // default for all operations: service only, no controller
@GetById
@GetList
public record Channel(...) {

    @Create
    public Channel(...) { ... }

    @Update(path = "/rename", exposure = Exposure.PUBLIC)   // per-operation override
    public void rename(String name) { ... }
}
```

- `INTERNAL` generates the service method, request/response records and repository, but no controller
  method, OpenAPI entry or HTTP test client. `PUBLIC` (the default, so existing apps are unchanged) generates
  everything as today. Operation-level `exposure` overrides the aggregate default.
- Views are the public face: `@ViewOf(Channel.class)` with `@GetList` generates the endpoint, calling the
  internal service. A view may be `@Update`/`@Create`-annotated too (see view commands), so an aggregate can
  be fully internal while its views and commands are the only HTTP surface.
- Because internal services have no HTTP security layer, tenant filtering still applies (it lives in the
  service), but authorization belongs to the view endpoint, which must declare its own security. The
  generated service stays a Spring bean; to stop other code from calling an internal aggregate by accident,
  an optional ArchUnit-style compile check can restrict callers to view assemblers/commands (open design
  question, not promised).
- A view of an aggregate in `Exposure.PUBLIC` also works, so exposure is an adoption choice, not a
  prerequisite.
- `Exposure` is a visibility gate (`PUBLIC` or `INTERNAL`), while TASK-127's planned `endpoints` attribute selects
  transport strategies such as REST and MCP. A future strategy module must honor both: an internal operation must not
  produce a remote endpoint, while a public operation may target one or more enabled strategies.

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

`examples/kafka/src/main/java/be/appify/prefab/example/kafka/dashboard/ChannelDashboardItem.java` is a
`@ViewOf(ChannelSummary.class)` list contract. Prefab's generated view service reuses the filtered, sorted and
paged `ChannelSummaryService`; `ChannelDashboardAssembler` enriches its batch with one
`UserStatusRepository.findByUser(...)` lookup. The generated view preserves page metadata and avoids one query per
channel. If the asynchronous status projection is absent, the item marks `unreadStatusAvailable` false instead of
implying that the count is authoritative. Repository failures are not converted into successful-looking empty
results.

`examples/kafka/src/main/java/be/appify/prefab/example/kafka/product/application/ProductCommandService.java`
composes generated `ProductService.create(...)` and `addTag(...)` calls. Its outer `@Transactional` boundary
joins the generated services' default REQUIRED transaction on the same PostgreSQL transaction manager.
`Product` does not publish events, so the demonstrated writes are covered by that local transaction. An
empty result during the follow-up update raises an exception and rolls back when called through the Spring
proxy. It does not claim atomicity across aggregate roots, databases, or a message broker.

Focused, runnable tests:

```shell
mvn -pl :kafka-example -am "-Dtest=ChannelDashboardAssemblerTest,ProductCommandServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

The dashboard tests verify batch mapping, missing versus empty projection state, and one repository lookup per
assembler batch. The generated view service supplies filtering, stable paging and page metadata. The command tests
verify generated-operation ordering and explicit failure handling.

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
