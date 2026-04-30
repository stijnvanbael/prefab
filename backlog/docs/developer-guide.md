# Prefab Developer Guide

**Version:** 0.7.x | **Framework:** Spring Boot 4.x | **Java:** 21+

This is the single authoritative reference for Prefab. It covers every annotation, built-in type, module,
generated artefact, extension point, and configuration option. Both human developers and LLMs should treat
this document as the primary source of truth for Prefab behaviour.

> **Living document rule:** Any agent or developer that changes or adds a Prefab feature **must** update the
> relevant section of this guide in the same commit/PR. See [AGENTS.md](../../AGENTS.md) for the full rule.

---

## Table of Contents

1. [What is Prefab?](#1-what-is-prefab)
2. [Module Dependency Matrix](#2-module-dependency-matrix)
3. [Getting Started](#3-getting-started)
4. [Annotation Reference](#4-annotation-reference)
   - [4.1 Aggregate Annotations](#41-aggregate-annotations)
   - [4.2 REST Annotations](#42-rest-annotations)
   - [4.3 Event Annotations](#43-event-annotations)
   - [4.4 Event Handler Annotations](#44-event-handler-annotations)
   - [4.5 Database Annotations](#45-database-annotations)
   - [4.6 Audit Annotations](#46-audit-annotations)
   - [4.7 Multi-tenancy Annotations](#47-multi-tenancy-annotations)
   - [4.8 Validation Annotations](#48-validation-annotations)
   - [4.9 Extension Annotations](#49-extension-annotations)
5. [Built-in Types](#5-built-in-types)
6. [Generated Artefacts](#6-generated-artefacts)
   - [6.1 Controller](#61-controller)
   - [6.2 Service](#62-service)
   - [6.3 Repository](#63-repository)
   - [6.4 Request/Response Records](#64-requestresponse-records)
   - [6.5 Event Consumer](#65-event-consumer)
   - [6.6 Database Migration Scripts](#66-database-migration-scripts)
7. [Feature Guides](#7-feature-guides)
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
8. [Extension Point Guide](#8-extension-point-guide)
9. [Configuration Reference](#9-configuration-reference)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. What is Prefab?

Prefab is an annotation-driven code-generation framework for Spring Boot applications built around the
**Aggregate Root** pattern. You write a plain Java record annotated with a handful of Prefab annotations;
the annotation processor generates a fully-wired Spring MVC controller, service, Spring Data repository,
request/response DTOs, event consumer, and database migration scripts at compile time.

### Core Concepts

| Concept | Description |
|---------|-------------|
| **Aggregate Root** | A Java record annotated with `@Aggregate`. The single, consistent unit of data in the domain. |
| **Event** | A Java record (or sealed interface) annotated with `@Event`. Published to a messaging platform. |
| **Event Handler** | A method annotated with `@EventHandler`. Processes events to create or update aggregates. |
| **Repository Mixin** | An interface annotated with `@RepositoryMixin`. Adds custom query methods to generated repositories. |
| **Plugin** | Implements `PrefabPlugin` and is registered via `META-INF/services`. Participates in code generation. |

---

## 2. Module Dependency Matrix

### Prefab Modules

| Module | Artifact ID | Required? | Description |
|--------|-------------|-----------|-------------|
| `core` | `prefab-core` | **Always** | Framework types, annotations, interfaces |
| `annotation-processor` | `prefab-annotation-processor` | **Always** | Compile-time code generator (APT) |
| `postgres` | `prefab-postgres` | PostgreSQL | Spring Data JDBC + Flyway + PostgreSQL support |
| `mongodb` | `prefab-mongodb` | MongoDB | Spring Data MongoDB support |
| `kafka` | `prefab-kafka` | Kafka events | Kafka producer/consumer configuration |
| `pubsub` | `prefab-pubsub` | GCP Pub/Sub | Google Cloud Pub/Sub support |
| `sns-sqs` | `prefab-sns-sqs` | AWS SNS/SQS | AWS SNS publisher + SQS consumer support |
| `avro` | `prefab-avro` | Avro serialization | Avro serialization/deserialization support |
| `avro-processor` | `prefab-avro-processor` | AVSC-first events | AVSC schema → Java record code generation |
| `security` | `prefab-security` | Security | Spring Security + OAuth2 integration |
| `openapi` | `prefab-openapi` | OpenAPI docs | SpringDoc OpenAPI / Swagger UI |
| `async-api` | `prefab-async-api` | AsyncAPI docs | AsyncAPI specification generation |
| `test` | `prefab-test` | Testing | Testcontainers-based integration test support |
| `terraform` | `prefab-terraform` | GCP infra | GCP Terraform configuration generation |

### Feature → Module Mapping

| Feature | Minimum Required Modules |
|---------|-------------------------|
| REST CRUD (PostgreSQL) | `core`, `annotation-processor`, `postgres` |
| REST CRUD (MongoDB) | `core`, `annotation-processor`, `mongodb` |
| Kafka JSON events | `core`, `annotation-processor`, `kafka` |
| Kafka Avro events | `core`, `annotation-processor`, `kafka`, `avro` |
| Kafka AVSC-first events | `core`, `annotation-processor`, `kafka`, `avro`, `avro-processor` |
| GCP Pub/Sub events | `core`, `annotation-processor`, `pubsub` |
| AWS SNS/SQS events | `core`, `annotation-processor`, `sns-sqs` |
| Audit trail | `core` (no extra module needed) |
| Multi-tenancy | `core` (no extra module needed) |
| Binary uploads | `core`, `annotation-processor`, persistence module |
| OpenAPI documentation | `core`, `annotation-processor`, `openapi` |
| AsyncAPI documentation | `core`, `annotation-processor`, `async-api` |
| Integration testing | `test`, plus one or more messaging modules |

### Maven Dependency Snippet

```xml
<!-- Always required -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-core</artifactId>
    <version>${prefab.version}</version>
</dependency>
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-annotation-processor</artifactId>
    <version>${prefab.version}</version>
    <scope>provided</scope>
</dependency>

<!-- PostgreSQL persistence -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-postgres</artifactId>
    <version>${prefab.version}</version>
</dependency>

<!-- Kafka events -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-kafka</artifactId>
    <version>${prefab.version}</version>
</dependency>
```

---

## 3. Getting Started

### Spring Boot Application Setup

1. Add `@EnablePrefab` to your main application class:

```java
@SpringBootApplication
@EnablePrefab
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

`@EnablePrefab` imports:
- `PrefabCoreConfiguration` – component scan for core beans
- `AuditConfiguration` – registers default `AuditContextProvider`
- `KafkaConfiguration` – Kafka serializer/deserializer (if Kafka is on classpath)
- `PubSubConfiguration` – Pub/Sub support (if GCP library is on classpath)
- `SnsConfiguration` – SNS/SQS support (if AWS library is on classpath)
- `SerializationRegistry` – topic-to-serialization-format registry

2. Write your first aggregate:

```java
@Aggregate
@GetById
@GetList
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        String name,
        double price
) {
    @Create
    public Product(String name, double price) {
        this(Reference.create(), 0L, name, price);
    }

    @Update
    public Product update(String name, double price) {
        return new Product(id, version, name, price);
    }

    @Delete
    public void delete() { }
}
```

This single class causes Prefab to generate: a Spring MVC controller with four endpoints, a service, a
Spring Data JDBC repository, request/response records, and a Flyway migration script.

---

## 4. Annotation Reference

### 4.1 Aggregate Annotations

#### `@Aggregate`

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

#### `@AsyncCommit`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`, `CONSTRUCTOR`, `METHOD`
**Retention:** `SOURCE`

Marks an aggregate or individual method to use the **async-commit (listen-to-self)** pattern. Instead
of persisting synchronously, the create/update method publishes an event and returns `202 Accepted`.
An `@EventHandler` on the same aggregate then persists it when the event arrives.

**Attributes:** None

**Behaviour:**
- On `@Create` static factory method: must return the event type; generates `202 Accepted`
- On `@Update` void method: method must call `publish()` internally; generates `202 Accepted`
- `@EventHandler` methods on an `@AsyncCommit` aggregate receive a deduplication guard in the generated consumer

**Example:**

```java
@Aggregate
@AsyncCommit
public record Order(
        @Id Reference<Order> id,
        String customerId,
        String status
) {
    @Create
    public static OrderPlaced create(@NotNull String customerId) {
        return new OrderPlaced(Reference.create(), customerId);
    }

    @EventHandler
    public static Order onOrderPlaced(OrderPlaced event) {
        return new Order(event.id(), event.customerId(), "PLACED");
    }
}
```

---

#### `@CustomType`

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

### 4.2 REST Annotations

All REST annotations are in package `be.appify.prefab.core.annotations.rest` with `@Retention(SOURCE)`.

#### `@Create`

**Target:** `CONSTRUCTOR`, `METHOD`

Exposes a constructor (synchronous) or static factory method (`@AsyncCommit`) as an HTTP create endpoint.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `method` | `String` | `"POST"` | HTTP method. Use constants from `HttpMethod`. |
| `path` | `String` | `""` | Path suffix appended to the aggregate's base path. |
| `security` | `@Security` | `@Security` (enabled, no authority) | Security settings. |

**Generated endpoint:** `POST /orders` → creates the aggregate, returns `201 Created` with location header.

```java
@Create
public Order(String customerName) {
    this(Reference.create(), 0L, customerName);
}
```

---

#### `@Update`

**Target:** `METHOD`

Exposes an instance method as an HTTP update endpoint.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `method` | `String` | `"PUT"` | HTTP method. |
| `path` | `String` | `""` | Path suffix appended after `/{id}`. E.g. `"/lines"` → `PUT /orders/{id}/lines`. |
| `security` | `@Security` | `@Security` | Security settings. |

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

#### `@Delete`

**Target:** `TYPE`, `METHOD`

Exposes a delete endpoint. On a type, performs a plain delete. On a method, the method runs first
(e.g. to publish an event) before the aggregate is deleted.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `method` | `String` | `"DELETE"` | HTTP method. |
| `path` | `String` | `"/{id}"` | Full path for the endpoint. |
| `security` | `@Security` | `@Security` | Security settings. |

```java
@Delete
public void delete() {
    publish(new OrderDeleted(id));
}
```

---

#### `@GetById`

**Target:** `TYPE`

Exposes a get-by-ID endpoint.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `method` | `String` | `"GET"` | HTTP method. |
| `path` | `String` | `"/{id}"` | Full path. |
| `security` | `@Security` | `@Security` | Security settings. |

```java
@Aggregate
@GetById
public record Order(...) { }
```

**Generated endpoint:** `GET /orders/{id}` → returns `OrderResponse` or `404 Not Found`.

---

#### `@GetList`

**Target:** `TYPE`

Exposes a paginated list endpoint. Combine with `@Filter` on fields to enable filtering.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `method` | `String` | `"GET"` | HTTP method. |
| `path` | `String` | `""` | Path suffix (defaults to base path of aggregate). |
| `security` | `@Security` | `@Security` | Security settings. |

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

#### `@Filter`

**Target:** `FIELD`
**Repeatable:** Yes (container: `@Filters`)

Enables filtering on a field in the `@GetList` endpoint.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `operator` | `Filter.Operator` | `CONTAINS` | Filter comparison operator. |
| `ignoreCase` | `boolean` | `true` | Whether to ignore case for string comparisons. |

**Operators:**

| Operator | SQL equivalent | Description |
|----------|---------------|-------------|
| `EQUAL` | `= :value` | Exact match |
| `CONTAINS` | `ILIKE '%:value%'` | Contains substring |
| `STARTS_WITH` | `ILIKE ':value%'` | Starts with |
| `ENDS_WITH` | `ILIKE '%:value'` | Ends with |
| `MATCHES_REGEX` | `~ :value` | PostgreSQL regex match |

```java
@Filter(operator = Filter.Operator.EQUAL)
@Filter(operator = Filter.Operator.CONTAINS)
String name;
```

---

#### `@Download`

**Target:** `FIELD` (must be of type `Binary`)

Exposes a download endpoint for a `Binary` field.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `security` | `@Security` | `@Security` | Security settings. |

```java
@Download
Binary attachment;
```

**Generated endpoint:** `GET /orders/{id}/attachment` → streams the file with content-type header.

---

#### `@Parent`

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

#### `@Security`

**Target:** (annotation attribute only — not placed directly on types)

Used as the value of the `security` attribute on `@Create`, `@Update`, `@Delete`, `@GetById`,
`@GetList`, `@Download`.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `enabled` | `boolean` | `true` | Whether Spring Security is enforced. |
| `authority` | `String` | `""` | Required Spring Security authority (role). |

```java
@Create(security = @Security(authority = "ROLE_ADMIN"))
public Order(String customerName) { ... }
```

---

### 4.3 Event Annotations

#### `@Event`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`
**Retention:** `SOURCE`

Marks a record or interface as a domain event.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `topic` | `String` | — **(required)** | Messaging topic name. Supports Spring property placeholders. |
| `platform` | `Event.Platform` | `DERIVED` | Messaging platform. Auto-detected when only one is configured. |
| `serialization` | `Event.Serialization` | `JSON` | Serialization format (`JSON` or `AVRO`). |

**Platforms:**

| Value | Module Required | Description |
|-------|----------------|-------------|
| `DERIVED` | — | Auto-detected from classpath |
| `KAFKA` | `prefab-kafka` | Apache Kafka |
| `PUB_SUB` | `prefab-pubsub` | Google Cloud Pub/Sub |
| `SNS_SQS` | `prefab-sns-sqs` | AWS SNS/SQS |

**Generated artefacts:**
- `{Type}Producer` / `{EventInterface}Producer` — Spring `@Component` that publishes to the topic
- Consumer class registered in the messaging platform subscriber (Kafka listener, Pub/Sub subscriber, SQS listener)
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

#### `@Avsc`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE` (interface)
**Retention:** `SOURCE`

AVSC-first event generation. Must be combined with `@Event(serialization = AVRO)` on the same interface.
The processor reads each AVSC file from the classpath and generates a corresponding Java record.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `value` | `String[]` | — **(required)** | One or more classpath-relative paths to `.avsc` files. |

**Generated artefacts:** One Java record per `.avsc` file, placed in the package defined by the schema's
`namespace` field. All generated records implement the annotated interface.

```java
@Event(topic = "sale", serialization = Event.Serialization.AVRO)
@Avsc({"avro/sale-created.avsc", "avro/sale-paid.avsc"})
public sealed interface SaleEvent permits SaleCreated, SalePaid { }
```

---

#### `@PartitioningKey`

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

### 4.4 Event Handler Annotations

#### `@EventHandler`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `METHOD`
**Retention:** `SOURCE`

Marks a method to process a domain event. The method parameter type determines which event type it handles.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `value` | `Class<?>` | `void.class` | Aggregate class whose service this handler merges into (for cross-aggregate handlers). |

**Variants:**

| Placement | Rules | Behaviour |
|-----------|-------|-----------|
| **Static method** on aggregate | `public static`, returns aggregate type or `Optional<Aggregate>` | Creates a new aggregate from the event |
| **Instance method** on aggregate | Instance method | Updates existing aggregate (combine with `@ByReference` or `@Multicast`) |
| **Instance method** on `@Component` | Class must have `@Component` | Injected as service dependency; called directly |

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

#### `@EventHandlerConfig`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`

Configures dead-lettering and retry behaviour for all `@EventHandler` methods on the annotated class.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `concurrency` | `String` | `"1"` | Number of parallel consumer threads. Supports Spring property placeholders. |
| `deadLetteringEnabled` | `boolean` | `true` | Whether to enable dead-letter routing on failure. |
| `deadLetterTopic` | `String` | `"${prefab.dlt.topic.name}"` | Dead-letter topic. |
| `retryLimit` | `String` | `"${prefab.dlt.retries.limit:5}"` | Max retry attempts before dead-lettering. |
| `minimumBackoffMs` | `String` | `"${prefab.dlt.retries.minimum-backoff-ms:1000}"` | Min retry delay (ms). |
| `maximumBackoffMs` | `String` | `"${prefab.dlt.retries.maximum-backoff-ms:30000}"` | Max retry delay (ms). |
| `backoffMultiplier` | `String` | `"${prefab.dlt.retries.backoff-multiplier:1.5}"` | Exponential backoff multiplier. |

```java
@Aggregate
@EventHandlerConfig(concurrency = "4", retryLimit = "3")
public record Channel(...) {
    @EventHandler
    @ByReference(property = "channel")
    public void onMessageSent(MessageSent event) { ... }
}
```

---

#### `@ByReference`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `METHOD`
**Retention:** `SOURCE`

Used on an instance `@EventHandler` method to specify which field on the event holds the reference to the
aggregate to update.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `property` | `String` | `""` | Name of the event field of type `Reference<Aggregate>`. If empty, uses the default reference field. |

```java
@EventHandler
@ByReference(property = "channel")
public void onMessageSent(MessageSent event) {
    messageCount++;
}
```

---

#### `@Multicast`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `METHOD`
**Retention:** `SOURCE`

Used on an instance `@EventHandler` method to deliver an event to **multiple** aggregate instances fetched
by a repository query.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `queryMethod` | `String` | — **(required)** | Name of the repository method that fetches the target aggregates. |
| `parameters` | `String[]` | `{}` | Event field names mapped to the query method parameters (in order). |

If no aggregates are found, `IllegalStateException` is thrown to trigger retry.

```java
@EventHandler
@Multicast(queryMethod = "findByChannel", parameters = "channel")
public ChannelSummary onMessageSent(MessageSent event) {
    return new ChannelSummary(id, version, channel, name, totalMessages + 1, totalSubscribers);
}
```

---

### 4.5 Database Annotations

#### `@DbMigration`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`
**Retention:** `SOURCE`

Controls database migration script generation.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `enabled` | `boolean` | `true` | Set to `false` to suppress migration generation for this aggregate. |

By default, a migration script is generated for every `@Aggregate`. Use `@DbMigration(enabled = false)`
to opt out (e.g. for aggregates managed by external tools or backed by views).

```java
@Aggregate
@DbMigration(enabled = false)
public record ExternalData(...) { }
```

---

#### `@DbDocument`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`
**Retention:** `RUNTIME`

Stores a field as a JSONB column in PostgreSQL instead of a separate table or VARCHAR column.
A GIN index and expression indexes are automatically generated.

**Attributes:** None

```java
@Aggregate
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @DbDocument List<Tag> tags
) { }
```

**Database mapping:**
- Column type: `JSONB`
- Index: `GIN` index on the column + expression indexes for searched fields

---

#### `@DbDefaultValue`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`
**Retention:** `SOURCE`

Sets a database-level default value on the generated column.

| Attribute | Type | Description |
|-----------|------|-------------|
| `value` | `String` | SQL default value expression (e.g. `"0"`, `"NOW()"`, `"'PENDING'"`) |

```java
@DbDefaultValue("'PENDING'")
Status status;
```

---

#### `@DbRename`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`
**Retention:** `SOURCE`

Generates a migration script that renames the column from `oldName` to the current field name.

| Attribute | Type | Description |
|-----------|------|-------------|
| `oldName` | `String` | Previous column name in the database. |

```java
@DbRename(oldName = "full_name")
String customerName;
```

---

#### `@Indexed`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`
**Retention:** `SOURCE`

Creates a database index on the corresponding column.

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `unique` | `boolean` | `false` | Whether to create a `UNIQUE` index. |

Indexes are also created automatically for `@Filter`-annotated fields and foreign key columns.

```java
@Indexed(unique = true)
String email;
```

---

#### `@Text`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `FIELD`, `RECORD_COMPONENT`
**Retention:** `SOURCE`

Maps a `String` field to an unbounded `TEXT` column (instead of the default `VARCHAR(255)`).

**Attributes:** None

Use Jakarta Validation's `@Size(max = N)` to generate `VARCHAR(N)`.

```java
@Text
String description;

@Size(max = 500)
String summary;  // generates VARCHAR(500)
```

---

#### `@Doc`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`

Documents an aggregate or event type. Used by AsyncAPI / OpenAPI documentation generators.

---

#### `@Example`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE`

Provides example values for documentation generation.

---

#### `@MongoMigration` *(deprecated)*

Use `@DbMigration` instead. `@DbMigration` works transparently for both SQL and MongoDB backends.

---

### 4.6 Audit Annotations

All audit annotations are in `be.appify.prefab.core.annotations.audit` with `@Retention(RUNTIME)`.

#### `@CreatedAt`

**Target:** `FIELD`

Field type must be `Instant`. Populated once on creation; never overwritten on update.

#### `@CreatedBy`

**Target:** `FIELD`

Field type must be `String`. Populated with `AuditContextProvider.currentUserId()` on creation only.

#### `@LastModifiedAt`

**Target:** `FIELD`

Field type must be `Instant`. Updated on every write (create and update).

#### `@LastModifiedBy`

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

### 4.7 Multi-tenancy Annotations

#### `@TenantId`

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

### 4.8 Validation Annotations

These annotations are in `be.appify.prefab.core.annotations.validation` and are used on `Binary` fields.

#### `@ContentType`

**Target:** `FIELD`

Restricts the allowed MIME content types for a `Binary` upload.

| Attribute | Type | Description |
|-----------|------|-------------|
| `value` | `String[]` | Allowed MIME types (e.g. `{"image/jpeg", "image/png"}`). |

```java
@ContentType({"image/jpeg", "image/png", "image/gif"})
Binary profilePicture;
```

---

#### `@FileSize`

**Target:** `FIELD`

Restricts the maximum file size for a `Binary` upload.

| Attribute | Type | Description |
|-----------|------|-------------|
| `max` | `long` | Maximum file size in bytes. |

```java
@FileSize(max = 5 * 1024 * 1024)  // 5 MB
Binary document;
```

---

### 4.9 Extension Annotations

#### `@RepositoryMixin`

**Package:** `be.appify.prefab.core.annotations`
**Target:** `TYPE` (interface)
**Retention:** `RUNTIME`

Marks an interface as a repository mixin. The interface is added as a super-interface of the generated
repository for the specified aggregate.

| Attribute | Type | Description |
|-----------|------|-------------|
| `value` | `Class<?>` | The aggregate type to extend. |

See [7.10 Repository Mixins](#710-repository-mixins) for a full example.

---

## 5. Built-in Types

### `Reference<T>`

**Package:** `be.appify.prefab.core.service`

A typed wrapper around a string ID. Used as the `@Id` field and for cross-aggregate references.

```java
public record Reference<T>(@JsonValue String id) {
    public static <T> Reference<T> create()           // random UUID
    public static <T> Reference<T> fromId(String id)  // from known ID
}
```

**Usage:**
```java
@Id Reference<Order> id           // primary key
Reference<Customer> customer      // foreign key to Customer aggregate
```

**Database mapping:** Stored as `VARCHAR(36)` (UUID string).
**JSON mapping:** Serialized as a plain string (not an object).

---

### `Binary`

**Package:** `be.appify.prefab.core.domain`

Represents an uploaded file.

```java
public record Binary(
        String name,          // original filename
        String contentType,   // MIME type (e.g. "image/jpeg")
        File data             // temporary file on disk
) { }
```

**Usage:** Use as a field type and annotate with `@Download` to generate a download endpoint.
Combine with `@ContentType` and `@FileSize` for validation.

**REST mapping:** Multipart form upload. The processor generates a `multipart/form-data` controller
parameter and a `StorageService` call to persist the file.

---

### `AuditInfo`

**Package:** `be.appify.prefab.core.audit`

Convenience record grouping the four audit fields.

```java
public record AuditInfo(
        @CreatedAt Instant createdAt,
        @CreatedBy String createdBy,
        @LastModifiedAt Instant lastModifiedAt,
        @LastModifiedBy String lastModifiedBy
) { }
```

**Usage:** Declare as a single field on the aggregate instead of the four individual fields.

**Database mapping:** Four separate columns: `created_at`, `created_by`, `last_modified_at`,
`last_modified_by`.

---

### `Page<T>`

**Package:** `be.appify.prefab.core.spring`

The return type of `@GetList` endpoints. Implements `org.springframework.data.domain.Page<T>`.

```java
public record Page<T>(
        List<T> content,
        PageInfo page
) { }

record PageInfo(int size, int number, long totalElements, int totalPages) { }
```

**Query parameters accepted by generated list endpoints:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | `int` | `0` | Zero-based page number |
| `size` | `int` | `20` | Page size |
| Filter fields | `String` | — | One parameter per `@Filter`-annotated field |

---

### `AuditContextProvider`

**Package:** `be.appify.prefab.core.audit`

Strategy interface for resolving the current user's identity.

```java
public interface AuditContextProvider {
    String currentUserId();
}
```

**Default:** `SystemAuditContextProvider` returns `"system"`.

**Override:** Declare a `@Bean` of type `AuditContextProvider`:

```java
@Bean
public AuditContextProvider auditContextProvider() {
    return () -> SecurityContextHolder.getContext()
        .getAuthentication().getName();
}
```

---

### `TenantContextProvider`

**Package:** `be.appify.prefab.core.tenant`

Strategy interface for resolving the current tenant ID.

```java
public interface TenantContextProvider {
    String currentTenantId();  // return null to disable tenant filtering
}
```

**Default:** No-op bean that returns `null` (no tenant filtering).

**Override:**

```java
@Component
public class JwtTenantContextProvider implements TenantContextProvider {
    @Override
    public String currentTenantId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getClaimAsString("organisation_id");
        }
        return null;
    }
}
```

---

### `PublishesEvents`

**Package:** `be.appify.prefab.core.domain`

Implement on an aggregate to gain the `publish(Object event)` convenience method.

```java
public interface PublishesEvents {
    default void publish(Object event) { ... }
}
```

Internally delegates to `DomainEventPublisher`, which is wired by `SpringDomainEventPublisher` to
publish Spring application events that are then forwarded to the configured messaging platform.

---

### `SerializationRegistry`

**Package:** `be.appify.prefab.core.util`

Spring `@Component` that maps topic names to their serialization format. Prefab generates a
`SerializationRegistryConfiguration` bean for each `@Event`-annotated type; you do not normally
interact with this directly.

---

### Exception Types

| Class | HTTP Status | Package |
|-------|-------------|---------|
| `BadRequestException` | 400 | `be.appify.prefab.core.problem` |
| `NotFoundException` | 404 | `be.appify.prefab.core.problem` |
| `ConflictException` | 409 | `be.appify.prefab.core.problem` |

Prefab's generated services throw `NotFoundException` when an aggregate is not found by ID.

---

## 6. Generated Artefacts

For an aggregate `Order` in package `com.example.order`, Prefab generates the following classes in
`com.example.order` (unless otherwise noted):

### 6.1 Controller

**Class:** `OrderController`
**Annotations:** `@RestController`, `@RequestMapping("/orders")`, `@Tag` (OpenAPI)

Generated methods for each REST annotation:

| Annotation | Method | Endpoint | Request | Response |
|------------|--------|----------|---------|----------|
| `@Create` | `create(CreateOrderRequest)` | `POST /orders` | `CreateOrderRequest` body | `201 Created` + `Location` header |
| `@Update` | `update{Method}(String id, Update{Method}OrderRequest)` | `PUT /orders/{id}` | `Update{Method}OrderRequest` body | `200 OK` + `OrderResponse` |
| `@Delete` | `delete(String id)` | `DELETE /orders/{id}` | — | `204 No Content` |
| `@GetById` | `getById(String id)` | `GET /orders/{id}` | — | `200 OK` + `OrderResponse` |
| `@GetList` | `list(Pageable, filter params)` | `GET /orders` | Query params | `200 OK` + `Page<OrderResponse>` |
| `@Download` | `download{Field}(String id)` | `GET /orders/{id}/{field}` | — | Binary stream |

**Security:** Each method is annotated with `@PreAuthorize` if `security.enabled = true`.

---

### 6.2 Service

**Class:** `OrderService`
**Annotations:** `@Service`

The service contains all business logic:
- Delegates to `OrderRepository` for persistence
- Calls audit/tenant population helpers
- Executes `@EventHandler` logic
- Publishes domain events via `DomainEventPublisher`
- Throws `NotFoundException` for missing aggregates
- Throws `ConflictException` on optimistic lock failures (`@Version` mismatch)

---

### 6.3 Repository

**Class:** `OrderRepository` (interface)
**Extends:** `CrudRepository<Order, String>`, `PagingAndSortingRepository<Order, String>`, plus any `@RepositoryMixin` interfaces

The repository is a Spring Data interface. Prefab generates custom query methods for `@Filter` fields
and `@Multicast` event handlers.

---

### 6.4 Request/Response Records

For each `@Create` constructor and `@Update` method, one request record is generated:

- `CreateOrderRequest` — fields from the `@Create` constructor parameters
- `UpdateOrderRequest` / `AddOrderLineRequest` — fields from each `@Update` method parameters

One response record:

- `OrderResponse` — all fields of the aggregate, with `Reference<T>` serialized as `String`

Nested value objects (inner records) are also represented as nested response records.

---

### 6.5 Event Consumer

When an aggregate has `@EventHandler` methods, one consumer class is generated per messaging platform:

| Platform | Generated Class | Type |
|----------|----------------|------|
| Kafka | `{Aggregate}EventConsumer` | `@KafkaListener` |
| Pub/Sub | `{Aggregate}PubSubSubscriber` | `MessageReceiver` |
| SNS/SQS | `{Aggregate}SqsConsumer` | `@SqsListener` |

The consumer:
- Deserializes the event from the topic
- Routes to the correct `@EventHandler` method in the service
- Handles retries and dead-lettering according to `@EventHandlerConfig`
- Generates per-topic executor fields to prevent cross-topic deadlocks

---

### 6.6 Database Migration Scripts

#### PostgreSQL (Flyway)

**Location:** `src/main/resources/db/migration/V{n}__{aggregate_snake_case}.sql` (e.g. `V001__order.sql`)

Generated SQL:

```sql
CREATE TABLE IF NOT EXISTS order (
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    version    BIGINT       NOT NULL,
    customer_name VARCHAR(255),
    created_at TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_order_customer_name ON order (customer_name);
```

Column type mapping:

| Java Type | SQL Type | Notes |
|-----------|----------|-------|
| `String` | `VARCHAR(255)` | Default |
| `String` + `@Text` | `TEXT` | Unbounded |
| `String` + `@Size(max=N)` | `VARCHAR(N)` | |
| `int` / `Integer` | `INT` | |
| `long` / `Long` | `BIGINT` | |
| `double` / `Double` | `DOUBLE PRECISION` | |
| `boolean` / `Boolean` | `BOOLEAN` | |
| `Instant` | `TIMESTAMP` | |
| `LocalDate` | `DATE` | |
| `Reference<T>` | `VARCHAR(36)` | Foreign key |
| `List<X>` (value type) | Array column | PostgreSQL array |
| `List<X>` + `@DbDocument` | `JSONB` | |
| `enum` | `VARCHAR(255)` | Enum name |
| `Binary` | Omitted | Stored externally |

---

## 7. Feature Guides

### 7.1 REST CRUD Operations

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
    public void delete() { }
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

### 7.2 Event Publishing

Any aggregate that `implements PublishesEvents` can call `publish(event)` from constructors or update methods.

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
) { }
```

---

### 7.3 Event Handling

Three patterns for handling domain events:

#### Pattern 1 — Static Handler (Create from event)

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

#### Pattern 2 — By-Reference Handler (Update via event)

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

#### Pattern 3 — Multicast Handler (Broadcast to many)

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

#### Create-or-Update Pattern

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

### 7.4 Avro / AVSC-first Events

#### Inline Avro (Java-first)

Define event records with `@Event(serialization = AVRO)`. The processor generates Avro schema and
converters automatically.

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
        @PartitioningKey Reference<Customer> id();
    }

    public record Created(Reference<Customer> id, String name) implements Events { }
}
```

#### AVSC-first

Place `.avsc` files on the classpath and use `@Avsc` + `@Event`:

```java
// src/main/resources/avro/sale-created.avsc
// { "type": "record", "name": "SaleCreated", "namespace": "be.example.sale", "fields": [...] }

@Event(topic = "sale", serialization = Event.Serialization.AVRO)
@Avsc({"avro/sale-created.avsc", "avro/sale-paid.avsc"})
public sealed interface SaleEvent permits SaleCreated, SalePaid { }
```

The processor generates `SaleCreated` and `SalePaid` records in the `be.example.sale` package.

---

### 7.5 Audit Trail

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

### 7.6 Multi-tenancy

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

### 7.7 Binary / File Fields

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

### 7.8 Async Commit Pattern

Use when you need at-least-once delivery semantics for aggregate creation via an event broker:

```java
@Aggregate
@AsyncCommit
@GetById
public record Order(
        @Id Reference<Order> id,
        String customerId,
        String status
) {
    // REST call publishes event and returns 202
    @Create
    public static OrderPlaced create(@NotNull String customerId) {
        return new OrderPlaced(Reference.create(), customerId);
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
) { }
```

---

### 7.9 Nested Value Objects and Embedded Types

Prefab supports nested Java records as value objects embedded in the aggregate:

```java
public record ProductDetails(
        String brand,
        String model,
        String colour
) { }

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

### 7.10 Repository Mixins

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

## 8. Extension Point Guide

### 8.1 PrefabPlugin Interface

All Prefab code generation is driven by `PrefabPlugin` implementations loaded via the Java `ServiceLoader`
(see `META-INF/services/be.appify.prefab.processor.PrefabPlugin`).

To create a custom plugin:

1. Implement `be.appify.prefab.processor.PrefabPlugin`
2. Register it in `META-INF/services/be.appify.prefab.processor.PrefabPlugin`

```java
package com.example.processor;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.TypeSpec;

public class MyCustomPlugin implements PrefabPlugin {

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        // Add custom methods to the generated controller
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        // Add custom methods to the generated service
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        // Generate entirely new source files
    }
}
```

**`META-INF/services/be.appify.prefab.processor.PrefabPlugin`:**
```
com.example.processor.MyCustomPlugin
```

### PrefabPlugin Callback Methods

| Method | When Called | Purpose |
|--------|-------------|---------|
| `initContext(PrefabContext)` | Once at startup | Inject processing environment |
| `writeController(manifest, builder)` | Per aggregate | Add methods to controller |
| `writeService(manifest, builder)` | Per aggregate | Add methods to service |
| `writeRepository(manifest, builder)` | Per aggregate | Add methods to repository |
| `writeTestClient(manifest, builder)` | Per aggregate | Add methods to test REST client |
| `writeAdditionalFiles(manifests)` | Once, after all aggregates | Generate extra source files |
| `writeGlobalFiles(manifests, polymorphicManifests)` | Once, all rounds done | Generate files spanning all aggregates |
| `writeEventFiles()` | Round 1 only | Generate event types (before aggregate code) |
| `getServiceDependencies(manifest)` | Per aggregate | Add Spring beans injected into service |
| `requestBodyParameter(parameter)` | Per method parameter | Override how a parameter maps to request body |
| `mapRequestParameter(parameter)` | Per method parameter | Override how a request param maps to domain type |
| `dataTypeOf(typeManifest)` | Per `@CustomType` field | Provide SQL column type for custom types |
| `avroSchemaOf(typeManifest)` | Per `@CustomType` field | Provide Avro schema for custom types |
| `toAvroValueOf(type, value)` | Per `@CustomType` field | Serialize custom type to Avro |
| `fromAvroValueOf(type, value)` | Per `@CustomType` field | Deserialize Avro to custom type |

### Built-in Plugins

The following plugins are included in `prefab-annotation-processor`:

| Plugin | Handles |
|--------|---------|
| `DbMigrationPlugin` | `@DbMigration` → Flyway SQL scripts |
| `MongoMigrationPlugin` | `@DbMigration` on MongoDB → JS migration scripts |
| `MongoIndexPlugin` | `@Indexed` for MongoDB |
| `SerializationPlugin` | `@Event` → `SerializationRegistryConfiguration` |
| `EventSchemaDocumentationPlugin` | `@Event` → AsyncAPI schema |
| `StaticEventHandlerPlugin` | Static `@EventHandler` methods |
| `ByReferenceEventHandlerPlugin` | `@ByReference` event handlers |
| `MulticastEventHandlerPlugin` | `@Multicast` event handlers |
| `CreatePlugin` | `@Create` → controller + service create method |
| `GetByIdPlugin` | `@GetById` → controller + service getById method |
| `DeletePlugin` | `@Delete` → controller + service delete method |
| `GetListPlugin` | `@GetList` + `@Filter` → controller + service list method |
| `UpdatePlugin` | `@Update` → controller + service update method |
| `BinaryPlugin` | `Binary` fields + `@Download` |
| `AggregateParameterPlugin` | Handles `@Aggregate`-typed method parameters |
| `TenantPlugin` | `@TenantId` → tenant filtering code |
| `AuditPlugin` | Audit field population code |
| `MotherPlugin` | Test object mother generation |

### 8.2 Repository Mixins

See [7.10 Repository Mixins](#710-repository-mixins). This is the simplest extension point for adding
custom queries without writing a plugin.

### 8.3 AuditContextProvider

Implement and register as a `@Bean` to integrate audit with your authentication mechanism. See
[5. Built-in Types → AuditContextProvider](#auditcontextprovider).

### 8.4 TenantContextProvider

Implement and register as a `@Component` or `@Bean` to provide the tenant ID. See
[5. Built-in Types → TenantContextProvider](#tenantcontextprovider).

### 8.5 SerializationRegistryCustomizer

Implement to programmatically register topic → serialization format mappings:

```java
@Bean
public SerializationRegistryCustomizer myCustomizer() {
    return registry -> registry.register("my-topic", Event.Serialization.AVRO);
}
```

---

## 9. Configuration Reference

### Application Properties

| Property | Default | Description |
|----------|---------|-------------|
| `prefab.dlt.topic.name` | — | Dead-letter topic name (required if dead-lettering is enabled) |
| `prefab.dlt.retries.limit` | `5` | Default max retries before dead-lettering |
| `prefab.dlt.retries.minimum-backoff-ms` | `1000` | Default minimum retry backoff (ms) |
| `prefab.dlt.retries.maximum-backoff-ms` | `30000` | Default maximum retry backoff (ms) |
| `prefab.dlt.retries.backoff-multiplier` | `1.5` | Default exponential backoff multiplier |

### Kafka Configuration

Prefab uses standard Spring Kafka properties (`spring.kafka.*`). The `KafkaConfiguration` class provides
dynamic JSON serializers/deserializers via `DynamicSerializer` and `DynamicDeserializer` that look up the
correct Avro or JSON schema from `SerializationRegistry`.

### Pub/Sub Configuration

Configure via `spring.cloud.gcp.pubsub.*` (standard Spring Cloud GCP properties). Prefab's
`PubSubConfiguration` auto-configures subscription and publishing beans.

### SNS/SQS Configuration

Configure via `spring.cloud.aws.*` (standard Spring Cloud AWS properties). Prefab's `SnsConfiguration`
auto-configures SNS publisher and SQS listener beans.

### `@EnablePrefab`

Add to your main application class. Imports all Prefab core beans. No attributes.

---

## 10. Troubleshooting

### Error: `No serialization format registered for topic [X]`

**Cause:** An event is being consumed or produced but the topic has not been registered in
`SerializationRegistry`.

**Fix:** Ensure the `@Event`-annotated class is in a package scanned by the annotation processor and
that the `SerializationRegistryConfiguration` generated bean is loaded (check component scanning).

---

### Error: `[X] was not found` (HTTP 404)

**Cause:** `NotFoundException` is thrown when `findById(id)` returns empty.

**Fix:** Verify the `id` path parameter is correct. Check that the aggregate was created and the migration
ran. For multi-tenant setups, verify the `@TenantId` field matches the current tenant.

---

### Error: `OptimisticLockingFailureException` (HTTP 409)

**Cause:** Two concurrent requests updated the same aggregate simultaneously. The `@Version` field
mismatch triggers `ConflictException`.

**Fix:** Retry the request with the latest `version` value from a fresh `GET` response.

---

### Error: `Compilation error: @TenantId field must not appear in @Create or @Update parameters`

**Cause:** A `@TenantId` field was included in a `@Create` constructor or `@Update` method.

**Fix:** Remove the `@TenantId` field from constructor/method parameters. The generated service populates
it automatically from `TenantContextProvider`.

---

### Error: `Compilation error: Cannot map event property to query parameter`

**Cause:** A `@Multicast` `parameters` value references a field that does not exist on the event.

**Fix:** Verify the `parameters` values match the event record field names exactly (case-sensitive).

---

### Error: `IllegalStateException` during event handling (causes retry)

**Cause:** A `@Multicast` handler found no aggregates to update. Prefab throws `IllegalStateException`
intentionally to trigger retry (the target aggregate may not have been created yet due to event ordering).

**Fix:** This is expected behaviour. Ensure the dead-letter configuration is set so that events that
permanently fail are routed to the DLT rather than blocking the consumer.

---

### Binary upload returns `400 Bad Request` with content-type error

**Cause:** The uploaded file's MIME type is not in the `@ContentType` whitelist.

**Fix:** Either use an allowed MIME type, or expand the `@ContentType` values on the field.

---

### Generated migration script conflicts with existing schema

**Cause:** A column was renamed without `@DbRename`, or a field was removed without a migration.

**Fix:** Use `@DbRename(oldName = "old_column")` when renaming fields, and write manual migration
scripts for removals.

---

### Kafka consumer deadlock (events for one topic block another)

**Cause:** A retry loop on topic A occupies all threads, preventing consumption from topic B.

**Fix:** This is handled automatically by Prefab's per-topic executor strategy in
`PubSubSubscriberWriter`. For Kafka, ensure `@EventHandlerConfig(concurrency = ...)` is set high enough
and that the dead-letter configuration is correct so retries do not block indefinitely.

---

### Maven annotation processor not running (no generated sources)

**Cause:** The annotation processor dependency is missing or not on the `provided` scope.

**Fix:** Ensure `prefab-annotation-processor` is declared with `<scope>provided</scope>` in `pom.xml`.
Verify `maven.compiler.release` is set to `21`. Run `mvn clean compile` to force regeneration.

---

## Appendix: Annotation Quick Reference

| Annotation | Target | Retention | Purpose |
|------------|--------|-----------|---------|
| `@Aggregate` | Type | RUNTIME | Marks an aggregate root |
| `@AsyncCommit` | Type, Method | SOURCE | Async-commit (listen-to-self) pattern |
| `@CustomType` | Type | RUNTIME | Opt out of automatic field mapping |
| `@Create` | Constructor, Method | SOURCE | HTTP create endpoint |
| `@Update` | Method | SOURCE | HTTP update endpoint |
| `@Delete` | Type, Method | SOURCE | HTTP delete endpoint |
| `@GetById` | Type | SOURCE | HTTP get-by-ID endpoint |
| `@GetList` | Type | SOURCE | HTTP paginated list endpoint |
| `@Filter` | Field | SOURCE | Enable filtering on `@GetList` |
| `@Download` | Field | SOURCE | HTTP binary download endpoint |
| `@Parent` | Field, Method | SOURCE | Parent aggregate reference for nested paths |
| `@Security` | (attribute only) | SOURCE | Security settings for REST endpoints |
| `@Event` | Type | SOURCE | Domain event for a messaging topic |
| `@Avsc` | Type | SOURCE | AVSC-first event generation |
| `@PartitioningKey` | Field, Method | SOURCE | Event partitioning/ordering key |
| `@EventHandler` | Method | SOURCE | Processes a domain event |
| `@EventHandlerConfig` | Type | — | Dead-letter and retry configuration |
| `@ByReference` | Method | SOURCE | Event handler — update by reference |
| `@Multicast` | Method | SOURCE | Event handler — broadcast to many |
| `@DbMigration` | Type | SOURCE | Control migration script generation |
| `@DbDocument` | Field | RUNTIME | Store field as JSONB |
| `@DbDefaultValue` | Field | SOURCE | Database column default value |
| `@DbRename` | Field | SOURCE | Generate rename migration |
| `@Indexed` | Field | SOURCE | Create a database index |
| `@Text` | Field | SOURCE | Map `String` to `TEXT` column |
| `@CreatedAt` | Field | RUNTIME | Audit: creation timestamp |
| `@CreatedBy` | Field | RUNTIME | Audit: creator user ID |
| `@LastModifiedAt` | Field | RUNTIME | Audit: last-modified timestamp |
| `@LastModifiedBy` | Field | RUNTIME | Audit: last-modifier user ID |
| `@TenantId` | Field | RUNTIME | Multi-tenancy discriminator |
| `@RepositoryMixin` | Type | RUNTIME | Add custom query methods to repository |
| `@ContentType` | Field | — | Allowed MIME types for `Binary` uploads |
| `@FileSize` | Field | — | Maximum file size for `Binary` uploads |

---

*This document is generated from the Prefab source code and maintained as a living document.
Any agent or developer changing Prefab behaviour must update the relevant section here.*
