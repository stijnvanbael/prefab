# Prefab examples

`showcase` is a shop application (catalog, customers, orders, invoices, sales analytics) that shows Prefab on the
reference stack: **Postgres + Kafka (JSON)**. The `showcase-<technology>` modules are variants: they contain only a
pom, configuration and a capabilities file, and run the *same* sources and tests on another technology.
See [decision-007](../backlog/decisions/decision-007%20-%20One-showcase-recompiled-per-technology-variant.md) for how
that works and why.

## What the showcase shows

Most feature integration tests use generated test clients as usage examples. Security and documentation tests use
MockMvc directly to exercise authentication and the served documents. Tests run on every variant (see
[Technology coverage](#technology-coverage)).

### Catalog: REST

Sources in [`catalog`](showcase/src/main/java/be/appify/prefab/example/showcase/catalog), tests in
[`catalog`](showcase/src/test/java/be/appify/prefab/example/showcase/catalog).

| Feature | Where | Test |
|---|---|---|
| `@Create`, `@GetById`, `@Update`, `@Delete` | `Product` | `ProductIntegrationTest` |
| Default authenticated reads and `@Security` role/authority checks on catalog writes | `Product`, `Category` | `ShowcaseSecurityAndDocumentationIntegrationTest` |
| Customer-owned order history; direct order reads are admin-only | `CustomerOrderHistoryAccessController`, `Order`, `OrderOverview` | `CustomerOrderHistoryIntegrationTest` |
| Jakarta Validation, `@ContentType`, `@FileSize` | `Product`, `Category`, `Promotion` | `ProductValidationIntegrationTest` |
| `@GetList` with `@Filter` (equality, range, full text), paging, `@SortOrder` | `Product`, `Category` | `ProductListIntegrationTest`, `CategoryIntegrationTest` |
| `@Text` (unbounded column) | `Product.description` | `ProductIntegrationTest` |
| `@Autocomplete` | `Product.brand`, `Category.name` | `AutocompleteIntegrationTest` |
| `@Parent` (nested resource) | `ProductVariant` | `ProductVariantIntegrationTest` |
| Polymorphic aggregate (sealed interface) | `Promotion` | `PromotionIntegrationTest` |
| `Binary` upload and `@Download` | `Product.image` | `ProductImageIntegrationTest` |
| Audit fields (`AuditInfo`: `@CreatedAt`, `@CreatedBy`, `@LastModifiedAt`, `@LastModifiedBy`) | `Product.audit` | `ProductAuditIntegrationTest` |
| Optimistic locking (`@Version`) | `Product.version` | `ProductOptimisticLockingIntegrationTest` |
| `@RepositoryMixin` | `ProductRepositoryMixin` | `ProductRepositoryMixinIntegrationTest` |

### Security and API documentation

The showcase adds `prefab-security`, `prefab-openapi` and `prefab-async-api` to both the reference application and
the shared variant build. Product creation and updates require the `ADMIN` role, deletion requires the
`ROLE_ADMIN` authority, and normal product reads use Prefab's default authenticated access. Category listing disables
endpoint-level security with `@Security(enabled = false)` and is still subject to the filter-chain authentication
policy. The integration test checks the generated rules with both admin and customer principals.
Order records and overviews are admin-only; customer history is available only at
`/customers/{customerId}/order-history` when the authenticated principal name matches the customer ID. There is no
generic customer-history list endpoint.

The `local` and `test` profiles use HTTP Basic and Spring Boot's generated development user, so the app starts without
real OAuth credentials or checked-in passwords. Outside those profiles Prefab's JWT resource-server configuration
remains active. The showcase's `AuditContextProvider` records the authenticated principal in the audit fields.

| Document | Route |
|---|---|
| OpenAPI JSON | `/v3/api-docs` |
| Swagger UI | `/swagger-ui/index.html` |
| AsyncAPI JSON | `/async-api/asyncapi.json` |
| AsyncAPI viewer | `/async-api` |

### Customers, ordering and invoicing: events

Sources in [`customer`](showcase/src/main/java/be/appify/prefab/example/showcase/customer),
[`ordering`](showcase/src/main/java/be/appify/prefab/example/showcase/ordering),
[`inventory`](showcase/src/main/java/be/appify/prefab/example/showcase/inventory) and
[`invoicing`](showcase/src/main/java/be/appify/prefab/example/showcase/invoicing). Placing an order publishes
`OrderEvent.Placed`; stock is reserved, an invoice is created and the order history of the customer is updated, each by
its own event handler. No event names a messaging platform, so the same sources publish to Kafka, Pub/Sub or SNS/SQS.

| Feature | Where | Test |
|---|---|---|
| `@Event` with the platform derived from the classpath (`Platform.DERIVED`) | all events | `EventPlatformTest` |
| `@PartitioningKey` | `OrderEvent.order()`, `CustomerRegistered.customer` | `CustomerOrderHistoryIntegrationTest` (ordered delivery, needs `ORDERED_DELIVERY`) |
| Sealed event hierarchy | `OrderEvent` (`Placed`, `Cancelled`) | `StockLevelIntegrationTest`, `InvoiceIntegrationTest` |
| `@EventHandler` updating another aggregate | `StockLevel` (order placed: stock reserved) | `StockLevelIntegrationTest` |
| `@EventHandler` on a static method creates an aggregate | `Invoice` (order placed: invoice created) | `InvoiceIntegrationTest` |
| `@ByReference` | `Invoice.onInvoicePaid` | `InvoiceIntegrationTest` |
| Projection with repository mixin | `CustomerOrderHistory`, `CustomerOrderHistoryRepositoryMixin` | `CustomerOrderHistoryIntegrationTest` |
| `@Multicast` | `Order.onCustomerDeactivated`, `StockLevel`, `Invoice`, `CustomerOrderHistory` | `OrderCancellationOnDeactivationIntegrationTest` |
| `@AsyncCommit` | `Invoice.pay` (`202 Accepted`, applied when the invoice handles its own event) | `InvoiceIntegrationTest` |
| `@EventHandlerConfig` (concurrency, retries) | `StockLevel` | `StockLevelIntegrationTest` |
| Event versioning: `@Event(version)`, `@EventMigration` and a snapshot record | `CustomerRegistered`, `CustomerRegisteredV1`, `CustomerRegisteredMigration` | `CustomerRegisteredMigrationTest` |
| `@ViewOf` aggregate-bound view model | `OrderOverview` | `OrderOverviewIntegrationTest` |

Not shown: `@TenantId`. Tenant isolation needs a `TenantContextProvider` and a tenant on every request, which would
touch every other showcase test; the technique is covered by the annotation processor tests.
A `@Multicast` fails (and dead-letters) when its query finds no aggregates, so the showcase only targets aggregates
that exist, see TASK-357.

## Technology coverage

Not every feature works on every stack. A variant lists what it lacks in
`src/test/resources/showcase-capabilities.yml`; every gap carries a reason and the backlog task that closes it:

```yaml
variant: showcase-pubsub
unsupported:
  STREAMS:
    reason: no Pub/Sub backend for Prefab Streams
    task: TASK-341
```

- A test that needs a capability declares `@RequiresCapability(Capability.STREAMS)`. On a variant without it, the test
  is skipped with a message like
  `showcase-pubsub: STREAMS not supported — no Pub/Sub backend for Prefab Streams (TASK-341)`.
- Capabilities whose code lives in optional source folders (streams, Avro) have no compiled tests on variants that lack
  them. `CapabilityGapsTest` reports one skipped test per unsupported capability instead, so gaps show up in every build.
- An unknown capability, or a gap without a reason or backlog task, fails the build.

| Symbol | Meaning |
|:---:|---|
| ✅ | supported, covered by the showcase tests |
| ❌ | gap, linked to the backlog task that tracks it |

`CoverageMatrixTest` fails when this table differs from the capability files. On a mismatch it prints the expected
table, so after changing a capability file, paste that between the markers.

<!-- coverage-matrix:begin -->
| Capability | showcase | showcase-mongodb |
| --- | :---: | :---: |
| FULL_TEXT_SEARCH | ✅ | ✅ |
| RANGE_FILTER | ✅ | ✅ |
| POLYMORPHIC_AGGREGATES | ✅ | ✅ |
| BINARY_DOWNLOAD | ✅ | ✅ |
| ORDERED_DELIVERY | ✅ | ✅ |
| STREAMS | ✅ | ✅ |
| AVRO | ✅ | ✅ |
| SECURITY | ✅ | ✅ |
| OPENAPI | ✅ | ✅ |
| ASYNCAPI | ✅ | ✅ |
<!-- coverage-matrix:end -->

## Adding a variant

1. Create `examples/showcase-<technology>` with a pom (parent `showcase-variant-parent`, only the technology
   dependencies), `application.yml`, `application-test.yml` and `showcase-capabilities.yml`.
2. Register the module in the root `pom.xml` and add a `Showcase tests (<technology>)` run configuration in `.run/`.
3. Run `CoverageMatrixTest` and paste the expected table into this file.
