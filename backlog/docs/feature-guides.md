# Feature Guides

**Part of the [Prefab Developer Guide](developer-guide.md)**

---

## Table of Contents

- [7.1 REST CRUD Operations](#71-rest-crud-operations)
- [7.2 Event Publishing](#72-event-publishing)
- [7.3 Event Handling](#73-event-handling)
- [7.4 Avro / AVSC-first Events](#74-avro--avsc-first-events)
- [7.5 Audit Trail](#75-audit-trail)
- [7.6 Multi-tenancy](#76-multi-tenancy)
- [7.7 Binary / File Fields](#77-binary--file-fields)
- [7.8 Async Commit Pattern](#78-async-commit-pattern)
- [7.9 Nested Value Objects and Embedded Types](#79-nested-value-objects-and-embedded-types)
- [7.10 Repository Mixins](#710-repository-mixins)
- [7.11 SSE Streaming](#711-sse-streaming)
- [7.12 Unit Testing Domain Events](#712-unit-testing-domain-events)
- [7.13 Custom PostgreSQL Types with @DbColumn](#713-custom-postgresql-types-with-dbcolumn)
- [7.14 Event Consumer Ordering and Hot-Key Stability](#714-event-consumer-ordering-and-hot-key-stability)
- [7.15 Streams DSL Baseline (Kafka Source/Sink)](#715-streams-dsl-baseline-kafka-sourcesink)
- [7.16 Per-aggregate Plugin Overrides](#716-per-aggregate-plugin-overrides)
- [7.17 Autocomplete Endpoints](#717-autocomplete-endpoints)
- [7.18 Aggregate-bound frontend views](#718-aggregate-bound-frontend-views)
- [7.19 Security and API Documentation](#719-security-and-api-documentation)
- [7.20 Integration Tests: In-memory or Containers](#720-integration-tests-in-memory-or-containers)

---

## 7.1 REST CRUD Operations

A complete CRUD aggregate with all operations:

```java

@Aggregate
@GetById
@GetList
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @NotNull @Size(max = 255) String name,
        @Filter double price,
        @Text String description
) {
    @Create
    public Product(@NotNull String name, double price, @Text String description) {
        this(Reference.create(), 0L, name, price, description);
    }

    @Update
    public Product updateDetails(@NotNull String name, double price) {
        return new Product(id, version, name, price, description);
    }

    @Update(path = "/description")
    public Product updateDescription(String description) {
        return new Product(id, version, name, price, description);
    }

    @Delete
    public void delete() {
    }
}
```

Generated endpoints:

- `POST   /products`             — create
- `GET    /products/{id}`        — get by ID
- `GET    /products?price=9.99`  — paginated list with price filter
- `PUT    /products/{id}`        — update details
- `PUT    /products/{id}/description` — update description
- `DELETE /products/{id}`        — delete

Return a new aggregate from every `@Update` instead of mutating the existing instance. Legacy `void`
updates produce a compiler warning and are scheduled for removal in the next major version. For
`@AsyncCommit`, publish the event and return `this`; the service does not persist the return value.
See the [`@Update` reference](annotation-reference.md#update) for migration details.

To get a stable order (and stable paging), declare a default sort order on `@GetList`:
`@GetList(sorted = @SortOrder(property = "name"))`. It applies only when the request has no `sort` parameter;
an explicit `?sort=price,desc` replaces it entirely. It combines with filters, paging, tenant and parent.

### Nullability annotations

Use JSpecify's `org.jspecify.annotations.Nullable` to mark an optional value:

```java
import org.jspecify.annotations.Nullable;

public record ProductDetails(String name, @Nullable String description) {}
```

Place `@Nullable` on the value itself. `@Nullable List<String>` makes the list optional; `List<@Nullable String>`
allows null elements but does not make the list optional. The same type-use placement is supported on record
components, fields, method return types and parameters, including array references and array components. Only a
nullable field/container affects persistence, REST requiredness, generated mothers and event compatibility; nullable
elements do not make the enclosing container nullable.

Prefab continues to recognize existing annotations whose simple name is `Nullable`, including
`jakarta.annotation.Nullable`, for source compatibility. Migrate imports to JSpecify; generated contracts emit only
JSpecify. A nullable marker keeps its existing optional semantics, including when a legacy and JSpecify marker both
appear; if both nullable and runtime validation annotations are present, nullable metadata still controls generated
optionality and the validation annotation retains its runtime behavior. No removal release for the legacy marker is
currently scheduled. Nullability annotations describe static contracts; they do not perform runtime validation. Keep Jakarta
Validation annotations such as `@NotNull`, `@Valid` and `@Size` for runtime validation, and keep Jakarta lifecycle
annotations such as `@PreDestroy`. No package-wide `@NullMarked` checking is enabled as part of this migration.

---

## 7.2 Event Publishing

Bind an `EventPublisher<E>` once in a `static final` aggregate field, then call its
`publish(event)` method from constructors or update methods. For non-generic events,
use `EventPublisher.bind(OrderCreated.class)`. A bound handle does not require the
aggregate to implement `PublishesEvents`.

The legacy `PublishesEvents.publish(...)`, `PublishesEvents.publishEvent(...)` and
direct `DomainEventPublisher.publish(...)` entry points are deprecated since 0.12.2.
They remain available during migration; deprecation does not prohibit compilation.

When an event subtype extends or implements a supertype annotated with `@Event`, Prefab generates a publisher for the
annotated supertype only. It does not generate one publisher class per concrete subtype.

```java

@Aggregate
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        String customerName
) {
    private static final EventPublisher<OrderCreated> CREATED =
            EventPublisher.bind(OrderCreated.class);

    @Create
    public Order(String customerName) {
        this(Reference.create(), 0L, customerName);
        CREATED.publish(new OrderCreated(id, customerName));
    }
}

@Event(topic = "orders", platform = Event.Platform.KAFKA)
public record OrderCreated(
        @PartitioningKey Reference<Order> id,
        String customerName
) {
}
```

To assert that the correct events are published in a **unit test** (without a Spring context), see
[7.12 Unit Testing Domain Events](#712-unit-testing-domain-events).

For JSON events with a sealed interface or other polymorphic supertype, add Jackson's name-based type metadata
to the contract:

```java
@Event(topic = "accounts")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public sealed interface AccountEvent permits AccountOpened, AccountClosed {
}
```

Consumers declared against `AccountEvent` (including `@TestEventConsumer EventConsumer<AccountEvent>`) need this
metadata to select the concrete subtype during deserialization. Prefab does not add `@JsonTypeInfo` automatically;
without it, publish-side serialization may succeed while consumer deserialization fails.

Binding captures only immutable type information, not a Spring bean or broker connection.
Every publication resolves the currently registered infrastructure publisher, including a
test replacement. Binding before application startup is safe; publishing without a
registered publisher throws `IllegalStateException` instead of dropping the event.
Non-generic payloads are passed through unchanged, preserving their topics and wire format.

The `EventPublisher.bind(EventType<E>)` overload captures concrete generic arguments once:

```java
private static final EventPublisher<Changed<Customer>> CHANGES =
        EventPublisher.bind(new EventType<Changed<Customer>>() {});

// Inside an aggregate method:
CHANGES.publish(new Changed<>(id, updated));
```

`bind(Changed.class)` rejects the raw generic class. Wildcards and unresolved type variables
are rejected by `EventType`; the publisher cannot infer concrete arguments from a payload.
For JSON on Kafka, Pub/Sub and SNS/SQS, repeat `@Event` on the generic declaration to bind each concrete argument
set to its own topics:

```java
@Event(topic = "customer-changes", typeArguments = Customer.class,
        platform = Event.Platform.KAFKA)
@Event(topic = "product-changes", typeArguments = Product.class,
        platform = Event.Platform.KAFKA)
public record Changed<T>(@PartitioningKey String id, T payload) {}
```

Each binding generates a separate typed registry registration, object mother and AsyncAPI
component. Kafka listeners for `Changed<Customer>` and `Changed<Product>` subscribe to
their binding's topics and check the `prefab-event-type` header before invoking a handler,
including when both bindings share a topic. JSON contains the original event, not the
publication envelope; the header supplies the registered concrete Jackson target type.
Unknown identities and identities registered only on another topic are rejected without
loading classes named by message metadata. `PublishTo.ALL` applies only within one binding.

Pub/Sub and SNS/SQS carry the same `prefab-event-type` identity in transport attributes.
Their generated subscribers use `EventType`-based subscription requests and independent,
deterministic subscription/queue names per handler owner, concrete binding and resolved
topic. Generic names use `prefab-` plus a full SHA-256 digest (71 characters, within SQS's
80-character limit); existing non-generic names are unchanged. Messages for another
registered binding on a shared topic are acknowledged/deleted without calling the handler.
Missing or unregistered identities fail explicitly. Pub/Sub's existing `type` attribute
and SNS's subject retain the original payload class, never the `TypedEvent` wrapper.

When configuring subscriptions manually, use
`new SubscriptionRequest<>(topic, subscription, new EventType<Changed<Customer>>() {}, consumer)`
for Pub/Sub or the corresponding `SqsSubscriptionRequest` constructor for SNS/SQS.
Class-based constructors remain available for non-generic events. Bound publication selects
the concrete annotation's platform, even if another parameterisation uses a different broker.

`typeArguments` are positional, concrete non-generic class literals. Incorrect counts,
duplicate bindings, violated bounds, raw arguments and handlers without a declared concrete
binding are compiler errors. Wildcard handlers are not supported in this initial integration.

Code-first generic Avro records have independent schema factories, converters and reader
registrations. Their schema names append a full SHA-256 digest of the canonical binding;
ordinary schema names are unchanged. Converter selection uses the binding registry, not
Spring's erased class lookup. Generic Avro interfaces and `@Avsc` contracts are rejected.
Kafka resolves the writer schema against the binding's reader schema. Pub/Sub and SNS/SQS
support the current concrete Avro schema; migrations there require writer-schema metadata
and remain limited by TASK-275.

Version baselines and migration chains are independent per binding. Declare migrations as
`Migration<ChangedV1<Customer>, Changed<Customer>>`; another parameterisation needs its
own chain. JSON transports and Kafka Avro apply event/component migrations using the
resolved arguments, and reject unsupported versions explicitly. Baseline filenames append
the same binding digest so specialisations cannot overwrite each other's released contract.
For streams, pass the same captured type to `streams.from(type)`, `.to(type)` and
`topology.input(type)` / `topology.output(type)`. Type-preserving operators keep the
binding; after `map`, `join` or a custom processor, call `.as(resultType)` before a
stateful operation that serializes the new value. Use
`.keyBy(Key.class, resultType, mapper)` when mapping and repartitioning together, and
`.aggregate(resultType, initializer, aggregator)` for a generic aggregate result.

```java
var changes = new EventType<Changed<Customer>>() {};
var results = new EventType<Changed<Product>>() {};
streams.from(changes)
        .map(change -> new Changed<>(change.id(), new Product(change.payload().name())))
        .as(results)
        .repartition()
        .to(results);
```

Generic joins, repartition topics, local stores and aggregation state retain the full
payload type rather than inferring arguments from values. `streams.createStore(type)`
and `streams.globalStore(type)` accept concrete bindings. Sources and global-store
updaters skip other registered bindings on shared topics; typed topology outputs reject
a different binding rather than delivering an incorrectly typed value. Kafka permits
only one global store per source topic, and that topic cannot also be a regular source.
Tracing metadata continues through generic map, repartition and join operations.

Configured Kafka, Pub/Sub and SNS/SQS brokers provision all resolved generic topics at
startup, including bindings with no handlers and every fan-out target. This requires
the broker permissions needed to inspect/create those resources. Kafka uses broker
defaults for partition count and replication factor. Existing ordinary topic provisioning
is unchanged. GCP Terraform generates literal generic topics and matching handler
subscriptions; unresolved Spring property/SpEL topics instead produce an explicit
compiler warning and must be managed separately if using Terraform.

---

## 7.3 Event Handling

Three patterns for handling domain events:

### Pattern 1 — Static Handler (Create from event)

```java

@Aggregate
public record ChannelSummary(
        @Id Reference<ChannelSummary> id,
        @Version long version,
        Reference<Channel> channel,
        String name
) {
    @EventHandler
    public static ChannelSummary onChannelCreated(ChannelCreated event) {
        return new ChannelSummary(Reference.create(), 0L, event.reference(), event.name());
    }
}
```

### Pattern 2 — By-Reference Handler (Update via event)

```java

@Aggregate
public record Channel(
        @Id Reference<Channel> id,
        @Version long version,
        String name,
        List<Reference<User>> subscribers
) {
    @EventHandler
    @ByReference(property = "channel")   // event.channel() holds the Reference<Channel>
    public void onUserSubscribed(UserEvent.SubscribedToChannel event) {
        subscribers.add(event.reference());
    }
}
```

### Pattern 3 — Multicast Handler (Broadcast to many)

```java

@Aggregate
public record ChannelSummary(...) {
    @EventHandler
    @Multicast(queryMethod = "findByChannel", parameters = "channel")
    public ChannelSummary onMessageSent(MessageSent event) {
        return new ChannelSummary(id, version, channel, name, totalMessages + 1, totalSubscribers);
    }
}

// Repository mixin required for the query method:
@RepositoryMixin(ChannelSummary.class)
public interface ChannelSummaryRepositoryMixin {
    List<ChannelSummary> findByChannel(Reference<Channel> channel);
}
```

By default, an empty query throws to trigger retry and eventually dead-letter handling, unless a matching static
`@EventHandler` exists to create the aggregate. For a multicast where no targets is a normal outcome, set
`@Multicast(failWhenEmpty = false)`; the generated handler returns without saving. A static companion continues to
take precedence and creates the aggregate for an empty query.

### Create-or-Update Pattern

Combine a static `@EventHandler` with a `@ByReference` or `@Multicast` handler for the same event type.
The shared event type may be a concrete event record or an interface / sealed supertype contract that
declares the accessors the handlers need:

```java

public sealed interface MessageEvent permits MessageSent {
    Reference<ChannelSummary> summary();
}

@EventHandler
public static ChannelSummary onCreate(MessageEvent event) {
    return new ChannelSummary(event.summary(), 1);
}

@EventHandler
@ByReference(property = "summary")
public void onUpdate(MessageEvent event) {
    // updates existing order
}
```

When the `@ByReference` handler finds no aggregate, the static handler creates it.

---

## 7.4 Avro / AVSC-first Events

### Inline Avro (Java-first)

Define event records with `@Event(serialization = AVRO)`. The processor generates Avro schema and
converters automatically.

When multiple AVRO events share an `@Event`-annotated supertype, Prefab also generates a
`GenericRecord -> Supertype` converter. It dispatches by Avro schema name to the concrete subtype
converter so handlers typed to the supertype can deserialize all subtype payloads.

```java

@Aggregate
public record Customer(
        @Id Reference<Customer> id,
        @Version long version,
        String name
) implements PublishesEvents {

    @Create
    public Customer(String name) {
        this(Reference.create(), 0L, name);
        publish(new Created(id, name));
    }

    @Event(topic = "customer", serialization = Event.Serialization.AVRO)
    public sealed interface Events permits Created {
        @PartitioningKey
        Reference<Customer> id();
    }

    public record Created(Reference<Customer> id, String name) implements Events {
    }
}
```

The shop showcase shows both styles on Kafka with a schema registry: `PaymentEvent` is code-first, `ShipmentEvent` is
AVSC-first (`examples/showcase/src/avro/java`, tests in `examples/showcase/src/avro-test/java`).

### AVSC-first

Place `.avsc` files on the classpath and use `@Avsc` + `@Event`:

```java
// src/main/resources/avro/sale-created.avsc
// { "type": "record", "name": "SaleCreated", "namespace": "be.example.sale", "fields": [...] }

@Event(topic = "sale", serialization = Event.Serialization.AVRO)
@Avsc({"avro/sale-created.avsc", "avro/sale-paid.avsc"})
public sealed interface SaleEvent permits SaleCreated, SalePaid {
    @PartitioningKey
    String saleId();
}
```

An `@Avsc` event contract must be a `sealed interface` and must list every generated event type in its
`permits` clause. This is required for runtime schema registration and dispatch; a non-sealed contract may compile
but fails during startup with `Only sealed interfaces are supported for event types`.

The processor generates `SaleCreated` and `SalePaid` records in the same Java package as `SaleEvent`.
Generated Java type names are always capitalised. When the original Avro name starts with a lowercase
letter (e.g. `saleCreated`), the generated record is capitalised (`SaleCreated`) and annotated with
`@AvroSchema(name = "saleCreated")` so schema factories can recover the original Avro name during
serialisation.

When individual generated AVSC records or enums should implement extra Java interfaces, declare
repeatable `@AvscInterface` mappings on the top-level `@Avsc` contract and identify targets by the
original Avro `namespace` + `name`:

```java
@Event(topic = "sale", serialization = Event.Serialization.AVRO)
@Avsc({"avro/sale-created.avsc", "avro/sale-paid.avsc"})
@AvscInterface(type = LifecycleEvent.class, namespace = "be.example.sale", name = "saleCreated")
@AvscInterface(type = LifecycleEvent.class, namespace = "be.example.sale", name = "salePaid")
@AvscInterface(type = SaleStatus.class, namespace = "be.example.sale", name = "saleStatus")
public sealed interface SaleEvent permits SaleCreated, SalePaid {}

interface LifecycleEvent {
    String saleId();
}

interface SaleStatus {}
```

This keeps the mapping stable even when Prefab capitalises Java type names or when the AVSC namespace
differs from the Java package. Multiple generated records or enums can share the same interface by
repeating `@AvscInterface` with different Avro type identifiers.

At runtime, generated schema factories validate their in-memory schema against the referenced AVSC
files. If a generated schema is not compatible with the AVSC contract, schema factory initialization
fails fast with an explicit exception.

When all referenced AVSC events share the same partitioning key, prefer `@Avsc({...})` and declare a
shared `@PartitioningKey` method on the contract. That method can either match a physical schema field
or be a derived default method:

```java
@Event(topic = "sale", serialization = Event.Serialization.AVRO)
@Avsc({"avro/sale-created.avsc", "avro/sale-paid.avsc"})
public sealed interface SaleEvent permits SaleCreated, SalePaid {
    String tenantId();
    String saleId();

    @PartitioningKey
    default String tenantSaleKey() {
        return tenantId() + ":" + saleId();
    }
}
```

For synthetic keys, every referenced schema must still expose the shared accessor methods used by the
default implementation (for example `tenantId` and `saleId` above). Prefab uses the shared method for
all generated event registrars.

Use `files = @AvscFile(...)` when different AVSC files need different partitioning-key properties, or
when only some of the generated events should register a partitioning-key extractor. In that case,
`keyProperty` must match a top-level field name in the referenced schema.

For nullable Avro fields, Prefab treats JSpecify `@Nullable` and legacy annotations named `Nullable` as optional
markers and generates Avro unions as `["null", T]` with a `null` default.

### Avro union fields

When an AVSC field declares a multi-branch union — for example `["double","string"]`,
`["RecordA","RecordB"]`, or `["null","RecordA","RecordB"]` — Prefab generates a **type-safe sealed
interface** instead of an untyped field.

**Scalar union** — `["double","string"]` on field `exactValue`:

- `ExactValue` — sealed interface
- `ExactValueDouble(double value)` — implements `ExactValue`
- `ExactValueString(String value)` — implements `ExactValue`

**Record union** — `["TextPayload","NumericPayload"]` on field `payload`:

- `Payload` — sealed interface
- `PayloadTextPayload(TextPayload value)` — implements `Payload`
- `PayloadNumericPayload(NumericPayload value)` — implements `Payload`
- `TextPayload` and `NumericPayload` records are generated separately with their own converters and schema factories.

**Enum union** — `["AlertLevel","string"]` on field `status`:

- `Status` — sealed interface
- `StatusAlertLevel(AlertLevel value)` — implements `Status`
- `StatusString(String value)` — implements `Status`

**Array branch union** — `["string",{"type":"array","items":"string"}]` on field `tags`:

- `Tags` — sealed interface
- `TagsString(String value)` — implements `Tags`
- `TagsStringList(List<String> value)` — implements `Tags`

**Branch naming**: `{CapitalizedFieldName}{Suffix}` where suffix is `Double`, `String`, `Int`, `Long`,
`Float`, `Boolean`, the record or enum simple name, or `{ElementType}List` for array branches.

**Nullable multi-branch unions**: For `["null","RecordA","RecordB"]`, the field is annotated `@Nullable`
and the sealed interface is generated for the non-null branches only.

**Generated artefacts per union**:

- One sealed interface (schema factory only — no standalone converter bean)
- One wrapper record per branch
- For record branches: full converters and schema factory are generated for each branch record type

**Converter behaviour**: serialisation and deserialisation logic is inlined into the parent record's
converter via a `switch` expression over the sealed interface's permitted subtypes.

---

## 7.5 Audit Trail

Add the four audit fields individually or use `AuditInfo` as a convenience wrapper:

```java

@Aggregate
@GetById
public record Contract(
        @Id Reference<Contract> id,
        @Version long version,
        String title,
        AuditInfo audit
) {
    @Create
    public Contract(String title) {
        this(Reference.create(), 0L, title, new AuditInfo());
    }

    @Update
    public Contract update(String title) {
        return new Contract(id, version, title, audit);
    }
}
```

The generated service:

- On create: sets `createdAt`, `createdBy`, `lastModifiedAt`, `lastModifiedBy`
- On update: sets only `lastModifiedAt` and `lastModifiedBy`

Override `AuditContextProvider` to provide the current user from your security context.

---

## 7.6 Multi-tenancy

```java

@Aggregate
@GetList
@GetById
public record Project(
        @Id Reference<Project> id,
        @Version long version,
        @TenantId String organisationId,
        String name
) {
    @Create
    public Project(String name) {
        this(Reference.create(), 0L, null, name);  // null: filled by generated service
    }
}
```

Implement `TenantContextProvider` as a Spring bean to supply the tenant ID:

```java

@Component
public class JwtTenantContextProvider implements TenantContextProvider {
    @Override
    public String currentTenantId() {
        return extractOrgIdFromJwt();
    }
}
```

---

## 7.7 Binary / File Fields

```java

@Aggregate
@GetById
public record Document(
        @Id Reference<Document> id,
        @Version long version,
        String title,
        @Download Binary file
) {
    @Create
    public Document(
            String title,
            @ContentType({"application/pdf", "application/msword"})
            @FileSize(max = 10 * 1024 * 1024) Binary file
    ) {
        this(Reference.create(), 0L, title, file);
    }
}
```

Generated endpoints:

- `POST /documents` — `multipart/form-data` with a JSON part `body` holding the non-binary fields and a file part
  named after the `Binary` parameter (`file`)
- `GET  /documents/{id}/file` — download the binary

Example request:

```bash
curl -X POST http://localhost:8080/documents \
  -F 'body={"title": "Contract"};type=application/json' \
  -F 'file=@contract.pdf;type=application/pdf'
```

Put `@ContentType` and `@FileSize` on the `@Create` constructor or `@Update` method parameter that receives the
file. An upload that violates them, a missing part, or a malformed multipart request is rejected with
`400 Bad Request`, and nothing is stored. See [Generated Artefacts, 6.1](generated-artefacts.md#61-controller).

`StorageService` (from `prefab-core`) handles actual file storage. Configure a backend (local, S3, GCS) via
Spring Boot auto-configuration.

---

## 7.8 Async Commit Pattern

Use when you need at-least-once delivery semantics for aggregate creation via an event broker.

The `@Create @AsyncCommit` static factory method must have a **`void` return type** and publish
the event explicitly using `PublishesEvents.publishEvent(event)`. If a non-void return type is used,
the annotation processor emits a compile-time error — the generated service discards any return value,
so the event would never be published.

```java

@Aggregate
@AsyncCommit
@GetById
public record Order(
        @Id Reference<Order> id,
        String customerId,
        String status
) {
    // REST call publishes event and returns 202 Accepted
    @Create
    public static void create(@NotNull String customerId) {
        PublishesEvents.publishEvent(new OrderPlaced(Reference.create(), customerId));
    }

    // Aggregate is persisted when event arrives
    @EventHandler
    public static Order onOrderPlaced(OrderPlaced event) {
        return new Order(event.id(), event.customerId(), "PLACED");
    }
}

@Event(topic = "orders")
public record OrderPlaced(
        @PartitioningKey Reference<Order> id,
        String customerId
) {
}
```

---

## 7.9 Nested Value Objects and Embedded Types

Prefab supports nested Java records as value objects embedded in the aggregate:

```java
public record ProductDetails(
        String brand,
        String model,
        String colour
) {
}

@Aggregate
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        String name,
        ProductDetails details    // embedded value object
) {
    @Create
    public Product(String name, ProductDetails details) {
        this(Reference.create(), 0L, name, details);
    }
}
```

**Database mapping (PostgreSQL):** Flattened into the same table with prefixed columns:
`details_brand`, `details_model`, `details_colour`.

**REST mapping:** The `CreateProductRequest` includes a nested `ProductDetailsRequest` (or inline fields,
depending on the nesting level).

---

## 7.10 Repository Mixins

Add custom query methods to the generated repository:

```java

@RepositoryMixin(ChannelSummary.class)
public interface ChannelSummaryRepositoryMixin {
    List<ChannelSummary> findByChannel(Reference<Channel> channel);
}
```

Prefab adds `ChannelSummaryRepositoryMixin` as a super-interface of the generated
`ChannelSummaryRepository`. Spring Data derives the query from the method name, or you can use
`@Query` for complex SQL:

```java

@RepositoryMixin(UserStatus.class)
public interface UserStatusRepositoryMixin {
    @Query("""
            SELECT *
            FROM user_status
            WHERE "user" IN (
                SELECT id FROM "user"
                WHERE EXISTS (
                    SELECT 1 FROM UNNEST(channel_subscriptions) AS cs
                    WHERE cs = :channel
                )
            )
            """)
    List<UserStatus> findUserStatusesInChannel(Reference<Channel> channel);
}
```

---

## 7.11 SSE Streaming

Prefab generates Server-Sent Events (SSE) endpoints for aggregate methods annotated with `@Streaming`.
Two delivery models are supported.

### Pull model — blocking `Stream<T>` or reactive `Flux<T>`

Use this when data comes from an in-process source such as a blocking iterator, subprocess stdout, or
a reactive stream.

```java

@Aggregate
@GetById
public record Session(
        @Id String id,
        @Version long version,
        String title) {

    @Streaming(path = "/stream", event = "token", heartbeatSeconds = 15)
    public java.util.stream.Stream<String> streamTokens() {
        return TokenRegistry.streamFor(id);  // blocking source
    }
}
```

Prefab generates:

- `GET /sessions/{id}/stream` → `text/event-stream` response using `SseEmitter`
- A virtual thread (`Thread.ofVirtual()`) consumes the `Stream<T>` and sends each element as an SSE
  frame with the configured `event` name.
- A heartbeat virtual thread sends `event: ping\ndata: {}\n\n` every `heartbeatSeconds` seconds.
- On disconnect (emitter completion / timeout) the stream is closed.

For `Flux<T>` return types, `Flux.subscribe()` is used instead of a virtual thread.

### Push model — `@EventHandler @ByReference @Streaming`

Use this when data arrives via a Kafka (or other messaging) event and should be forwarded to a
connected SSE client.

```java

@Event(topic = "${topics.tokens.name}")
public record TokenEmitted(
        @PartitioningKey Reference<Session> sessionId,
        String token,
        boolean done) {
}

@Aggregate
@GetById
public record Session(
        @Id String id,
        @Version long version,
        String title) {

    @EventHandler
    @ByReference(property = "sessionId")
    @Streaming(path = "/stream", event = "token", terminal = "done")
    public Session onTokenEmitted(TokenEmitted event) {
        return this;
    }
}
```

Prefab generates:

- `GET /sessions/{id}/stream` SSE connect endpoint — registers an `SseEmitter` in
  `SessionSseRegistry`, sets up `onCompletion`/`onTimeout` cleanup, and starts the heartbeat.
- `SessionSseRegistry` — a `@Component` backed by a `ConcurrentHashMap<String, SseEmitter>`.
- An augmented `onTokenEmitted` service method that:
    1. Applies the `@ByReference` update and saves the aggregate (standard event-handler logic).
    2. Pushes `event.token()` (field matching `event = "token"`) as the SSE data payload.
    3. Calls `emitter.complete()` when `event.done() == true` (field matching `terminal = "done"`),
       cleanly closing the SSE connection.

### Choosing the right model

| Scenario                                                      | Recommended model                                  |
|---------------------------------------------------------------|----------------------------------------------------|
| Data from a Kafka event already in the Prefab event flow      | **Push** (`@EventHandler @ByReference @Streaming`) |
| Data from a blocking in-process source (iterator, subprocess) | **Pull** (method returning `Stream<T>`)            |
| Data from a reactive source (Project Reactor)                 | **Pull** (method returning `Flux<T>`)              |

---

## 7.12 Unit Testing Domain Events

Aggregate roots publish through bound `EventPublisher` handles inside constructors or update
methods. In a plain JUnit 5 unit test there is no Spring context: install the
`PublishedEventsExtension` below to provide a capturing publisher. A bound handle throws
when no publisher is registered; only the deprecated `PublishesEvents` helper retains its
legacy no-op behavior. `prefab-test` provides two classes to make unit-testing event publishing
easy and safe.

### `CapturingDomainEventPublisher`

**Package:** `be.appify.prefab.test.domain`

A `DomainEventPublisher` implementation that records every published event in an in-memory list
instead of forwarding it to a message broker.

| Method                        | Description                                                                       |
|-------------------------------|-----------------------------------------------------------------------------------|
| `publishedEvents()`           | Returns an unmodifiable list of all captured events.                              |
| `publishedEventsOf(Class<T>)` | Returns a filtered list of events of the given type.                              |
| `clear()`                     | Clears the captured list (useful when arranging multiple sub-phases in one test). |

### `PublishedEventsExtension`

**Package:** `be.appify.prefab.test.domain`

A JUnit 5 extension that installs a fresh `CapturingDomainEventPublisher` before each test and
resets the static publisher to `null` after each test. This guarantees that no state bleeds
between tests.

Annotate your test class with `@ExtendWith(PublishedEventsExtension.class)` and declare a
`CapturingDomainEventPublisher` parameter on any test method to receive the publisher directly.

```java

@ExtendWith(PublishedEventsExtension.class)
class OrderTest {

    @Test
    void createOrder_publishesOrderCreatedEvent(CapturingDomainEventPublisher publisher) {
        // Arrange & Act
        new Order("Alice");

        // Assert
        assertThat(publisher.publishedEventsOf(OrderCreated.class))
                .singleElement()
                .extracting(OrderCreated::customerName)
                .isEqualTo("Alice");
    }

    @Test
    void noCreation_noEventsPublished(CapturingDomainEventPublisher publisher) {
        assertThat(publisher.publishedEvents()).isEmpty();
    }
}
```

### How it works internally

`DomainEventPublisher` exposes two static methods used exclusively by framework and test infrastructure:

| Method                                        | Usage                                                                                                                 |
|-----------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `DomainEventPublisher.setInstance(publisher)` | Called by `SpringDomainEventPublisher` on startup and by `PublishedEventsExtension` before each test.                 |
| `DomainEventPublisher.reset()`                | Called by `SpringDomainEventPublisher` on shutdown (`@PreDestroy`) and by `PublishedEventsExtension` after each test. |

> **Dependency:** `prefab-test` is already on the test classpath for any project generated by
> Prefab. No additional dependency is needed.

---

## 7.13 Custom PostgreSQL Types with @DbColumn

Use `@DbColumn` when an aggregate field's Java type is not in Prefab's built-in set — for example,
`float[]` for pgvector embeddings, PostGIS geometry, or `hstore`.

### Cookbook: pgvector `vector(N)` Field

**1. Add `pgvector` dependency:**

```xml

<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
    <version>0.1.6</version>
</dependency>
```

**2. Write the converter:**

```java
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.postgresql.util.PGobject;

@WritingConverter
public class FloatArrayToVectorConverter implements Converter<float[], PGobject> {
    @Override
    public PGobject convert(float[] source) {
        try {
            var sb = new StringBuilder("[");
            for (int i = 0; i < source.length; i++) {
                if (i > 0)
                    sb.append(',');
                sb.append(source[i]);
            }
            sb.append(']');
            var pgObject = new PGobject();
            pgObject.setType("vector");
            pgObject.setValue(sb.toString());
            return pgObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

**3. Declare the aggregate field:**

```java

@Aggregate
@GetById
@GetList
public record MemoryEntry(
        @Id Reference<MemoryEntry> id,
        @Version long version,
        @Text String content,
        @DbColumn(type = "vector(1536)", converter = FloatArrayToVectorConverter.class)
        float[] embedding
) {
    @Create
    public MemoryEntry(String content, float[] embedding) {
        this(Reference.create(), 0L, content, embedding);
    }
}
```

**What Prefab generates:**

- The generated migration includes `"embedding" vector(1536) NOT NULL`.
- `FloatArrayToVectorConverter` is auto-registered with `JdbcCustomConversions` — no `@Component` needed.
- The REST API serialises `float[]` as a JSON array.

**4. Flyway migration (auto-generated):**

```sql
CREATE TABLE "memory_entry"
(
    "id"        VARCHAR(255) NOT NULL,
    "version"   BIGINT       NOT NULL,
    "content"   TEXT         NOT NULL,
    "embedding" vector(1536) NOT NULL,
    PRIMARY KEY ("id")
);
```

### Supported array types

`@DbColumn` works on any Java field type that `TypeManifest` can represent, including:

| Java type     | Suggested `@DbColumn(type = ...)` |
|---------------|-----------------------------------|
| `float[]`     | `vector(N)`, `real[]`             |
| `Float[]`     | `vector(N)`, `real[]`             |
| `byte[]`      | `bytea`                           |
| Custom record | Any PostgreSQL type/extension     |

### Notes

- `@DbColumn.type()` must not be blank — a compile-time error is reported if it is.
- When `converter()` is `void.class` (the default), no converter is registered; handle JDBC mapping via a `@Component`
  -annotated converter or a `PrefabPlugin`.
- The annotation targets both `ElementType.FIELD` and `ElementType.RECORD_COMPONENT`.

---

## 7.14 Event Consumer Ordering and Hot-Key Stability

When multiple events can update the same aggregate row (a "hot key"), parallel event handlers can produce
`OptimisticLockingFailureException` retry noise and, under load, retry exhaustion.

Ordering can reduce races, but it does not replace idempotent handlers: every broker here can redeliver, and ordering is
scoped to one key/partition/group rather than global. Prefer deterministic IDs in create-or-update projections, and
design handlers so retries and duplicate deliveries do not cause incorrect state or side effects.

### Platform guidance

- **Kafka**: `@PartitioningKey` is the record key, so records with the same key go to the same partition and are read in
  partition order. Kafka does not serialize independent handlers or consumers that update the same database row; use
  `@EventHandlerConfig(concurrency = "1")` when those updates must not race.
- **Pub/Sub**: `@PartitioningKey` is the ordering key, and Prefab enables message ordering on generated subscriptions.
  Ordering is per key and is subject to Pub/Sub's ordering-key requirements and at-least-once redelivery behavior.
- **SNS/SQS**: events with a registered `@PartitioningKey` automatically use SNS FIFO topics and SQS FIFO queues
  (both provisioned with the required `.fifo` suffix). SNS receives the key as `MessageGroupId`; the SQS listener
  processes messages sequentially, preserving delivery order for that group. Naming a topic with `.fifo` explicitly
  also provisions FIFO resources; without a key, those messages share the `prefab-default` group. Events without a key
  or `.fifo` topic keep using standard resources. A declared partitioning key must be non-blank and at most 128
  characters. FIFO ordering is not global and has AWS throughput limits; Prefab's current single polling worker per
  queue also serializes different groups. Retries preserve order while a message remains queued, but moving a failed
  message to a dead-letter queue can let later messages proceed.

### Example

```java

@Aggregate
@EventHandlerConfig(concurrency = "1")
public record ChannelSummary(
        @Id Reference<ChannelSummary> id,
        @Version long version,
        Reference<Channel> channel,
        String name,
        int totalMessages,
        int totalSubscribers
) {
    private static Reference<ChannelSummary> summaryId(Reference<Channel> channel) {
        return Reference.fromId("channel-summary-" + channel.id());
    }

    @EventHandler
    public static ChannelSummary onMessageSent(MessageSent event) {
        return new ChannelSummary(summaryId(event.channel()), 0L, event.channel(), "<pending>", 1, 0);
    }

    @EventHandler
    @Multicast(queryMethod = "findByChannel", parameters = "channel")
    public ChannelSummary applyMessageSent(MessageSent event) {
        return new ChannelSummary(id, version, channel, name, totalMessages + 1, totalSubscribers);
    }
}
```

---

## 7.15 Streams DSL Baseline (Kafka Source/Sink)

Prefab provides a backend-neutral Streams DSL (`prefab-streams`) with Kafka and Pub/Sub backends:

| Artifact               | Contains                                                                                                   | Depends on Kafka? |
|------------------------|------------------------------------------------------------------------------------------------------------|-------------------|
| `prefab-streams`       | The DSL (`PrefabStreams`, `PrefabStream`, `StreamDefinition`, `Store`, ...) and the backend-neutral test API (`be.appify.prefab.streams.testing`) | No                |
| `prefab-streams-engine` | In-process operator engine, source/sink/state SPIs and in-memory topology test harness                     | No                |
| `prefab-streams-postgres` | Postgres `StreamStateRepository` (state, inbox, outbox, relay lease) for the in-process engine | No                |
| `prefab-streams-mongodb` | MongoDB `StreamStateRepository` (state, inbox, outbox, relay lease) for the in-process engine; needs a replica set | No                |
| `prefab-streams-pubsub` | Pub/Sub adapter and topology-test factory for the in-process engine | No |
| `prefab-streams-sns-sqs` | SNS/SQS source and sink, `StreamBackend` `sns-sqs` and auto-configuration for the in-process engine | No                |
| `prefab-streams-kafka` | `KafkaPrefabStreams`, serdes, `KafkaStreamBreakoutAdapter`, the Kafka test harness and the `StreamsConfiguration` auto-configuration | Yes               |

Add `prefab-streams-kafka` to run the DSL on Kafka Streams; it brings `prefab-streams` along. Application code that
only declares topologies compiles against `prefab-streams` and does not import any Kafka type. Broker backends are
separate artifacts that implement the same DSL.

```xml
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-streams-kafka</artifactId>
</dependency>
```

**Migrating from earlier versions.** Existing applications that depend on `prefab-streams` must add
`prefab-streams-kafka`; without it the DSL has no backend, no `StreamsConfiguration` is activated and
`TopologyTestHarness.create(...)` fails with `No TopologyTestHarness.Factory found on the classpath`. The Kafka
classes (`be.appify.prefab.streams.kafka.*`, `...kafka.testing.*`) keep their package and name, so no source changes
are needed apart from one: `StreamBackend` is no longer an enum. Use `KafkaStreamBackend.KAFKA` instead of
`StreamBackend.KAFKA`, and `StreamBackend.named("my-backend")` instead of `StreamBackend.CUSTOM` in a custom
`StreamBreakoutAdapter`. State store names, topics and serialised bytes are unchanged.

Supported operations:

- `from(Class<?>)`
- `filter(Predicate<?>)`
- `map(Function<?, ?>)`
- `flatMap(Function<?, Iterable<?>>)`
- `branch(Predicate<?>)`
- `branch(Class<S>)`
- `join(PrefabStream<VO>, JoinWindow, BiFunction<V, VO, VR>)`
- `merge(PrefabStream<? extends V>)`
- `PrefabStreams.merge(PrefabStream<? extends M>, PrefabStream<? extends M>)`
- `deduplicate(Function<?, String>, Duration)`
- `breakout(StreamBreakoutAdapter<?, ?, ?, ?>)`
- `globalStore(Class<?>)`, `globalStore(String, Class<?>)`, `globalStore(Class<S>, Class<V>, Function<S, V>)`, `globalStoreFrom(Class<S>).updatedBy(...).as(Class<V>)`
- `to(Class<?>)`
- `to(String)`
- `end()`

The shop showcase demonstrates the operators together on its order events: category revenue aggregation and hourly
windows are in `examples/showcase/src/streams/java`, with backend-neutral topology tests in
`examples/showcase/src/streams-test/java`. Its Kafka-native breakout example is kept in a separate optional source
root (`src/streams-kafka/java`) and guarded by the `KAFKA_STREAMS_BREAKOUT` capability; variants that provide another
Streams backend can compile and run the common topology slice without the Kafka adapter.

Serialization and deserialization reuse the existing Kafka dynamic serde infrastructure:

- `DynamicDeserializer` for source records
- `DynamicSerializer` for sink records

Default key serialization in the Streams DSL is now shape-based:

- **Single-field record keys** (for example `record OrderId(String value)`) are encoded as plain UTF-8 strings.
- **Composite record keys** (records with 2+ components) are encoded as JSON.
- Non-record keys also use JSON by default.

This is a **breaking change** for persisted Kafka state/changelog key bytes for topologies that previously relied on
`Key.toString()`/`Key.parse(...)` semantics. Existing state stores and changelog topics may need reset or migration when
moving to this version.

When Prefab assigns Kafka Streams processor names for DSL-owned steps, it uses deterministic,
representative names that encode the operation type and the simple class names of the value
types involved, converted to kebab-case.  This keeps identical DSL topologies stable across
runs while avoiding name collisions inside one topology.

| DSL operator          | Processor name pattern                                | Example                                  |
|-----------------------|-------------------------------------------------------|------------------------------------------|
| `filter`              | `filter-{input-type}`                                 | `filter-incoming-order`                  |
| `map`                 | `map-{input-type}`                                    | `map-incoming-order`                     |
| `flatMap`             | `flat-map-{input-type}`                               | `flat-map-word-batch`                    |
| `branch(Predicate)`   | `branch-{input-type}` / `branch-{input-type}-matched` | `branch-incoming-order-matched`          |
| `branch(Class<S>)`    | `branch-subtype-{type}` / `branch-subtype-{type}-cast`| `branch-subtype-order-created-cast`      |
| `merge`               | `merge-{left-type}` or `merge-{left-type}-{right-type}`| `merge-incoming-order`                  |
| `join`                | `join-{left-type}-{right-type}`                       | `join-incoming-order-shipping-update`    |
| `process`             | `process-{input-type}`                                | `process-incoming-order`                 |

When the same operator+type combination appears more than once in the same topology a numeric
suffix is appended starting at `-2` (e.g. `filter-incoming-order-2`).  When the value type is
not statically known — typically after `map`, `flatMap`, or `breakout` — the type segment is
omitted and only the operator prefix is used (e.g. `map`).

`breakout` delegates to user-supplied native Kafka code and keeps whatever name the caller assigns.

`from(Class<?>)` and `to(Class<?>)` resolve topic names via registered Kafka event types and fail fast:

- Throws `IllegalArgumentException` when no topic is registered for a class
- Throws `IllegalStateException` when multiple topics are registered for a class

Use instance `merge(...)` when the current stream type already represents the target type. Use
`PrefabStreams.merge(...)` when sibling streams should be widened into a declared common supertype.

Use `join(...)` for KStream-KStream **inner join** composition with explicit windowing via `JoinWindow`.

### Pub/Sub streams (`prefab-streams-pubsub`)

Add `prefab-streams-pubsub` and `prefab-streams-postgres` alongside the application's Pub/Sub and JDBC
configuration. The backend activates when `PubSubUtil` and a `StreamStateRepository` are available, unless a
`PrefabStreams` bean already exists. The Postgres schema and initialization property are described below.
Use `PubSubStreamBackend.PUBSUB` and `TopologyTestExtension.withBackend("pubsub")` to run backend-neutral topology
tests with the in-memory engine; those tests do not exercise the broker or Postgres.

Each `from` declaration has its own ordered subscription named
`{spring.application.name}.streams.{source-name}`. Branches of the same declaration share it. Source identities
are deterministic for a fixed topology; all instances must declare identical sources in the same order. Changing
source declarations can create a new subscription and inbox namespace, so plan such changes as a topology migration.
Global stores have one shared subscription per store and **do not contain history published before that subscription
was created**. A tombstone has an empty body, a key, and the attribute `prefab-tombstone=true`.

The engine commits state, inbox and outbox together before acknowledging input; input redelivery is skipped while
its inbox identity is retained. The outbox may publish again after a crash between broker acceptance and outbox
completion, so downstream consumers need message-ID deduplication. External side effects are not covered. The
backend writes `prefab-message-id`, `prefab-event-time`, and bounded `prefab-correlation-ids` attributes; the
oldest correlation IDs are removed until the encoded value fits 1024 UTF-8 bytes. Missing event time falls back to
Pub/Sub publish time. Window stream time is tracked per key, unlike Kafka's per-partition stream time.

Outputs use record keys as Pub/Sub ordering keys, and the supplied Spring publisher customizer enables ordering.
Custom `PublisherFactory` implementations must also enable ordering. Ordering is per key, and order-dependent
operators require ordered input. Logical repartitioning does not create broker topics. `breakout` and
`StreamProcessorContext.schedule` are unsupported on this backend and fail at startup; scheduling is tracked by
TASK-363. A `to(String)` output registers its topic as a serialization alias without changing the event's default
publication topics.

### Streams on SNS/SQS (`prefab-streams-sns-sqs`)

`prefab-streams-sns-sqs` runs the Streams DSL on AWS SNS/SQS (backend `sns-sqs`, `SnsSqsStreamBackend.SNS_SQS`). It is
a thin adapter on the in-process engine (decision-008): sources and sinks for SNS/SQS, with state, inbox and outbox
in your database. Add it together with `prefab-sns-sqs` and a state repository such as `prefab-streams-postgres` or
`prefab-streams-mongodb`:

```xml
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-streams-sns-sqs</artifactId>
</dependency>
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-streams-postgres</artifactId>
</dependency>
```

Auto-configuration activates when an `SqsUtil` and a `StreamStateRepository` bean exist. It provides `PrefabStreams`
(an `InProcessPrefabStreams`), starts the topology after all `StreamDefinition` beans were created and stops it when
the context closes. Topologies are declared exactly as for Kafka, so a `@Bean StreamDefinition` written against
`prefab-streams` runs unchanged.

**What happens per message.** One database transaction per message: inbox check, operators, state writes, and outputs
to the outbox. Then the transaction commits, the message is deleted from the SQS queue and the relay publishes the
outbox rows to SNS. A message that fails is not deleted. SQS delivers it again after the visibility timeout and, after
`prefab.dlt.retries.limit` receives, moves it to the dead-letter queue, as for event handlers. A redelivered message
(same `prefab-message-id`, or the same SQS message ID) is recognised by the inbox and changes neither state nor
output.

| Source / sink | Behaviour |
|---------------|-----------|
| `from(..)` | One SQS queue per `from` declaration, `{spring.application.name}-streams-{source-name}` (sanitized to the characters SQS allows, shortened with a hash beyond 75 characters), subscribed to the source topic with the existing dead-letter redrive policy. All instances share the queue. Source names are deterministic for a fixed topology, so all instances must declare the same sources in the same order; changing the declarations creates new queues and a new inbox namespace. |
| Global store | One queue for the store, shared by all instances, feeding the shared table. A tombstone is a message with the attributes `prefab-tombstone=true` and `prefab-key`, and the placeholder body `null` because SNS refuses an empty message. |
| `to(Class)`, `to(EventType)`, `to(String)` | Published to SNS by the outbox relay. `to(String)` creates the topic. |
| FIFO | A source is FIFO when its topic ends in `.fifo` or its event declares a `@PartitioningKey`. Topic, queue and dead-letter queue are then FIFO. On a FIFO topic the sink sets `MessageGroupId` to the record key (hashed beyond 128 characters) and `MessageDeduplicationId` to `prefab-message-id`. |
| Event time | The attribute `prefab-event-time` (ISO-8601), otherwise the SQS `SentTimestamp`. |
| Attributes | The sink writes three of its own, `prefab-message-id`, `prefab-event-time` and `prefab-correlation-ids`, plus the event type and version attributes of the event. SQS delivers at most ten attributes. Tombstones add `prefab-tombstone` and `prefab-key`. |

| Property | Default | Description |
|----------|---------|-------------|
| `prefab.streams.sns-sqs.visibility-timeout` | `2m` | Visibility timeout of the source queues. It must exceed the time to process one message, including the engine's conflict retries and the retries of the dead-letter policy; otherwise SQS delivers a message again while it is still being processed. |
| `prefab.streams.inbox-retention` | `7d` | How long processed message IDs are remembered for deduplication. |
| `prefab.streams.tracing.enabled`, `prefab.streams.tracing.max-correlation-ids` | `false`, `100` | Message and correlation IDs, as on Kafka. |
| `prefab.dlt.*` | | Dead-letter queue name and retry policy, shared with event handlers. |

**Operator support.** Everything in the engine matrix of decision-008 runs: `from`, `to`, `as`, `end`, `filter`,
`map`, `flatMap`, `branch`, `merge`, `process` (also with stores), `aggregate`, `keyBy`/`repartition` (logical, no
topics), `groupBy` with `aggregate` and `windowedAggregate` (with grace), inner `join` with `JoinWindow`,
`deduplicate` and global stores. `breakout` and `StreamProcessorContext.schedule` fail at startup (TASK-363 for `schedule`); keep a Kafka-native `breakout` topology in a configuration that
only loads with the Kafka backend.

**Differences from Kafka that you must know about.**

- *Ordering needs FIFO.* Per-key order comes from FIFO message groups. Without FIFO, state stays correct thanks to the
  optimistic versions in the database, but an order-dependent aggregator can give a different result, and the engine
  logs a startup warning naming the source and the operator. Declare `@PartitioningKey` on the events of such sources.
- *FIFO limits.* FIFO topics and queues have throughput quotas per message group and per queue. A failing message
  blocks its message group until it moves to the dead-letter queue.
- *Stream time per key.* Windows and joins close per key, not per partition, so a record that Kafka drops because
  another key advanced stream time is accepted here.
- *Global stores start empty.* SNS/SQS cannot replay a topic, so a global store only holds the events published after
  its queue was created.
- *`breakout` and wall-clock punctuation are not available.*

**Testing.** `TopologyTestHarness` finds the backend by name: `TopologyTestExtension.withBackend("sns-sqs")`, or
`-Dprefab.streams.test.backend=sns-sqs` for a whole run. It runs the engine synchronously with in-memory state, so
topology tests need neither LocalStack nor a database. The module's integration test runs the backend on LocalStack
and Postgres.

### Postgres state for the in-process engine (`prefab-streams-postgres`)

`prefab-streams-postgres` implements the engine's `StreamStateRepository` (decision-008) with `JdbcTemplate`. When
the application has a `JdbcTemplate` and a `PlatformTransactionManager`, auto-configuration registers a
`PostgresStreamStateRepository`; define your own `StreamStateRepository` bean to replace it.

The transaction manager must manage the same `DataSource` object as the `JdbcTemplate`. Otherwise each statement
would autocommit on its own, and a version conflict halfway through a commit would keep the message's inbox entry and
part of its state while losing the rest of the state and its outputs. The repository rejects a
`DataSourceTransactionManager` for another `DataSource` at construction, and fails a commit, before writing anything,
when the transaction does not hold a connection for the template's `DataSource`.

| Table | Purpose |
|-------|---------|
| `prefab_stream_store` | Keyed state, windows, join buffers and deduplication identities. Every row has a `version` for optimistic concurrency. Windows, join buffers and identities carry an `expiry_key` and `expires_at` (event time). |
| `prefab_stream_inbox` | Processed input messages (`subscription`, `message_id`), so redelivered messages are skipped. |
| `prefab_stream_outbox` | Output records written in the message transaction, published by the relay in `seq` order. |
| `prefab_stream_lease` | One relay lease per sink topic, with expiry. Another instance takes over after it expires. |

Values are `bytea` (JSON with concrete class names, including nested polymorphic values, by default; provide a
`StreamStateCodec` bean to change it). State rows are trusted application data: the default codec resolves classes
from stored metadata. Restrict write access to the state tables and use a stricter codec if database contents can be
controlled by untrusted parties. Rows written by the previous JSON envelope remain readable.
Keys use `StreamKeyEncoding`, shared with the Kafka backend. Commit validates every state version observed while
processing a message, including read-only observations and keys observed as absent; a changed version, claimed inbox
identity or deadlock throws `StreamStateRepository.OptimisticConflictException`, which the engine retries.

| Property | Default | Description |
|----------|---------|-------------|
| `prefab.streams.state.initialize-schema` | `always` | `always` runs the idempotent DDL below on start; `never` leaves the tables to you. The application's Flyway `V{n}` sequence is not used. |
| `prefab.streams.inbox-retention` | `7d` | Wall-clock retention of inbox rows (decision-008), bound by the Pub/Sub backend. |

Closed windows, join buffers and deduplication identities are removed by the engine through `deleteExpired` with the
operator's stream time; expired relay leases are removed together with old inbox rows.

Limitation: a row deleted and recreated between one transaction's read and write is not detected, because deleted rows
leave no version behind.

DDL for teams that manage their own schema (`initialize-schema=never`):

```sql
-- Prefab Streams state schema for PostgreSQL. Idempotent; safe to run on every start.

-- Keyed state, windows, join buffers and deduplication identities.
-- Windows and join buffers are rows with an expiry (expiry_key + expires_at).
CREATE TABLE IF NOT EXISTS prefab_stream_store (
    namespace  TEXT        NOT NULL,
    state_key  BYTEA       NOT NULL,
    state_value BYTEA     NOT NULL,
    version    BIGINT      NOT NULL,
    expiry_key BYTEA,
    expires_at TIMESTAMPTZ,
    PRIMARY KEY (namespace, state_key)
);
CREATE INDEX IF NOT EXISTS prefab_stream_store_expiry
    ON prefab_stream_store (namespace, expiry_key, expires_at) WHERE expiry_key IS NOT NULL;

-- Processed input messages, for deduplication of redelivered messages.
CREATE TABLE IF NOT EXISTS prefab_stream_inbox (
    subscription TEXT        NOT NULL,
    message_id   TEXT        NOT NULL,
    recorded_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (subscription, message_id)
);
CREATE INDEX IF NOT EXISTS prefab_stream_inbox_recorded_at ON prefab_stream_inbox (recorded_at);

-- Output records waiting to be published. seq gives the publish order.
CREATE TABLE IF NOT EXISTS prefab_stream_outbox (
    seq     BIGSERIAL PRIMARY KEY,
    id      TEXT  NOT NULL UNIQUE,
    topic   TEXT  NOT NULL,
    message BYTEA NOT NULL
);
CREATE INDEX IF NOT EXISTS prefab_stream_outbox_topic ON prefab_stream_outbox (topic, seq);

-- One relay per sink topic, with expiry so another instance can take over.
CREATE TABLE IF NOT EXISTS prefab_stream_lease (
    topic      TEXT        PRIMARY KEY,
    owner      TEXT        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
```

### MongoDB state for the in-process engine (`prefab-streams-mongodb`)

`prefab-streams-mongodb` is the MongoDB counterpart of `prefab-streams-postgres`: it implements the engine's
`StreamStateRepository` (decision-008) with `MongoTemplate`. When the application has a `MongoTemplate`,
auto-configuration registers a `MongoStreamStateRepository`; define your own `StreamStateRepository` bean to replace
it. Use it with `prefab-streams-pubsub` or `prefab-streams-sns-sqs` instead of `prefab-streams-postgres`.

```xml
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-streams-mongodb</artifactId>
</dependency>
```

**Transactions are required.** Every input message is processed in one multi-document transaction (inbox, state and
outbox together), so the deployment must be a replica set or a sharded cluster. A standalone `mongod` is rejected at
startup with `MongoTransactionsUnavailableException`, reported by a Spring Boot failure analyzer that explains how to
run a replica set. A single-node replica set is enough for development; the Testcontainers `MongoDBContainer` is
started as one with `withReplicaSet()`.

The repository uses a private `MongoTransactionManager`; it does not register one, so your application's transaction
management is unaffected.

| Collection | Purpose |
|------------|---------|
| `prefab_stream_store` | Keyed state, windows, join buffers and deduplication identities, with `_id` `{namespace, key}`. Every document has a `version` for optimistic concurrency. Windows, join buffers and identities carry `expiryKey` and `expiresAt` (event time). |
| `prefab_stream_inbox` | Processed input messages, with `_id` `{subscription, messageId}` and `recordedAt`, so redelivered messages are skipped. |
| `prefab_stream_outbox` | Output records written in the message transaction, published by the relay in `seq` order per topic. |
| `prefab_stream_lease` | One relay lease per sink topic (`_id`), with `owner` and `expiresAt`. Another instance takes over after it expires. |

Values, keys, the `StreamStateCodec` and the retention of expired data work as described for Postgres. Commit validates
every state version observed while processing a message, including read-only observations and keys observed as absent;
a changed version, claimed inbox identity or transient transaction error throws
`StreamStateRepository.OptimisticConflictException`, which the engine retries.

| Property | Default | Description |
|----------|---------|-------------|
| `prefab.streams.state.initialize-schema` | `always` | `always` creates the collections and indexes on start (idempotent); `never` leaves them to you. |

Indexes created by `initialize-schema=always`:

| Collection | Index | Purpose |
|------------|-------|---------|
| `prefab_stream_store` | `expiry`: `_id.namespace`, `expiryKey`, `expiresAt`, partial on `expiryKey` existing | Expiry scans of windows, join buffers and identities |
| `prefab_stream_inbox` | `recordedAt` | Retention cleanup |
| `prefab_stream_outbox` | `topicSeq`: `topic`, `seq` | Relay ordering |

The collections themselves are created up front because a transaction cannot implicitly create a collection on every
MongoDB version.

### Exactly-once processing and event deduplication

**Exactly-once processing.** Set `prefab.streams.processing-guarantee=exactly_once_v2` to run Kafka Streams with
transactional processing (default: `at_least_once`; hyphenated values such as `exactly-once-v2` are accepted, any other
value fails at startup). Prerequisites and limitations:

- Brokers must be Kafka 2.5+; transaction state topic replication should be 3 in production.
- The guarantee covers read-process-write cycles between Kafka topics and Kafka Streams state stores. Side effects
  outside Kafka (HTTP calls, database writes in a `process(...)` step) are not covered and can run more than once.
- Throughput and end-to-end latency are affected by the commit interval (transactions are committed on every commit).

**Deduplication.** `deduplicate(...)` suppresses records whose event identity was already seen:

```java
streams.from(OrderPlaced.class)
        .deduplicate(order -> order.eventId(), Duration.ofHours(1))
        .to(OrderAccepted.class);
```

- `deduplicate(Function<V, String> idExtractor, Duration retention)` uses a custom identity;
  `deduplicate(Duration retention)` uses the string form of `Keyed.key()`.
- The first record per identity is forwarded unchanged; later records with the same identity within `retention` (event
  time, from the record timestamp) are dropped. After the retention period the identity is forgotten and a repeated
  event is emitted again. Records without an identity (extractor returns `null`) are always forwarded.
- State lives in a persistent, changelog-backed window store named `deduplicate-{input-type}`; retention bounds its size.
- Identities are tracked per partition: records sharing an identity must share a record key, otherwise re-key first
  with `keyBy(...)`.
- Deduplication only guarantees idempotent output when upstream duplicates fall within the retention window. Combine it
  with exactly-once processing to avoid duplicates introduced by the application itself.
### Global state stores

`globalStore(Class<V>)` declares a Kafka Streams **global store**: a read-only store that is fed from the topic
registered for `V` and fully replicated to every application instance. Processors can look up any key regardless of
the partition of the record being processed, which makes it suited for reference data such as customer profiles.

Use a global store when many processors or branches need to read the same, relatively small reference data set. Use a
local per-processor store (`StatefulStreamProcessor` with `createStore(...)`) when state is private to one processor and
partitioned by the input key; local stores are **not** shared between instances.

```java
@Configuration
class StreamTopologyConfiguration {

    @Bean
    StreamDefinition orderEnrichment(PrefabStreams streams) {
        var customerProfiles = streams.globalStore(CustomerProfile.class);

        return streams.from(OrderPlaced.class)
                .process(new EnrichOrderProcessor(customerProfiles))
                .to(OrderEnriched.class);
    }
}
```

```java
class EnrichOrderProcessor extends ContextualStreamProcessor<OrderId, OrderPlaced, OrderId, OrderEnriched> {
    private final GlobalStore<CustomerId, CustomerProfile> customerProfiles;

    EnrichOrderProcessor(GlobalStore<CustomerId, CustomerProfile> customerProfiles) {
        this.customerProfiles = customerProfiles;
    }

    @Override
    public Collection<GlobalStore<?, ?>> globalStores() {
        return List.of(customerProfiles);
    }

    @Override
    public void process(StreamRecord<OrderId, OrderPlaced> record) {
        var profile = customerProfiles.get(record.value().customerId());
        // ...
    }
}
```
Behaviour and constraints:

- The store is keyed by `Keyed.key()` and populated only from the source topic of the value type; processors can read
  (`GlobalStore.get`) but not write. Publish changes to the topic instead.
- A record with a `null` value removes the key.
- The state store is named `global-{name}`; the name defaults to the simple name of the store type and can be overridden
  with `globalStore(String name, Class<V>)`. Declaring the same name again with the same types returns the same store.
- Kafka allows **one global store per source topic**, so each source type can back at most one global store; a second
  declaration on the same source type fails fast. To keep several stores of the same store type, or a store whose type
  differs from the topic's records, use a distinct source type and a mapper:
  `globalStore(String name, Class<S> sourceType, Class<V> storeType, Function<S, V> mapper)`. A mapper result of
  `null` removes the key. Stores whose type differs from the source type are serialised as JSON.
- The store has no changelog topic; on restart it is restored from the source topic. Use a compacted topic, and ensure
  the source topic exists before Kafka Streams starts (for example, provision it with a `NewTopic` bean).
- A processor reads a global store by returning it from `StreamProcessor.globalStores()`; the framework binds it to the
  processor context. Returning a store
  that was not declared via `globalStore(...)` or `globalStoreFrom(...)` fails fast with an `IllegalArgumentException`.
- A global store is read from the processing thread, so a lookup may not yet see a record that was just published to the
  source topic.

#### Stores computed from the source records

A processor cannot write to a global store, because Kafka allows exactly one writer: the processor attached to the
store's source topic. When the store content is computed (counters, aggregates) rather than mapped one-to-one, declare it
with a `GlobalStoreUpdater`:

```java
@Bean
GlobalStore<String, SnapshotCounter> snapshotCounters(PrefabStreams streams, ApplicationEventPublisher publisher) {
    return streams.globalStoreFrom(OrderLifecycleEvent.class)
            .updatedBy(new SnapshotCounterUpdater(publisher))
            .as(SnapshotCounter.class); // or .as("snapshot-counters", SnapshotCounter.class)
}
```

```java
class SnapshotCounterUpdater
        implements GlobalStoreUpdater<String, OrderLifecycleEvent, String, SnapshotCounter> {
    @Override
    public void update(StreamRecord<String, OrderLifecycleEvent> record, Store<String, SnapshotCounter> store) {
        var counter = store.get(record.key()).orElseGet(() -> SnapshotCounter.init(record.key()));
        store.put(record.key(), counter.apply(record.value()));
    }
}
```

- The updater only consumes: it cannot forward records, has no processor context and cannot bind other stores.
- Tombstones (`null` values) are not passed to the updater.
- The returned `GlobalStore` is read-only; return it from `StreamProcessor.globalStores()` to read it. It is registered on
  the shared `StreamsBuilder`, so no `StreamDefinition` is involved.
- The same constraints apply as for other global stores: one global store per source type, unique names (a name can
  only be declared once). The stored type is serialised as JSON when it differs from the source type.
- The updater runs on every application instance, and Kafka replays the source topic through it when the store is
  restored, so side effects in an updater (for example publishing application events) can repeat. Keep them idempotent.
### Testing global stores

All stream definitions and global stores declared on one `PrefabStreams` share a single topology, so the session of a test runs all of them. Pipe records into the source topic with `topology.input(...)` and assert on the store with `topology.lookup(customerProfiles, key)`.

### Ending a topology without a sink

`to(...)` returns the `StreamDefinition`. When a branch ends in a `process(...)` step that has no output topic, call
`end()` to obtain the `StreamDefinition`:

```java
return streams.from(OrderPlaced.class)
        .process(new AuditProcessor(customerProfiles))
        .end();
```
### Key Serialization Wire Format (JSON + AVRO)

Kafka stream keys are automatically serialized without manual `parse(String)` / `toString()` plumbing.

**How it works:**

- For **JSON topics**, key types (`Key<K>`) are serialized as JSON bytes via Jackson's `ObjectMapper`.
- For **AVRO topics**, key types are serialized as AVRO bytes using the Confluent AVRO serde stack.
- AVRO key conversion first uses registered Spring converters (`KeyType <-> GenericRecord`) when available, and otherwise falls back to reflection-based record mapping for record-style keys.
- Deserialization reconstructs the key object directly; manual `Key.register(...)` parsers are no longer required.

**Example key type (no manual serialization needed):**

```java
public record OrderKey(Reference<Order> orderId, Instant timestamp) implements Key<OrderKey> {
    // No parse(String), toString(), or Key.register() required!
}
```

**Deprecated API:**

The legacy `Key.register(Class, Function)` method is now deprecated and will be removed in a future release. If you have existing key types using manual registration, migration is simple: remove the static initializer block and custom `parse()`/`toString()` methods, and let framework-managed JSON/AVRO key serdes handle serialization.

Example migration:

```java
// Old (deprecated):
public record OrderKey(...) implements Key<OrderKey> {
    static {
        Key.register(OrderKey.class, OrderKey::parse);
    }
    public static OrderKey parse(String str) { ... }
    public String toString() { ... }
}

// New (recommended):
public record OrderKey(...) implements Key<OrderKey> {
    // Jackson handles everything automatically
}
```

Example topology with subtype branching and factory merge:

```java

@Configuration
class StreamTopologyConfiguration {

    @Bean
    StreamDefinition streamEventRoutingTopology(PrefabStreams streams) {
        var classified = streams.from(StreamEvent.class)
                .flatMap(event -> List.of(event.payload().split(",")))
                .map(word -> word.length() <= 4
                        ? (ClassifiedWord) new ShortWord(word)
                        : new LongWord(word));

        var shortWords = classified.branch(ShortWord.class);
        var longWords = classified.branch(LongWord.class);

        shortWords.to("streams.words.short");
        longWords.to("streams.words.long");

        return streams.merge(shortWords, longWords)
                .map(word -> word.value())
                .to("streams.words.all");
    }
}

sealed interface ClassifiedWord permits ShortWord, LongWord {
    String value();
}

record ShortWord(String value) implements ClassifiedWord {
}

record LongWord(String value) implements ClassifiedWord {
}
```

Example join with deterministic keys and explicit window:

```java
var joined = streams.from(OrderPlaced.class)
        .join(
                streams.from(ShipmentUpdated.class),
                JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                (order, shipment) -> new FulfillmentJoined(order.orderId(), order.customer(), shipment.status())
        )
        .to(FulfillmentJoined.class);
```

In this setup, `OrderPlaced.orderId` and `ShipmentUpdated.orderId` are emitted as deterministic Kafka keys
(for example by annotating those fields with `@PartitioningKey`).

Example breakout that injects a native Kafka Streams fragment via adapter SPI:

```java

@Configuration
class StreamTopologyConfiguration {

    @Bean
    StreamDefinition streamEventRoutingTopology(PrefabStreams streams) {
        return streams.from(StreamEvent.class)
                .breakout(new KafkaStreamBreakoutAdapter<>(
                        nativeStream -> nativeStream.selectKey((key, value) -> value.id())
                ))
                .to("streams.words.all");
    }
}
```

Portability note:

- `breakout(...)` accepts a backend adapter abstraction in core DSL API.
- `KafkaStreamBreakoutAdapter` (in `prefab-streams-kafka`) is Kafka-specific and should be treated as an escape hatch.
- `StreamBreakoutAdapter.backend()` returns a `StreamBackend`, an open identifier rather than an enum, so a backend
  module can add its own adapter without a change to `prefab-streams`.
- Pipelines using breakout are no longer backend-portable unless equivalent adapters exist for other backends.

`StreamDefinition` beans are auto-discovered by Prefab Streams at startup. Their DSL wiring is bootstrapped into one
Kafka Streams topology automatically, so no extra manual topology startup code is required.

Topology tests use the backend-neutral `TopologyTestExtension` (package `be.appify.prefab.streams.testing`). It injects
a fresh `PrefabStreams` to declare the topology on and a `TopologySession` to run it. The session starts on first use
and is closed after the test, so no set-up code and no try-with-resources are needed.

```java
@ExtendWith(TopologyTestExtension.class)
class OrderTopologyTest {
    @TestStreams PrefabStreams streams;
    @TestStreams TopologySession topology;

    @Test
    void shouldForwardOrder() {
        streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), order.customer()))
                .to(ProcessedOrder.class);

        topology.input(IncomingOrder.class).send(new IncomingOrder("o-1", "Alice"));

        assertThat(topology.output(ProcessedOrder.class).values())
                .containsExactly(new ProcessedOrder("o-1", "Alice"));
    }
}
```

- `input(...).send(value[, timestamp])` takes the key from `Keyed.key()`; use `send(key, value)` for another key and
  `sendTombstone(key)` for a record without a value.
- `output(...)` reads typed records, `output(topic)` reads the serialised values of a named topic.
- `topology.inspect()` describes the structure (step names, stores, global stores) without exposing the backend.
- `topology.unwrap(...)` gives access to the backend facility, for example Kafka's `TopologyTestDriver`.
- `TopologyTestExtension.withAppId("...")` (with `@RegisterExtension`) changes the application id used for topic names.
- `@TestStreams` also works on test method parameters.
- The extension picks a backend from the classpath by default. Use `TopologyTestExtension.withBackend("in-process")` or
  run tests with `-Dprefab.streams.test.backend=in-process` to use the in-memory engine when multiple factories are
  present. The same tests can run against every backend that registers a `TopologyTestHarness.Factory`; Kafka tests
  need `kafka-streams-test-utils` on the test classpath. Tests that inspect serialized Kafka key bytes or unwrap
  Kafka-specific facilities remain Kafka-backend tests.

### Message and correlation IDs (opt-in)

Prefab streams can trace which consumed messages contributed to every published message. Tracing is off by default;
when off, the topology, state store names and the bytes written to state stores and internal topics are unchanged.

```yaml
prefab:
  streams:
    tracing:
      enabled: true            # default: false, applies to all streams of the application
      max-correlation-ids: 100 # default: 100
```

Semantics:

- Every message published by `to(...)` gets a fresh **message ID** in the `prefab-message-id` header.
- A message consumed by `from(...)` without a message ID is assigned a random one.
- The **correlation IDs** of a published message are the message IDs of all consumed messages it derives from, in the
  `prefab-correlation-ids` header (comma-separated, so it also works for systems that only support one value per
  header). The header is absent when a message does not originate from another message, for example one emitted by a
  scheduled punctuator.
- IDs are minted when publishing, not on every operator. Chains of `map`, `filter`, `flatMap`, `repartition`,
  `process` and so on therefore keep the lineage of the consumed message, also without an intermediary topic.
- `join` correlates to both contributing messages. `aggregate` and `windowedAggregate` keep the union of the folded
  messages in the state store (per window for windowed aggregates).
- Custom processors inherit the correlation IDs of the record being processed. Records forwarded outside `process`
  have none, unless set explicitly.

Limitations:

- A union is capped at `max-correlation-ids`; when exceeded, the oldest IDs are dropped first.
- `breakout` hides records from Prefab, so lineage is lost across a native fragment.
- The processor based `aggregate(groupBy, aggregation, egressCondition)` only correlates to the message that triggered
  the output (TASK-304).

Test with `@RegisterExtension static final TopologyTestExtension EXTENSION = TopologyTestExtension.withTracing();`,
send with `input(...).sendWithMessageId(value, "m-1")` and assert on `output(...).messages()`, which expose
`messageId()` and `correlationIds()`.

#### Enabling tracing on a running application

Joins and aggregations keep correlation IDs inside their state stores and repartition topics, wrapped in a small
envelope that starts with the bytes `0xFF 'P' 'T'`. Every version that contains this feature can **read** both the
old and the new format, whether or not tracing is enabled; only the writer depends on the setting. Information that
was never recorded cannot be recovered: state written before enabling reads as "no correlation IDs" until it is
updated. To roll out without breaking running instances:

1. Deploy the new version with `prefab.streams.tracing.enabled=false`. All instances can now read the envelope.
2. Once no old instance is running, set `prefab.streams.tracing.enabled=true` and deploy again.

To turn tracing off again, first disable it and finish the rollout; stored envelopes are still read and are rewritten
in the plain format as state is updated. Do not roll back to a version without this feature after enabling tracing, as
it cannot read the envelope. Tracing steps have fixed names (`trace-*`) and never change the names of existing steps,
stores or topics.
---

## 7.16 Per-aggregate Plugin Overrides

Use `@Generate` on a specific aggregate when plugin behavior must differ from project defaults.

```java

@Aggregate
@Generate(plugin = AsyncApiDocumentationPlugin.class, enabled = false)
@Generate(plugin = CreatePlugin.class, target = OutputTarget.TEST)
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        String customerName
) {
    @Create
    public Order(String customerName) {
        this(Reference.create(), 0L, customerName);
    }
}
```

What this does:

- Disables AsyncAPI generation only for `Order`
- Routes `CreatePlugin` artefacts for `Order` to test output when supported
- Leaves all other aggregates on their normal project-wide/default behavior

Precedence order:

1. `@Generate` on the aggregate
2. compiler option `-Aprefab.plugin.<id>.enabled=...`
3. plugin default (enabled)

Project-wide option example (Maven):

```xml

<compilerArg>-Aprefab.plugin.create.enabled=false</compilerArg>
```

Then re-enable for one aggregate:

```java

@Generate(plugin = CreatePlugin.class, enabled = true)
@Aggregate
public record CriticalOrder(...) {
}
```

Tips:

- Use repeatable `@Generate` to configure multiple plugins on one type
- Prefer `OutputTarget.DEFAULT` unless you explicitly need generated test artefacts
- If a plugin class is invalid or missing from processor classpath, compilation fails fast with a clear message

---

## 7.17 Autocomplete Endpoints

`@Autocomplete` generates a `GET` endpoint that returns distinct field values matching a query term.
Two orthogonal attributes control the matching behaviour:

| Attribute       | Controls                 | Default                     |
|-----------------|--------------------------|-----------------------------|
| `scanMode`      | Where the term appears   | `ScanMode.PREFIX`           |
| `matchStrategy` | How the term is compared | `MatchStrategy.IGNORE_CASE` |

### Basic usage

```java

@Aggregate
public record Product(
        @Id String id,
        @Version long version,
        @Autocomplete String name          // PREFIX + IGNORE_CASE
) {
}
```

**Generated endpoint:** `GET /products/name/autocomplete?query=ap&page=0&limit=20` → `["Apple", "Apricot"]`

Pass a different `limit` to control page size and increment `page` to fetch the next slice of
results, for example `GET /products/name/autocomplete?query=ap&page=1&limit=20`.

---

### Scan mode

```java
// PREFIX — generated SQL: LOWER("name") LIKE LOWER(CONCAT(:query, '%'))
@Autocomplete(scanMode = ScanMode.PREFIX)
String name;

// CONTAINS — generated SQL: LOWER("name") LIKE LOWER(CONCAT('%', :query, '%'))
@Autocomplete(scanMode = ScanMode.CONTAINS)
String name;
```

MongoDB uses an anchored regex (`^term`) for `PREFIX` and an unanchored regex (`term`) for `CONTAINS`.

---

### Match strategy

```java
// EXACT (case-sensitive)
@Autocomplete(matchStrategy = MatchStrategy.EXACT)
String sku;

// IGNORE_CASE (default)
@Autocomplete(matchStrategy = MatchStrategy.IGNORE_CASE)
String name;

// FUZZY — requires pg_trgm on PostgreSQL
@Autocomplete(matchStrategy = MatchStrategy.FUZZY)
String description;

// FULL_TEXT — type-ahead over the words and initials of the value
@Autocomplete(matchStrategy = MatchStrategy.FULL_TEXT)
String title;

// FULL_TEXT_FUZZY — the same, tolerating typos (requires pg_trgm on PostgreSQL)
@Autocomplete(matchStrategy = MatchStrategy.FULL_TEXT_FUZZY)
String summary;
```

> **FULL_TEXT** matches values in which every word of the query is the start of a word, in any order: `spr bo g`
> finds `Spring Boot guide`. A single word of two or more letters also matches the initials of the words: `sbg` finds
> `Spring Boot guide`. `scanMode` has no effect. On PostgreSQL the query is reduced to letters and digits before it is
> turned into a `to_tsquery`, so no input can break it, and a GIN index on the same `to_tsvector('simple', ...)`
> expression plus a btree index on the initials of the value are generated. On MongoDB `$text` cannot match prefixes,
> so the service turns the query into a case-insensitive regular expression with the same semantics.
>
> **FULL_TEXT_FUZZY** is `FULL_TEXT` that also accepts typos: every word must be similar to a word of the value (or be
> the start of one), so `sprint boo` finds `Spring Boot` but `spring post` does not. It requires `pg_trgm` and
> generates a trigram index; MongoDB behaves like `FULL_TEXT`.

> **FUZZY on PostgreSQL** requires the `pg_trgm` extension:
> ```
> CREATE EXTENSION IF NOT EXISTS pg_trgm;
> SET pg_trgm.word_similarity_threshold = 0.3;
> ```
> The similarity threshold is fixed at `0.3`.
>
> **FUZZY on MongoDB** falls back to a case-insensitive regex — there is no native server-side
> fuzzy matching in MongoDB.

---

### Full query matrix

| `scanMode` | `matchStrategy` | JDBC WHERE clause                                                                                          |
|------------|-----------------|------------------------------------------------------------------------------------------------------------|
| `PREFIX`   | `EXACT`         | `"col" LIKE CONCAT(:query, '%')`                                                                           |
| `PREFIX`   | `IGNORE_CASE`   | `LOWER("col") LIKE LOWER(CONCAT(:query, '%'))`                                                             |
| `PREFIX`   | `FUZZY`         | `LOWER(:query) <% LOWER("col")` — pg_trgm word-similarity operator; matches when query is similar to a prefix/word of the value (`"foo"` matches `"foebar"`, not `"barfoo"`) |
| `CONTAINS` | `EXACT`         | `"col" LIKE CONCAT('%', :query, '%')`                                                                      |
| `CONTAINS` | `IGNORE_CASE`   | `LOWER("col") LIKE LOWER(CONCAT('%', :query, '%'))`                                                        |
| `CONTAINS` | `FUZZY`         | `similarity(LOWER("col"), LOWER(:query)) > 0.3` — whole-string similarity match                            |

---

### Custom path and security

```java

@Autocomplete(
        path = "/brands/search",
        scanMode = ScanMode.CONTAINS,
        matchStrategy = MatchStrategy.IGNORE_CASE,
        security = @Security(role = "USER")
)
String brand;
```

---

To make selected endpoints public without replacing Prefab's default
`SecurityFilterChain`, register an `HttpSecurityCustomizer` bean. Its matchers run
before the default authenticated fallback:

```java
@Bean
HttpSecurityCustomizer publicArticleReads() {
    return http -> http.authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/articles", "/articles/*").permitAll());
}
```

Import `org.springframework.http.HttpMethod` for the method-specific matcher.
`@Security(enabled = false)` disables generated method-level authorization only; an
anonymous request must also match a `permitAll()` rule in the filter chain. Extra
path-scoped `SecurityFilterChain` beans do not remove Prefab's default chain. To replace
it completely, name the replacement bean `prefabSecurityFilterChain`.

---

### Migrating from `ignoreCase`

The old `boolean ignoreCase` attribute has been removed. Migrate as follows:

```java
// Before
@Autocomplete(ignoreCase = true)
String name;
@Autocomplete(ignoreCase = false)
String sku;
@Autocomplete
String code;

// After
@Autocomplete(scanMode = ScanMode.CONTAINS, matchStrategy = MatchStrategy.IGNORE_CASE)
String name;
@Autocomplete(matchStrategy = MatchStrategy.EXACT)
String sku;
@Autocomplete
String code; // PREFIX + IGNORE_CASE
```

---

## 7.18 Aggregate-bound frontend views

Use `@ViewOf` on a record when an HTTP contract should be smaller or more screen-oriented than its aggregate
response. Add `@GetById` and/or `@GetList`; the aggregate must declare the matching operation so Prefab can
reuse its generated service. Multiple view records can target one aggregate without changing that aggregate.

```java
@Aggregate(exposure = Exposure.INTERNAL)
@GetById
@GetList
public record ChannelSummary(...) {
}

@ViewOf(ChannelSummary.class)
@GetList(security = @Security(role = "USER"))
public record ChannelDashboardItem(
        Reference<Channel> channel,
        String name,
        int totalMessages,
        int totalSubscribers,
        int unreadMessages,
        boolean unreadStatusAvailable
) {
}
```

Prefab generates the view controller, a read-only service, an HTTP test client and a test mother. A view's
`@GetById`/`@GetList` attributes configure its own path, HTTP method, sorting and security. For lists, aggregate
filters are available on the view endpoint too. The generated view service calls the aggregate service first,
so tenant filtering, paging, filter predicates and total counts are applied before mapping.

Mapping shortcuts are tried in this order:

1. Matching component names and types use a generated mapper.
2. A public constructor taking the aggregate, or a public static `from(Aggregate)` factory, maps one row.
3. A Spring bean implementing `ViewAssembler<Aggregate, View>` maps the complete, already-scoped page.

Use an assembler for enrichment, joins or aggregation. It receives one batch and a `ViewContext` with the
authenticated principal and optional tenant ID. It must return exactly one view per source row; a mismatch
fails explicitly, and generated paging retains the aggregate page's total count.

```java
@Component
class ChannelDashboardAssembler implements ViewAssembler<ChannelSummary, ChannelDashboardItem> {
    private final UserStatusRepository userStatusRepository;

    ChannelDashboardAssembler(UserStatusRepository userStatusRepository) {
        this.userStatusRepository = userStatusRepository;
    }

    @Override
    public List<ChannelDashboardItem> assemble(List<ChannelSummary> page, ViewContext context) {
        var statuses = context.currentUser()
                .map(user -> userStatusRepository.findByUser(Reference.<User>fromId(user.getName())))
                .orElseGet(List::of);
        var unreadByChannel = statuses.stream()
                .flatMap(status -> status.unreadMessages().stream())
                .collect(Collectors.groupingBy(UnreadMessage::channel, Collectors.summingInt(message -> 1)));
        var statusAvailable = !statuses.isEmpty();
        return page.stream()
                .map(channel -> new ChannelDashboardItem(
                        channel.channel(),
                        channel.name(),
                        channel.totalMessages(),
                        channel.totalSubscribers(),
                        unreadByChannel.getOrDefault(channel.channel(), 0),
                        statusAvailable))
                .toList();
    }
}
```

An internal aggregate (`@Aggregate(exposure = Exposure.INTERNAL)`) still gets its services and records but
does not expose aggregate REST operations or their OpenAPI/test-client methods. A view over it is an independent
HTTP surface: its endpoint must use its own security configuration; aggregate controller security is not inherited.
Tenant filtering remains in the aggregate service and also applies to page counts. Keep row-level authorization in
the view endpoint or in a service-side query when tenant scoping alone is insufficient.

For simple create forms, annotate a form record with both `@ViewOf(Aggregate.class)` and type-level `@Create`;
provide `toRequest()` returning the aggregate's generated `CreateAggregateRequest`. Prefab generates a
transactional adapter and secured POST endpoint, so no `ViewCommands` class is needed for a one-to-one create.
Use `ViewCommands<Aggregate>` when commands need custom mapping or orchestration. Only its methods annotated with
`@Create`, `@Update` or `@Delete` become HTTP operations; they run behind a generated transactional service.

The Kafka example demonstrates this pattern with `ChannelDashboardItem` and `ChannelDashboardAssembler`. List
views of child aggregates that need a parent path are not currently supported; expose a parent-scoped query through
a custom service instead.

---

## 7.19 Security and API Documentation

Add `prefab-security` to enable Spring Security and method security on generated endpoints. By default, generated
endpoints require an authenticated principal. Use `@Security` to require an authority or role, or to omit the
generated method-level check for one endpoint:

```java
@Aggregate
@GetById
@GetList
@Delete(security = @Security(authority = "ROLE_ADMIN"))
public record Product(...) {
    @Create(security = @Security(role = "ADMIN"))
    public Product(...) { ... }
}

@Aggregate
@GetList(security = @Security(enabled = false))
public record Category(...) { ... }
```

`authority = "ROLE_ADMIN"` checks the exact authority; `role = "ADMIN"` uses Spring Security's `hasRole` semantics.
They are mutually exclusive. `enabled = false` disables the generated method-level authorization only; the web
security filter chain may still require authentication. To make an endpoint anonymously reachable, configure the
filter chain with a suitable `HttpSecurityCustomizer` matcher as well. Customizer request matchers are applied before
Prefab's default `anyRequest().authenticated()` fallback.

`@Security` enforces endpoint-level authorities or roles, not ownership of individual records. For row-level access,
scope the data query to the principal or explicitly check ownership. The showcase keeps raw order and overview reads
admin-only and serves a customer's history at `/customers/{customerId}/order-history`, where a method-security rule
requires the authenticated principal name to match the customer ID. It removes the generic generated history list so
callers cannot select another customer's ID as a filter.

Implement `AuditContextProvider` to populate `@CreatedBy` and `@LastModifiedBy` from the active principal:

```java
@Component
class PrincipalAuditContextProvider implements AuditContextProvider {
    @Override
    public String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                ? authentication.getName()
                : "system";
    }
}
```

Add `prefab-openapi` for the generated OpenAPI document at `/v3/api-docs` and Swagger UI at
`/swagger-ui/index.html`. Add `prefab-async-api` for the compile-time event contract at
`/async-api/asyncapi.json` and its browser viewer at `/async-api`. OpenAPI annotations are also emitted during
compilation when the module is on the classpath.

The showcase defaults to the `local` profile, where it disables JWT resource-server authentication and enables HTTP
Basic for local use. Spring Boot generates a development password at startup; no OAuth credentials or static
passwords are stored in the repository. Its `test` profile uses the same mechanism with Spring Security's MockMvc
test principals. Production profiles retain Prefab's JWT configuration. For applications embedding Swagger UI,
review the default Content Security Policy before permitting inline styles or scripts.

---

## 7.20 Integration Tests: In-memory or Containers

`@IntegrationTest` (from `prefab-test`) runs the full Spring context with MockMvc. **By default it runs on an
in-memory backend and needs no Docker**; a test class or a whole module opts into real infrastructure
(Testcontainers) when it needs it.

| Mode | How to select | What runs |
|---|---|---|
| In memory (default) | nothing, or `prefab.test.containers=false` | Prefab's own `ConcurrentHashMap` repositories instead of Postgres/MongoDB, a synchronous in-process event bus instead of Kafka, Pub/Sub and SNS/SQS |
| Containers | `@IntegrationTest(containers = true)` for one class, `prefab.test.containers: true` in `application-test.yml` for the module | Postgres, MongoDB, Kafka, Pub/Sub, LocalStack and the schema registry as Testcontainers |

The code generated by the annotation processor is the same in both modes (it depends on the compile classpath, see
decision-007), so main dependencies such as `prefab-postgres` and `prefab-kafka` stay in place. At test runtime, the
store and broker auto-configurations are dropped in memory mode, and the generated repositories, listeners and
subscribers are bound to the in-memory backend. Tests written with generated test clients,
`@TestEventConsumer` and `EventConsumerAssert` run unchanged in both modes, so one test class can be run either way.

```java
@IntegrationTest                      // in memory
class OrderIntegrationTest { ... }

@IntegrationTest(containers = true)   // Testcontainers, own cached application context
class UserStatusIntegrationTest { ... }
```

### Persistence in memory

- Every generated repository, including polymorphic (sealed) aggregates, is backed by one map per aggregate root.
  Derived queries, Query by Example (`@GetList` filters), range and full-text filters, `@Autocomplete` and
  `@RepositoryMixin` derived queries work. Full-text relevance and fuzzy matching are approximations of the
  Postgres and MongoDB ranking.
- `@Version` is checked: saving a stale copy throws `OptimisticLockingFailureException` (HTTP 409).
- State is cleared before each test, as with containers.

### Events in memory

- Events are **serialized and deserialized** with the platform's own serializers (JSON and Avro, version headers,
  `@EventMigration`), so schema and serialization problems surface in the publishing call as with a real broker.
  Avro uses Confluent's mock schema registry; a configured `schema.registry.url` is ignored.
- Delivery is **synchronous and in-process**, after the publishing transaction commits. When a MockMvc call returns,
  every cascaded handler has run, so assertions on `EventConsumerAssert` pass on the first poll. Events published by a
  handler are delivered after that handler returns, in publish order.
- Every `@KafkaListener` method, Pub/Sub subscriber and SQS subscriber of the application receives the events of its
  topics; `@TestEventConsumer` fields are filled the same way.
- An event handler that throws does **not** fail the request that published the event; the failure is recorded and
  **fails the test** that triggered it, with the handler as the cause. A test that expects a handler to fail clears
  the recorded failures with `InMemoryEventBus.drainFailures()`. Retries and dead-letter topics are not emulated.

### Known differences

| Area | In memory | With containers |
|---|---|---|
| Native queries (`@Query`, `@Aggregation` in a `@RepositoryMixin`) | Fail when called, naming the method and `@IntegrationTest(containers = true)`; a warning is logged at startup | Run |
| Database constraints (unique indexes, foreign keys, column sizes), Flyway and Mongo migrations | Not enforced or validated | Enforced |
| Rollback | Writes of a rolled-back transaction are **not** undone | Rolled back |
| Retries, dead-letter topics, `@EventHandlerConfig` retry settings, `@AsyncCommit` | Not emulated | Real broker behaviour |
| Partitions, offsets, consumer groups, concurrency | None: one delivery per listener method | Real |
| Prefab Streams topologies | Not available (TASK-374) | Run |
| Schema registry HTTP API | None (mock registry), TASK-375 | Real registry |
| Code injecting `DataSource`, `JdbcTemplate`, `MongoTemplate` or `KafkaTemplate` | No such beans | Available |

Tests that rely on one of these use `@IntegrationTest(containers = true)`. Keep most tests in memory and run the few
that exercise infrastructure on containers: a test class with `containers = true` gets its own cached context.
Do not add `containers = true` in a module whose `application-test.yml` already sets `prefab.test.containers: true`:
it changes nothing but starts a second containers context, and a second Pub/Sub emulator setup currently removes
the emulator of the first context (TASK-376).

### Upgrading

In earlier Prefab releases `@IntegrationTest` always ran on containers. To keep the old behaviour for a whole module, set
`prefab.test.containers: true` in `src/test/resources/application-test.yml`. This repository does that for all
examples except `examples/showcase-inmemory`, which proves the in-memory backend on the showcase (see
[examples/README.md](../../examples/README.md#technology-coverage)).
