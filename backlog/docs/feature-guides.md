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

---

## 7.2 Event Publishing

Any aggregate that `implements PublishesEvents` can call `publish(event)` from constructors or update methods.

When an event subtype extends or implements a supertype annotated with `@Event`, Prefab generates a publisher for the
annotated supertype only. It does not generate one publisher class per concrete subtype.

```java

@Aggregate
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        String customerName
) implements PublishesEvents {

    @Create
    public Order(String customerName) {
        this(Reference.create(), 0L, customerName);
        publish(new OrderCreated(id, customerName));  // publish event
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

Combine a static `@EventHandler` with a `@ByReference` or `@Multicast` handler for the same event type:

```java

@EventHandler
public static Order onCreate(OrderCreated event) {
    return new Order(event.id(), 0L, event.customer());
}

@EventHandler
@ByReference(property = "id")
public void onUpdate(OrderUpdated event) {
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
}
```

The processor generates `SaleCreated` and `SalePaid` records in the same Java package as `SaleEvent`.
Generated Java type names are always capitalised. When the original Avro name starts with a lowercase
letter (e.g. `saleCreated`), the generated record is capitalised (`SaleCreated`) and annotated with
`@AvroSchema(name = "saleCreated")` so schema factories can recover the original Avro name during
serialisation.

At runtime, generated schema factories validate their in-memory schema against the referenced AVSC
files. If a generated schema is not compatible with the AVSC contract, schema factory initialization
fails fast with an explicit exception.

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

Aggregate roots that `implement PublishesEvents` call `publish()` inside constructors or update
methods. In a plain JUnit 5 unit test there is no Spring context, so events would silently be
discarded. `prefab-test` provides two classes to make unit-testing event publishing easy and safe.

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
- `breakout(StreamBreakoutAdapter<?, ?, ?, ?>)`
- `to(Class<?>)`
- `to(String)`

Serialization and deserialization reuse the existing Kafka dynamic serde infrastructure:

- `DynamicDeserializer` for source records
- `DynamicSerializer` for sink records

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

Topology tests can use the lightweight `KafkaTopologyTestBootstrap` helper to avoid repetitive
`TopologyTestDriver` and serde setup code.

Example:

```java

@Test
void shouldForwardOrder() {
    var test = KafkaTopologyTestBootstrap.bootstrap();
    test.registerJson("orders.in", IncomingOrder.class);
    test.registerJson("orders.out", ProcessedOrder.class);

    var topology = test.streams(new StreamsBuilder())
            .from(IncomingOrder.class)
            .to(ProcessedOrder.class);

    try (var topologyTest = test.run(topology)) {
        topologyTest.input("orders.in").pipeInput("o-1", new IncomingOrder("o-1", "Alice"));
        assertThat(topologyTest.output("orders.out").readValue())
                .isEqualTo(new ProcessedOrder("o-1", "Alice"));
    }
}
```

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

**Generated endpoint:** `GET /products/name/autocomplete?query=ap` → `["Apple", "Apricot"]`

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
```

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
        security = @Security(authenticated = true, authorities = {"ROLE_USER"})
)
String brand;
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



