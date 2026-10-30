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

To get a stable order (and stable paging), declare a default sort order on `@GetList`:
`@GetList(sorted = @SortOrder(property = "name"))`. It applies only when the request has no `sort` parameter;
an explicit `?sort=price,desc` replaces it entirely. It combines with filters, paging, tenant and parent.

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
For Kafka JSON, repeat `@Event` on the generic declaration to bind each concrete argument
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

`typeArguments` are positional, concrete non-generic class literals. Incorrect counts,
duplicate bindings, violated bounds, raw arguments and handlers without a declared concrete
binding are compiler errors. Wildcard handlers are not supported in this initial integration.

TASK-303 remains in progress: generic Avro, Pub/Sub, SNS/SQS, provisioning, streams and
per-binding evolution/migration integration are not complete. Kafka JSON stamps the exact
binding's version and enforces its minimum supported version; generic major-version or
component migration is explicitly rejected rather than processed as an erased event.

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
public interface SaleEvent {}

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
public interface SaleEvent {
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

For nullable Avro fields, Prefab treats annotations named `Nullable` (including
`jakarta.annotation.Nullable`) as optional markers and generates Avro unions as `["null", T]` with a
`null` default.

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
        @Download
        @ContentType({"application/pdf", "application/msword"})
        @FileSize(max = 10 * 1024 * 1024)
        Binary file
) {
    @Create
    public Document(String title, Binary file) {
        this(Reference.create(), 0L, title, file);
    }
}
```

Generated endpoints:

- `POST /documents` (multipart/form-data with `title` field and `file` file part)
- `GET  /documents/{id}/file` — download the binary

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

Use this strategy in event-driven aggregates:

1. **Serialize handler execution per aggregate type** using `@EventHandlerConfig(concurrency = "1")` on aggregates
   that receive hot-key updates (`@ByReference` / `@Multicast` handlers).
2. **Prefer deterministic IDs in create-or-update projections** so duplicate create races collapse into one logical
   aggregate record. For example, derive a summary ID from the source aggregate reference.
3. **Combine static create handlers + update handlers** for the same event type to make first-write wins deterministic
   while still allowing subsequent updates.
4. **Design for idempotency** in projection handlers so that retries and duplicate deliveries do not cause incorrect
   state or side effects.
5. **For higher concurrency, partition by hot key first** at the platform level (e.g. Kafka partitioning key, Pub/Sub
   ordering key) to ensure all events for the same aggregate are processed by the same consumer instance.

### Platform guidance

- **Kafka**: partitioning already keeps a key ordered within a partition, but independent handlers can still race on
  database updates for the same row. Keep hot-key handlers single-threaded when they target one aggregate table.
- **Pub/Sub**: at-least-once delivery and parallel callback threads increase race risk; set handler concurrency to `1`
  for hot-key aggregates and rely on deterministic IDs for projection upserts.
- **SNS/SQS**: delivery fan-out can cause concurrent updates across consumers; use the same hot-key strategy as Pub/Sub
  and keep projection creation idempotent with deterministic IDs.

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

Prefab provides a dedicated `streams` module (`prefab-streams`) with a Kafka-backed DSL.

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
- The store has no changelog topic; on restart it is restored from the source topic. Use a compacted topic.
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
    return streams.globalStoreFrom(MeteringconfigLifecycleEvent.class)
            .updatedBy(new SnapshotCounterUpdater(publisher))
            .as(SnapshotCounter.class); // or .as("snapshot-counters", SnapshotCounter.class)
}
```

```java
class SnapshotCounterUpdater
        implements GlobalStoreUpdater<String, MeteringconfigLifecycleEvent, String, SnapshotCounter> {
    @Override
    public void update(StreamRecord<String, MeteringconfigLifecycleEvent> record, Store<String, SnapshotCounter> store) {
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
- `KafkaStreamBreakoutAdapter` is Kafka-specific and should be treated as an escape hatch.
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
- The extension picks the backend from the classpath, so the same tests run against every backend that registers a
  `TopologyTestHarness.Factory`. The Kafka implementation needs `kafka-streams-test-utils` on the test classpath.

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

To extend the default `WebSecurityConfiguration` without replacing Prefab's
`SecurityFilterChain`, register an `HttpSecurityCustomizer` bean:

```java
@Bean
HttpSecurityCustomizer docsCustomizer() {
    return http -> http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll());
}
```

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
