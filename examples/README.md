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
policy unless a matching `HttpSecurityCustomizer` rule permits anonymous access. The showcase permits anonymous
`GET /categories` requests while other category operations remain authenticated. The integration test checks the
generated rules with both admin and customer principals.
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
that exist, see TASK-365.

### Streams: order sales analytics

The optional sources in [`streams`](showcase/src/streams/java/be/appify/prefab/example/showcase/streams) and their
backend-neutral tests in [`streams-test`](showcase/src/streams-test/java/be/appify/prefab/example/showcase/streams)
demonstrate:

| Operator or feature | Showcase topology |
|---|---|
| `filter`, `flatMap`, `process`, `globalStore` | Enrich placed order lines with category data from a replicated catalog |
| `groupBy`, `aggregate` | Running revenue and unit count per category |
| `windowedAggregate` | Hourly category revenue with a five-minute grace period |
| KStream-KStream `join` | Match order events and payment confirmations within a `JoinWindow` |
| `branch`, `merge` | Route placed and cancelled orders, then combine the typed branches |
| Opt-in tracing | Verify source order IDs on aggregate messages |

The catalog is replicated asynchronously, so an order can arrive before its product: such sales are counted in the
`uncategorized` category instead of failing, because a failing message is redelivered forever on brokers without
dead-lettering (the Pub/Sub emulator).

Kafka-native breakout is isolated in [`streams-kafka`](showcase/src/streams-kafka/java/be/appify/prefab/example/showcase/streams/kafka)
and guarded by the `KAFKA_STREAMS_BREAKOUT` capability. The shared variant build leaves the optional source roots out
unless a variant enables the corresponding streams backend. Variants without Streams list `STREAMS` in
`showcase-capabilities.yml`. `showcase-pubsub` runs the common slice on the in-process engine
(`prefab-streams-pubsub` with Postgres state, decision-008) and lists only `KAFKA_STREAMS_BREAKOUT` as unsupported,
because `breakout` hands over the Kafka Streams API. The showcase uses no wall-clock punctuation
(`StreamProcessorContext.schedule`, TASK-363). SNS/SQS can run the common slice the same way once its variant lands.

### Avro: payments and shipments

The optional sources in [`avro`](showcase/src/avro/java/be/appify/prefab/example/showcase) (with the schema in
[`avro/resources`](showcase/src/avro/resources/avro)) and their tests in
[`avro-test`](showcase/src/avro-test/java/be/appify/prefab/example/showcase) serialize two events as Avro. Serialization
is chosen per event in source (`@Event(serialization = AVRO)`), so Avro cannot be a configuration-only variant; the
slice sits next to the JSON events and is compiled only by variants that include it.

| Feature | Where | Test |
|---|---|---|
| Code-first Avro: schema, converters and registry subject generated from records | `PaymentEvent` (`Received`, `Refunded`) | `PaymentIntegrationTest` |
| `@Decimal` (Avro decimal logical type for `BigDecimal`) | `PaymentEvent.Received.amount` | `PaymentIntegrationTest` |
| Avro events consumed by `@EventHandler` (static creation and `@Multicast`) | `Receipt` | `PaymentIntegrationTest` |
| Schema-first Avro: `@Avsc` + `@AvscFile(keyProperty)` generate the event record from a schema file | `ShipmentEvent`, `shipment-dispatched.avsc` | `ShipmentIntegrationTest` |
| Schema registry in tests (`prefab.test.schema-registry.enabled`) | `application-test.yml` | both |

