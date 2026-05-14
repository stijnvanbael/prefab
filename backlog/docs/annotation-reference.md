# Annotation Reference

**Part of the [Prefab Developer Guide](developer-guide.md)**

---

## Table of Contents

- [4.1 Aggregate Annotations](#41-aggregate-annotations)
- [4.2 REST Annotations](#42-rest-annotations)
- [4.3 Event Annotations](#43-event-annotations)
- [4.4 Event Handler Annotations](#44-event-handler-annotations)
- [4.5 Database Annotations](#45-database-annotations)
- [4.6 Audit Annotations](#46-audit-annotations)
- [4.7 Multi-tenancy Annotations](#47-multi-tenancy-annotations)
- [4.8 Validation Annotations](#48-validation-annotations)
- [4.9 Extension Annotations](#49-extension-annotations)
- [Quick Reference Table](#quick-reference-table)

---

## 4.1 Aggregate Annotations

### `@Aggregate`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE` (class or record)
**Retention:** `RUNTIME`

Marks a class as an aggregate root — the primary domain entity managed by Prefab.

**Attributes:** None

**Generated artefacts:**
- `{Type}Controller` – Spring MVC REST controller
- `{Type}Service` – Spring `@Service` with CRUD logic
- `{Type}Repository` – Spring Data `CrudRepository` + `PagingAndSortingRepository`
- `{Type}Request` records (one per `@Create`/`@Update` method)
- `{Type}Response` record
- Flyway migration script (e.g. `V001__order.sql`) or MongoDB script

**Rules:**
- Must be a Java `record`
- Must have an `@Id Reference<Self>` field
- Must have a `@Version long` field

**Example:**

```java
@Aggregate
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

---

### `@AsyncCommit`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`, `CONSTRUCTOR`, `METHOD`
**Retention:** `SOURCE`

Marks an aggregate or individual method to use the **async-commit (listen-to-self)** pattern. Instead
of persisting synchronously, the create/update method publishes an event and returns `202 Accepted`.
An `@EventHandler` on the same aggregate then persists it when the event arrives.

**Attributes:** None

**Placement and scope:**

| Placement                               | Scope                                                                                                                      |
|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| On the aggregate **type**               | All `@Create` **and** all `@Update` methods are async — they return `202 Accepted` and do **not** call `repository.save()` |
| On a specific **`@Create` method**      | Only that method is async                                                                                                  |
| On a specific **`@Update void` method** | Only that method is async                                                                                                  |

> **ℹ Note**: Placing `@AsyncCommit` at the type level means the entire aggregate follows the
> listen-to-self pattern. Every `@Create` and `@Update` method must publish an event instead of
> returning a value, and state is persisted exclusively via `@EventHandler` consumers.
> If only selected methods should be asynchronous, place `@AsyncCommit` on those methods individually
> rather than on the type.

**Behaviour:**
- On `@Create` static factory method: must have `void` return type and call `PublishesEvents.publishEvent(event)` internally; the generated endpoint returns `202 Accepted`. The event is routed through the Spring application event bus to the generated Kafka (or Pub/Sub, SNS) producer.
- On `@Update void` method: method must call `PublishesEvents.publishEvent(event)` internally; generates `202 Accepted`; `repository.save()` is **not** called.
- `@EventHandler` methods on an `@AsyncCommit` aggregate receive a deduplication guard in the generated consumer.

> **⚠ Warning**: A `@Create @AsyncCommit` method with a non-void return type will cause a **compile-time error**.
> The generated service discards any return value, so a non-void factory would silently never publish its event.

**Example — type-level `@AsyncCommit` (all mutations are async):**

```java
@Aggregate
@AsyncCommit
public record Order(
        @Id Reference<Order> id,
        String customerId,
        String status
) {
    @Create
    public static void create(@NotNull String customerId) {
        PublishesEvents.publishEvent(new OrderPlaced(Reference.create(), customerId));
    }

    @Update
    public void cancel() {
        PublishesEvents.publishEvent(new OrderCancelled(id));
    }

    @EventHandler
    public static Order onOrderPlaced(OrderPlaced event) {
        return new Order(event.id(), event.customerId(), "PLACED");
    }

    @EventHandler
    @ByReference
    public Order onOrderCancelled(OrderCancelled event) {
        return new Order(id, customerId, "CANCELLED");
    }
}
```

**Example — method-level `@AsyncCommit` (only `@Create` is async, `@Update` is synchronous):**

```java
@Aggregate
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        String customerId,
        String status
) {
    @Create
    @AsyncCommit
    public static void create(@NotNull String customerId) {
        PublishesEvents.publishEvent(new OrderPlaced(Reference.create(), customerId));
    }

    @Update
    public Order complete() {          // synchronous — returns 200 OK and calls repository.save()
        return new Order(id, version, customerId, "COMPLETED");
    }

    @EventHandler
    public static Order onOrderPlaced(OrderPlaced event) {
        return new Order(event.id(), 0L, event.customerId(), "PLACED");
    }
}
```

---

### `@CustomType`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`
**Retention:** `RUNTIME`

Opts a type out of Prefab's automatic field-mapping. Fields of this type are skipped during:
- Database migration script generation (no column generated)
- Avro schema generation (field omitted)

REST responses still include the field; Jackson handles serialization normally.

**Attributes:** None

To include a `@CustomType` field in migrations or Avro schemas, implement a `PrefabPlugin` and override
`dataTypeOf()`, `avroSchemaOf()`, `toAvroValueOf()`, and `fromAvroValueOf()`.

---

## 4.2 REST Annotations

All REST annotations are in package `be.appify.prefab.core.annotations.rest` with `@Retention(SOURCE)`.

### `@Create`

**Target:** `CONSTRUCTOR`, `METHOD`

Exposes a constructor (synchronous) or static factory method (`@AsyncCommit`) as an HTTP create endpoint.

| Attribute  | Type        | Default                             | Description                                        |
|------------|-------------|-------------------------------------|----------------------------------------------------|
| `method`   | `String`    | `"POST"`                            | HTTP method. Use constants from `HttpMethod`.      |
| `path`     | `String`    | `""`                                | Path suffix appended to the aggregate's base path. |
| `security` | `@Security` | `@Security` (enabled, no authority) | Security settings.                                 |

**Generated endpoint:** `POST /orders` → creates the aggregate, returns `201 Created` with location header.

```java
@Create
public Order(String customerName) {
    this(Reference.create(), 0L, customerName);
}
```

---

### `@Update`

**Target:** `METHOD`

Exposes an instance method as an HTTP update endpoint.

| Attribute  | Type        | Default     | Description                                                                     |
|------------|-------------|-------------|---------------------------------------------------------------------------------|
| `method`   | `String`    | `"PUT"`     | HTTP method.                                                                    |
| `path`     | `String`    | `""`        | Path suffix appended after `/{id}`. E.g. `"/lines"` → `PUT /orders/{id}/lines`. |
| `security` | `@Security` | `@Security` | Security settings.                                                              |

The method may either return `void` (mutable record update via field replacement) or return a new
instance of the aggregate (immutable pattern). Parameters become the request body fields.

```java
@Update(path = "/lines", method = "POST")
public void addLine(String product, double quantity, double price) {
    lines.add(new Line(product, quantity, price));
}

// Or immutable:
@Update
public Order update(String customerName) {
    return new Order(id, version, customerName);
}
```

---

### Create-or-Update (Upsert) Pattern

When a `@Create` constructor and an `@Update` method share the same HTTP method and effective URL path,
the framework generates a **single combined endpoint** instead of separate create and update endpoints.

**Pairing rule:** The `@Create.path` must start with a path variable segment (e.g. `/{id}`) and the
remainder must equal `@Update.path`. Both must use the same HTTP method.

The generated service:
1. Calls `repository.findById(lookupVariable)` using the path variable from the URL.
2. If found → delegates to the `@Update` method and saves.
3. If not found → calls the `@Create` constructor with the path variable + request body and saves.
4. Always returns `201 Created` with the resource location.

```java
@Aggregate
public record Product(
        @Id String id,
        @Version long version,
        String name,
        String price
) {
    @Create(method = HttpMethod.PUT, path = "/{id}")
    public Product(String id, String name, String price) {
        this(id, 0L, name, price);
    }

    @Update(method = HttpMethod.PUT)
    public Product update(String name, String price) {
        return new Product(id, version, name, price);
    }
}
```

This generates a single `PUT /products/{id}` endpoint. The `@Create` constructor must accept the path
variable (`id` here) as one of its parameters.

---

### `@Delete`

**Target:** `TYPE`, `METHOD`

Exposes a delete endpoint. On a type, performs a plain delete. On a method, the method runs first
(e.g. to publish an event) before the aggregate is deleted.

| Attribute  | Type        | Default     | Description                 |
|------------|-------------|-------------|-----------------------------|
| `method`   | `String`    | `"DELETE"`  | HTTP method.                |
| `path`     | `String`    | `"/{id}"`   | Full path for the endpoint. |
| `security` | `@Security` | `@Security` | Security settings.          |

```java
@Delete
public void delete() {
    publish(new OrderDeleted(id));
}
```

---

### `@GetById`

**Target:** `TYPE`

Exposes a get-by-ID endpoint.

| Attribute  | Type        | Default     | Description        |
|------------|-------------|-------------|--------------------|
| `method`   | `String`    | `"GET"`     | HTTP method.       |
| `path`     | `String`    | `"/{id}"`   | Full path.         |
| `security` | `@Security` | `@Security` | Security settings. |

```java
@Aggregate
@GetById
public record Order(...) { }
```

**Generated endpoint:** `GET /orders/{id}` → returns `OrderResponse` or `404 Not Found`.

---

### `@GetList`

**Target:** `TYPE`

Exposes a paginated list endpoint. Combine with `@Filter` on fields to enable filtering.

| Attribute  | Type        | Default     | Description                                       |
|------------|-------------|-------------|---------------------------------------------------|
| `method`   | `String`    | `"GET"`     | HTTP method.                                      |
| `path`     | `String`    | `""`        | Path suffix (defaults to base path of aggregate). |
| `security` | `@Security` | `@Security` | Security settings.                                |

```java
@Aggregate
@GetList
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        @Filter String customerName
) { }
```

**Generated endpoint:** `GET /orders?page=0&size=20&customerName=foo` → returns `Page<OrderResponse>`.

---

### `@Filter`

**Target:** `FIELD`, `RECORD_COMPONENT`
**Repeatable:** Yes (container: `@Filters`)

Enables filtering on a field in the `@GetList` endpoint.

| Attribute    | Type              | Default    | Description                                    |
|--------------|-------------------|------------|------------------------------------------------|
| `operator`   | `Filter.Operator` | `CONTAINS` | Filter comparison operator.                    |
| `ignoreCase` | `boolean`         | `true`     | Whether to ignore case for string comparisons. |

**Operators:**

| Operator        | SQL equivalent     | Description            |
|-----------------|--------------------|------------------------|
| `EQUAL`         | `= :value`         | Exact match            |
| `CONTAINS`      | `ILIKE '%:value%'` | Contains substring     |
| `STARTS_WITH`   | `ILIKE ':value%'`  | Starts with            |
| `ENDS_WITH`     | `ILIKE '%:value'`  | Ends with              |
| `MATCHES_REGEX` | `~ :value`         | PostgreSQL regex match |

```java
@Filter(operator = Filter.Operator.EQUAL)
@Filter(operator = Filter.Operator.CONTAINS)
String name;
```

---

### `@Download`

**Target:** `FIELD` (must be of type `Binary`)

Exposes a download endpoint for a `Binary` field.

| Attribute  | Type        | Default     | Description        |
|------------|-------------|-------------|--------------------|
| `security` | `@Security` | `@Security` | Security settings. |

```java
@Download
Binary attachment;
```

**Generated endpoint:** `GET /orders/{id}/attachment` → streams the file with content-type header.

---

### `@Streaming`

**Target:** `METHOD`
**Package:** `be.appify.prefab.core.annotations.rest`
**Retention:** `SOURCE`

Generates a Server-Sent Events (SSE) streaming endpoint for an aggregate method. Two usage models
are supported:

- **Pull model**: annotate an instance method returning `Stream<T>` or `Flux<T>`. The processor
  generates a controller endpoint that consumes the stream on a virtual thread (for `Stream<T>`) or
  via reactive subscribe (for `Flux<T>`) and forwards each element as an SSE frame.
- **Push model**: annotate an `@EventHandler @ByReference` method. Each time the event fires for a
  given aggregate instance, the event payload is pushed to any SSE client currently connected for
  that instance via a generated `{Aggregate}SseRegistry`.

| Attribute          | Type        | Default     | Description                                                                                                                                                    |
|--------------------|-------------|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `path`             | `String`    | `"/stream"` | Path suffix appended after `/{id}`. E.g. `"/stream"` → `GET /sessions/{id}/stream`.                                                                            |
| `event`            | `String`    | `"message"` | SSE event name sent in the `event:` field of each SSE frame.                                                                                                   |
| `heartbeatSeconds` | `int`       | `15`        | Interval between keepalive `event: ping` frames. `0` disables heartbeat.                                                                                       |
| `terminal`         | `String`    | `""`        | Push model only: name of a `boolean` field on the event record. When `true`, the SSE stream is closed after the final frame. Empty string disables auto-close. |
| `security`         | `@Security` | `@Security` | Security settings for the generated SSE connect endpoint.                                                                                                      |

See [Feature Guides — SSE Streaming](feature-guides.md#711-sse-streaming) for full examples.

---

### `@Parent`

**Target:** `FIELD`, `METHOD`

Marks a `Reference` field as the parent aggregate. The parent ID is included in every REST path for
this aggregate. Use when modelling nested sub-resources.

**Attributes:** None

```java
@Aggregate
@GetList
@GetById
public record OrderLine(
        @Id Reference<OrderLine> id,
        @Version long version,
        @Parent Reference<Order> order,
        String product,
        int quantity
) { }
```

**Generated base path:** `/orders/{orderId}/order-lines`

---

### `@Security`

**Target:** (annotation attribute only — not placed directly on types)

Used as the value of the `security` attribute on `@Create`, `@Update`, `@Delete`, `@GetById`,
`@GetList`, `@Download`, `@Streaming`.

| Attribute   | Type      | Default | Description                                |
|-------------|-----------|---------|--------------------------------------------|
| `enabled`   | `boolean` | `true`  | Whether Spring Security is enforced.       |
| `authority` | `String`  | `""`    | Required Spring Security authority (role). |

```java
@Create(security = @Security(authority = "ROLE_ADMIN"))
public Order(String customerName) { ... }
```

---

## 4.3 Event Annotations

### `@Event`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`
**Retention:** `CLASS`

Marks a record or interface as a domain event.

`@Event` metadata is retained in bytecode so consuming modules can generate subscribers for handlers whose
event types are declared in dependency modules.

| Attribute       | Type                  | Default          | Description                                                    |
|-----------------|-----------------------|------------------|----------------------------------------------------------------|
| `topic`         | `String`              | — **(required)** | Messaging topic name. Supports Spring property placeholders.   |
| `platform`      | `Event.Platform`      | `DERIVED`        | Messaging platform. Auto-detected when only one is configured. |
| `serialization` | `Event.Serialization` | `JSON`           | Serialization format (`JSON` or `AVRO`).                       |

**Platforms:**

| Value     | Module Required  | Description                  |
|-----------|------------------|------------------------------|
| `DERIVED` | —                | Auto-detected from classpath |
| `KAFKA`   | `prefab-kafka`   | Apache Kafka                 |
| `PUB_SUB` | `prefab-pubsub`  | Google Cloud Pub/Sub         |
| `SNS_SQS` | `prefab-sns-sqs` | AWS SNS/SQS                  |

**Generated artefacts:**
- `{Type}Producer` / `{EventInterface}Producer` — Spring `@Component` that publishes to the topic
- `{Type}KafkaEventTypeRegistrar` — registers the event type with `KafkaJsonTypeResolver`
- Consumer class registered in the messaging platform subscriber
- `SerializationRegistryConfiguration` — registers topic → serialization format mapping

```java
@Event(topic = "${topics.order.name}")
public record OrderCreated(
        @PartitioningKey Reference<Order> id,
        String customerName
) { }

// Sealed interface with multiple sub-events:
@Event(topic = "${topics.user.name}")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public sealed interface UserEvent permits UserEvent.Created, UserEvent.Updated {
    @PartitioningKey
    Reference<User> reference();

    record Created(Reference<User> reference, String name) implements UserEvent { }
    record Updated(Reference<User> reference, String name) implements UserEvent { }
}
```

---

### `@Avsc`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE` (interface)
**Retention:** `CLASS`

AVSC-first event generation. Must be combined with `@Event(serialization = AVRO)` on the same interface.

| Attribute | Type       | Default          | Description                                            |
|-----------|------------|------------------|--------------------------------------------------------|
| `value`   | `String[]` | — **(required)** | One or more classpath-relative paths to `.avsc` files. |

See [Feature Guides — Avro / AVSC-first Events](feature-guides.md#74-avro--avsc-first-events) for full details.

```java
@Event(topic = "sale", serialization = Event.Serialization.AVRO)
@Avsc({"avro/sale-created.avsc", "avro/sale-paid.avsc"})
public sealed interface SaleEvent permits SaleCreated, SalePaid { }
```

---

### `@AvroSchema`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`
**Retention:** `SOURCE`

Overrides the Avro schema `name` and/or `namespace` for a generated record or enum type. Emitted
automatically by AVSC-first generation when the original Avro name or namespace differs from the Java
type name or package name.

| Attribute   | Type     | Default | Description                                                                           |
|-------------|----------|---------|---------------------------------------------------------------------------------------|
| `name`      | `String` | `""`    | Avro schema name. Set when the Avro name differs from the capitalised Java type name. |
| `namespace` | `String` | `""`    | Avro namespace. Set when it differs from the Java package name.                       |

---

### `@PartitioningKey`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`, `METHOD`
**Retention:** `SOURCE`

Marks the field that determines which partition (Kafka) or ordering key (Pub/Sub, SQS) an event is
routed to. Ensures that events for the same entity arrive in order.

**Attributes:** None

```java
@Event(topic = "orders")
public record OrderCreated(
        @PartitioningKey Reference<Order> id,
        String customerName
) { }
```

---

## 4.4 Event Handler Annotations

### `@EventHandler`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `METHOD`
**Retention:** `SOURCE`

Marks a method to process a domain event. The method parameter type determines which event type it handles.

| Attribute | Type       | Default      | Description                                                                            |
|-----------|------------|--------------|----------------------------------------------------------------------------------------|
| `value`   | `Class<?>` | `void.class` | Aggregate class whose service this handler merges into (for cross-aggregate handlers). |

**Variants:**

| Placement                           | Rules                                                            | Behaviour                                                                |
|-------------------------------------|------------------------------------------------------------------|--------------------------------------------------------------------------|
| **Static method** on aggregate      | `public static`, returns aggregate type or `Optional<Aggregate>` | Creates a new aggregate from the event                                   |
| **Instance method** on aggregate    | Instance method                                                  | Updates existing aggregate (combine with `@ByReference` or `@Multicast`) |
| **Instance method** on `@Component` | Class must have `@Component`                                     | Injected as service dependency; called directly                          |

```java
// Static (create pattern):
@EventHandler
public static Order onOrderCreated(OrderCreated event) {
    return new Order(event.id(), 0L, event.customerName());
}

// Instance + @ByReference (update pattern):
@EventHandler
@ByReference(property = "orderId")
public void onOrderPaid(OrderPaid event) {
    this.status = Status.PAID;
}
```

---

### `@EventHandlerConfig`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`

Configures dead-lettering and retry behaviour for all `@EventHandler` methods on the annotated class.

| Attribute              | Type      | Default                                            | Description                                                                 |
|------------------------|-----------|----------------------------------------------------|-----------------------------------------------------------------------------|
| `concurrency`          | `String`  | `"1"`                                              | Number of parallel consumer threads. Supports Spring property placeholders. |
| `deadLetteringEnabled` | `boolean` | `true`                                             | Whether to enable dead-letter routing on failure.                           |
| `deadLetterTopic`      | `String`  | `"${prefab.dlt.topic.name}"`                       | Dead-letter topic.                                                          |
| `retryLimit`           | `String`  | `"${prefab.dlt.retries.limit:5}"`                  | Max retry attempts before dead-lettering.                                   |
| `minimumBackoffMs`     | `String`  | `"${prefab.dlt.retries.minimum-backoff-ms:1000}"`  | Min retry delay (ms).                                                       |
| `maximumBackoffMs`     | `String`  | `"${prefab.dlt.retries.maximum-backoff-ms:30000}"` | Max retry delay (ms).                                                       |
| `backoffMultiplier`    | `String`  | `"${prefab.dlt.retries.backoff-multiplier:1.5}"`   | Exponential backoff multiplier.                                             |
| `autoOffsetReset`      | `String`  | `""`                                                 | Kafka-only listener override for `auto.offset.reset`; supports literals and placeholders. |

```java
@Aggregate
@EventHandlerConfig(concurrency = "4", retryLimit = "3")
public record Channel(...) {
    @EventHandler
    @ByReference(property = "channel")
    public void onMessageSent(MessageSent event) { ... }
}
```

When `autoOffsetReset` is set, generated Kafka listeners add
`@KafkaListener(properties = "auto.offset.reset=...")`. This per-listener setting takes
precedence over `spring.kafka.consumer.auto-offset-reset`.

---

### `@ByReference`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `METHOD`
**Retention:** `SOURCE`

Used on an instance `@EventHandler` method to specify which field on the event holds the reference to the
aggregate to update.

| Attribute  | Type     | Default | Description                                                                                         |
|------------|----------|---------|-----------------------------------------------------------------------------------------------------|
| `property` | `String` | `""`    | Name of the event field of type `Reference<Aggregate>`. If empty, uses the default reference field. |

```java
@EventHandler
@ByReference(property = "channel")
public void onMessageSent(MessageSent event) {
    messageCount++;
}
```

---

### `@Multicast`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `METHOD`
**Retention:** `SOURCE`

Used on an instance `@EventHandler` method to deliver an event to **multiple** aggregate instances fetched
by a repository query.

| Attribute     | Type       | Default          | Description                                                         |
|---------------|------------|------------------|---------------------------------------------------------------------|
| `queryMethod` | `String`   | — **(required)** | Name of the repository method that fetches the target aggregates.   |
| `parameters`  | `String[]` | `{}`             | Event field names mapped to the query method parameters (in order). |

If no aggregates are found, `IllegalStateException` is thrown to trigger retry.

```java
@EventHandler
@Multicast(queryMethod = "findByChannel", parameters = "channel")
public ChannelSummary onMessageSent(MessageSent event) {
    return new ChannelSummary(id, version, channel, name, totalMessages + 1, totalSubscribers);
}
```

---

## 4.5 Database Annotations

### `@DbMigration`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`
**Retention:** `SOURCE`

Controls database migration script generation.

| Attribute | Type      | Default | Description                                                         |
|-----------|-----------|---------|---------------------------------------------------------------------|
| `enabled` | `boolean` | `true`  | Set to `false` to suppress migration generation for this aggregate. |

```java
@Aggregate
@DbMigration(enabled = false)
public record ExternalData(...) { }
```

---

### `@DbDocument`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`
**Retention:** `RUNTIME`

Stores a field as a JSONB column in PostgreSQL instead of a separate table or VARCHAR column.
A GIN index and expression indexes are automatically generated.

**Database mapping:**
- Column type: `JSONB`
- Index: `GIN` index on the column + expression indexes for searched fields

```java
@DbDocument List<Tag> tags
```

---

### `@DbDefaultValue`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`
**Retention:** `SOURCE`

Sets a database-level default value on the generated column.

| Attribute | Type     | Description                                                          |
|-----------|----------|----------------------------------------------------------------------|
| `value`   | `String` | SQL default value expression (e.g. `"0"`, `"NOW()"`, `"'PENDING'"`) |

```java
@DbDefaultValue("'PENDING'")
Status status;
```

---

### `@DbRename`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`
**Retention:** `SOURCE`

Generates a migration script that renames the column from `oldName` to the current field name.

| Attribute | Type     | Description                           |
|-----------|----------|---------------------------------------|
| `oldName` | `String` | Previous column name in the database. |

```java
@DbRename(oldName = "full_name")
String customerName;
```

---

### `@Indexed`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`
**Retention:** `SOURCE`

Creates a database index on the corresponding column.

| Attribute | Type      | Default | Description                         |
|-----------|-----------|---------|-------------------------------------|
| `unique`  | `boolean` | `false` | Whether to create a `UNIQUE` index. |

Indexes are also created automatically for `@Filter`-annotated fields and foreign key columns.

```java
@Indexed(unique = true)
String email;
```

---

### `@Text`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`, `RECORD_COMPONENT`
**Retention:** `SOURCE`

Maps a `String` field to an unbounded `TEXT` column (instead of the default `VARCHAR(255)`).

Use Jakarta Validation's `@Size(max = N)` to generate `VARCHAR(N)`.

```java
@Text
String description;

@Size(max = 500)
String summary;  // generates VARCHAR(500)
```

---

### `@DbColumn`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`
**Retention:** `RUNTIME`

Declares a **custom SQL column type** for an aggregate field, bypassing Prefab's built-in type validation.

Use this annotation when a field's Java type is not in Prefab's supported set — for example, `float[]`,
custom value types, or PostgreSQL extension types such as `vector(N)` (pgvector), PostGIS geometry,
or `hstore`.

| Attribute   | Type      | Default      | Description                                                                                  |
|-------------|-----------|--------------|----------------------------------------------------------------------------------------------|
| `type`      | `String`  | *(required)* | Exact SQL column type in the generated DDL (e.g. `"vector(1536)"`, `"geometry(Point,4326)"`)|
| `converter` | `Class<?>`| `void.class` | Optional converter class auto-registered with `JdbcCustomConversions`.                       |

**Behaviour:**
- The annotation processor accepts the field **without** throwing `IllegalArgumentException`, regardless of Java type.
- When `@DbMigration` is enabled, the value of `type()` is emitted **verbatim** in the generated `CREATE TABLE` or `ALTER TABLE` statement.
- When `converter()` is specified (non-void), the class is instantiated and registered automatically as a `JdbcCustomConversions` contributor. The converter must have a public no-argument constructor.
- A compile-time error is emitted if `type()` is blank.

**Example — pgvector embedding:**

```java
@Aggregate
public record MemoryEntry(
        @Id Reference<MemoryEntry> id,
        @Version long version,
        String content,
        @DbColumn(type = "vector(1536)", converter = FloatArrayToVectorConverter.class)
        float[] embedding
) { }
```

The generated DDL will contain:

```sql
CREATE TABLE "memory_entry" (
    "id"        VARCHAR (255) NOT NULL,
    "version"   BIGINT        NOT NULL,
    "content"   VARCHAR (255) NOT NULL,
    "embedding" vector(1536)  NOT NULL,
    PRIMARY KEY ("id")
);
```

---

### `@Doc`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`

Documents an aggregate or event type. Used by AsyncAPI / OpenAPI documentation generators.

---

### `@Example`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`

Provides example values for documentation generation.

---

### `@MongoMigration` *(deprecated)*

Use `@DbMigration` instead. `@DbMigration` works transparently for both SQL and MongoDB backends.

---

## 4.6 Audit Annotations

All audit annotations are in `be.appify.prefab.core.annotations.audit` with `@Retention(RUNTIME)`.

### `@CreatedAt`

**Target:** `FIELD`

Field type must be `Instant`. Populated once on creation; never overwritten on update.

### `@CreatedBy`

**Target:** `FIELD`

Field type must be `String`. Populated with `AuditContextProvider.currentUserId()` on creation only.

### `@LastModifiedAt`

**Target:** `FIELD`

Field type must be `Instant`. Updated on every write (create and update).

### `@LastModifiedBy`

**Target:** `FIELD`

Field type must be `String`. Updated with `AuditContextProvider.currentUserId()` on every write.

**Convenience alternative:** Use `AuditInfo` as a single field instead of the four annotations individually.

```java
@Aggregate
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        String customerName,
        AuditInfo audit           // groups all four audit fields
) { }
```

---

## 4.7 Multi-tenancy Annotations

### `@TenantId`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`
**Retention:** `RUNTIME`

Marks the tenant discriminator field. Must be of type `String` or `Reference`.

**Behaviour:**
- Populated from `TenantContextProvider.currentTenantId()` on every write
- Added as `WHERE tenantField = :tenantId` to every read (GetById, GetList, Update, Delete)
- Generates a `NOT NULL` column with a database index

**Rules:**
- Only one `@TenantId` field per aggregate
- Must not appear in any `@Create` constructor or `@Update` method parameters

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
        this(Reference.create(), 0L, null, name);
    }
}
```

---

## 4.8 Validation Annotations

These annotations are in `be.appify.prefab.core.annotations.validation` and are used on `Binary` fields.

### `@ContentType`

**Target:** `FIELD`

Restricts the allowed MIME content types for a `Binary` upload.

| Attribute | Type       | Description                                              |
|-----------|------------|----------------------------------------------------------|
| `value`   | `String[]` | Allowed MIME types (e.g. `{"image/jpeg", "image/png"}`). |

```java
@ContentType({"image/jpeg", "image/png", "image/gif"})
Binary profilePicture;
```

---

### `@FileSize`

**Target:** `FIELD`

Restricts the maximum file size for a `Binary` upload.

| Attribute | Type   | Description                 |
|-----------|--------|-----------------------------|
| `max`     | `long` | Maximum file size in bytes. |

```java
@FileSize(max = 5 * 1024 * 1024)  // 5 MB
Binary document;
```

---

## 4.9 Extension Annotations

### `@RepositoryMixin`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE` (interface)
**Retention:** `RUNTIME`

Marks an interface as a repository mixin. The interface is added as a super-interface of the generated
repository for the specified aggregate.

| Attribute | Type       | Description                   |
|-----------|------------|-------------------------------|
| `value`   | `Class<?>` | The aggregate type to extend. |

See [Feature Guides — Repository Mixins](feature-guides.md#710-repository-mixins) for a full example.

---

## Quick Reference Table

| Annotation            | Target                    | Retention | Purpose                                                                                                                                                                                                                                                               |
|-----------------------|---------------------------|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@Aggregate`          | Type                      | RUNTIME   | Marks an aggregate root                                                                                                                                                                                                                                               |
| `@AsyncCommit`        | Type, Constructor, Method | SOURCE    | Async-commit (listen-to-self) pattern. At **type level**: all `@Create` and `@Update` methods become async (202, no `repository.save()`). At **method level**: only that method is async. `@Create` methods must be `void` and call `PublishesEvents.publishEvent()`. |
| `@CustomType`         | Type                      | RUNTIME   | Opt out of automatic field mapping                                                                                                                                                                                                                                    |
| `@Create`             | Constructor, Method       | SOURCE    | HTTP create endpoint                                                                                                                                                                                                                                                  |
| `@Update`             | Method                    | SOURCE    | HTTP update endpoint                                                                                                                                                                                                                                                  |
| `@Delete`             | Type, Method              | SOURCE    | HTTP delete endpoint                                                                                                                                                                                                                                                  |
| `@GetById`            | Type                      | SOURCE    | HTTP get-by-ID endpoint                                                                                                                                                                                                                                               |
| `@GetList`            | Type                      | SOURCE    | HTTP paginated list endpoint                                                                                                                                                                                                                                          |
| `@Filter`             | Field                     | SOURCE    | Enable filtering on `@GetList`                                                                                                                                                                                                                                        |
| `@Download`           | Field                     | SOURCE    | HTTP binary download endpoint                                                                                                                                                                                                                                         |
| `@Streaming`          | Method                    | SOURCE    | SSE streaming endpoint                                                                                                                                                                                                                                                |
| `@Parent`             | Field, Method             | SOURCE    | Parent aggregate reference for nested paths                                                                                                                                                                                                                           |
| `@Security`           | (attribute only)          | SOURCE    | Security settings for REST endpoints                                                                                                                                                                                                                                  |
| `@Event`              | Type                      | CLASS     | Domain event for a messaging topic                                                                                                                                                                                                                                    |
| `@Avsc`               | Type                      | CLASS     | AVSC-first event generation                                                                                                                                                                                                                                           |
| `@AvroSchema`         | Type                      | SOURCE    | Override Avro schema name/namespace                                                                                                                                                                                                                                   |
| `@PartitioningKey`    | Field, Method             | SOURCE    | Event partitioning/ordering key                                                                                                                                                                                                                                       |
| `@EventHandler`       | Method                    | SOURCE    | Processes a domain event                                                                                                                                                                                                                                              |
| `@EventHandlerConfig` | Type                      | —         | Dead-letter and retry configuration                                                                                                                                                                                                                                   |
| `@ByReference`        | Method                    | SOURCE    | Event handler — update by reference                                                                                                                                                                                                                                   |
| `@Multicast`          | Method                    | SOURCE    | Event handler — broadcast to many                                                                                                                                                                                                                                     |
| `@DbMigration`        | Type                      | SOURCE    | Control migration script generation                                                                                                                                                                                                                                   |
| `@DbDocument`         | Field                     | RUNTIME   | Store field as JSONB                                                                                                                                                                                                                                                  |
| `@DbColumn`           | Field, Record Component   | RUNTIME   | Custom SQL column type; bypasses built-in type validation                                                                                                                                                                                                             |
| `@DbDefaultValue`     | Field                     | SOURCE    | Database column default value                                                                                                                                                                                                                                         |
| `@DbRename`           | Field                     | SOURCE    | Generate rename migration                                                                                                                                                                                                                                             |
| `@Indexed`            | Field                     | SOURCE    | Create a database index                                                                                                                                                                                                                                               |
| `@Text`               | Field                     | SOURCE    | Map `String` to `TEXT` column                                                                                                                                                                                                                                         |
| `@CreatedAt`          | Field                     | RUNTIME   | Audit: creation timestamp                                                                                                                                                                                                                                             |
| `@CreatedBy`          | Field                     | RUNTIME   | Audit: creator user ID                                                                                                                                                                                                                                                |
| `@LastModifiedAt`     | Field                     | RUNTIME   | Audit: last-modified timestamp                                                                                                                                                                                                                                        |
| `@LastModifiedBy`     | Field                     | RUNTIME   | Audit: last-modifier user ID                                                                                                                                                                                                                                          |
| `@TenantId`           | Field                     | RUNTIME   | Multi-tenancy discriminator                                                                                                                                                                                                                                           |
| `@RepositoryMixin`    | Type                      | RUNTIME   | Add custom query methods to repository                                                                                                                                                                                                                                |
| `@ContentType`        | Field                     | —         | Allowed MIME types for `Binary` uploads                                                                                                                                                                                                                               |
| `@FileSize`           | Field                     | —         | Maximum file size for `Binary` uploads                                                                                                                                                                                                                                |