The tests produce and consume the events on a real Kafka and schema registry (Testcontainers) and check that the
schemas are registered (in memory, Confluent's mock registry is used and the registry check is skipped). Running the application locally needs a schema registry at `http://localhost:8081` next to
Kafka. Variants that cannot decode Avro list `AVRO` as unsupported and leave the slice out: Pub/Sub and SNS/SQS write
bare Avro binary without a writer schema (TASK-275), so `showcase-pubsub` and `showcase-sns-sqs` declare that gap and
include the slice once it is fixed.

## Technology coverage

Not every feature works on every stack. A variant lists what it lacks in
`src/test/resources/showcase-capabilities.yml`; every gap carries a reason and the backlog task that closes it:

```yaml
variant: showcase-pubsub
unsupported:
  AVRO:
    reason: Pub/Sub payloads are bare Avro binary without a writer schema
    task: TASK-275
```

- A test that needs a capability declares `@RequiresCapability(Capability.STREAMS)`. On a variant without it, the test
  is skipped with a message like
  `showcase-pubsub: AVRO not supported — Pub/Sub payloads are bare Avro binary without a writer schema (TASK-275)`.
- Kafka-only native breakout tests declare `@RequiresCapability(Capability.KAFKA_STREAMS_BREAKOUT)` independently of
  the broader `STREAMS` capability.
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
| Capability | showcase | showcase-mongodb | showcase-pubsub |
| --- | :---: | :---: | :---: |
| FULL_TEXT_SEARCH | ✅ | ✅ | ✅ |
| RANGE_FILTER | ✅ | ✅ | ✅ |
| POLYMORPHIC_AGGREGATES | ✅ | ✅ | ✅ |
| BINARY_DOWNLOAD | ✅ | ✅ | ✅ |
| ORDERED_DELIVERY | ✅ | ✅ | ✅ |
| STREAMS | ✅ | ✅ | ✅ |
| KAFKA_STREAMS_BREAKOUT | ✅ | ✅ | ❌ [TASK-341](../backlog/completed/task-341%20-%20Prefab-Streams-backend-for-Google-Pub-Sub.md) |
| AVRO | ✅ | ✅ | ❌ [TASK-275](../backlog/tasks/task-275%20-%20Make-Avro-payloads-self-describing-on-Pub-Sub-and-SNS-SQS.md) |
| SECURITY | ✅ | ✅ | ✅ |
| OPENAPI | ✅ | ✅ | ✅ |
| ASYNCAPI | ✅ | ✅ | ✅ |
<!-- coverage-matrix:end -->

### Test infrastructure

`@IntegrationTest` runs on an in-memory backend by default (see
[Feature Guides, 7.20](../backlog/docs/feature-guides.md#720-integration-tests-in-memory-or-containers)). To keep
real-infrastructure coverage in CI, every example module, the reference `showcase` and `showcase-mongodb` set
`prefab.test.containers: true` in their `application-test.yml`, so they run on Testcontainers. The tests that call the
native `@Query` of `UserStatusRepositoryMixin` (`examples/kafka`, `examples/pubsub`, `examples/sns-sqs`) also say so
with `@IntegrationTest(containers = true)`.

`showcase-inmemory` is the exception and proves the in-memory backend: the same sources and tests as the reference
showcase, generated for Postgres + Kafka (with the Avro slice), but without `prefab.test.containers`. Its tests pass
**without Docker**. It leaves out the Streams slices and lists the gaps in its capabilities file: `STREAMS` and
`KAFKA_STREAMS_BREAKOUT` (TASK-372) and `SCHEMA_REGISTRY_API` (TASK-373, the mock schema registry has no HTTP endpoint).
The reference showcase and `showcase-inmemory` cover the same sources on real and in-memory infrastructure.

## Adding a variant

1. Create `examples/showcase-<technology>` with a pom (parent `showcase-variant-parent`, only the technology
   dependencies), `application.yml`, `application-test.yml` and `showcase-capabilities.yml`. If the variant provides
   Prefab Streams, add its backend dependency and set `showcase.streams.skip` to `false`; set
   `showcase.kafka.streams.skip` to `false` only when it also provides `prefab-streams-kafka`. If it can decode Avro
   events, add `prefab-avro` and `prefab-avro-processor` (provided), set `showcase.avro.skip` to `false`, enable
   `prefab.test.schema-registry` in `application-test.yml` and configure `schema.registry.url`.
2. Leave those source-copy properties at their default `true` for variants without Streams or Avro, and list
   `STREAMS` or `AVRO` as an unsupported capability with the relevant backlog task ID.
3. Register the module in the root `pom.xml` and add a `Showcase tests (<technology>)` run configuration in `.run/`.
4. Run `CoverageMatrixTest` and paste the expected table into this file.
