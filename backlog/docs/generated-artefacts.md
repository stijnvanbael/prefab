# Generated Artefacts

**Part of the [Prefab Developer Guide](developer-guide.md)**

For an aggregate `Order` in package `com.example.order`, Prefab generates the following classes in
`com.example.order` (unless otherwise noted):

---

## 6.1 Controller

**Class:** `OrderController`
**Annotations:** `@RestController`, `@RequestMapping("/orders")`, `@Tag` (OpenAPI)

Generated methods for each REST annotation:

| Annotation  | Method                                                  | Endpoint                   | Request                           | Response                          |
|-------------|---------------------------------------------------------|----------------------------|-----------------------------------|-----------------------------------|
| `@Create`   | `create(CreateOrderRequest)`                            | `POST /orders`             | `CreateOrderRequest` body         | `201 Created` + `Location` header |
| `@Update`   | `update{Method}(String id, Update{Method}OrderRequest)` | `PUT /orders/{id}`         | `Update{Method}OrderRequest` body | `200 OK` + `OrderResponse`        |
| `@Delete`   | `delete(String id)`                                     | `DELETE /orders/{id}`      | —                                 | `204 No Content`                  |
| `@GetById`  | `getById(String id)`                                    | `GET /orders/{id}`         | —                                 | `200 OK` + `OrderResponse`        |
| `@GetList`  | `list(Pageable, filter params)`                         | `GET /orders`              | Query params                      | `200 OK` + `Page<OrderResponse>`  |
| `@Download` | `download{Field}(String id)`                            | `GET /orders/{id}/{field}` | —                                 | Binary stream                     |

**Security:** Each method is annotated with `@PreAuthorize` if `security.enabled = true`.

### Multipart requests (`Binary` parameters)

When a `@Create` constructor or `@Update` method has a `Binary` parameter, the endpoint takes
`multipart/form-data` instead of a JSON body. The request has one part per input:

| Part                | Content type                   | Holds                                                              |
|---------------------|--------------------------------|--------------------------------------------------------------------|
| `body`              | `application/json`             | A JSON object with all non-binary parameters (`{"title": "..."}`). |
| one per `Binary`    | the file's own type            | The file, named after the parameter (`file`).                      |

```bash
curl -X POST http://localhost:8080/documents \
  -F 'body={"title": "Contract"};type=application/json' \
  -F 'file=@contract.pdf;type=application/pdf'
```

A plain form field (`-F title=Contract`) is **not** read: the other parameters must be in the `body` JSON part.

`@ContentType` and `@FileSize` on the parameter are copied onto the generated controller parameter, so an upload that
violates them is rejected with `400 Bad Request` before it is stored. A missing part, or a request that is not valid
multipart, also results in `400 Bad Request` (error code `INVALID_MULTIPART_REQUEST` for the latter), not `500`.

---

## 6.2 Service

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

## 6.3 Repository

**Class:** `OrderRepository` (interface)
**Extends:** `CrudRepository<Order, String>`, `PagingAndSortingRepository<Order, String>`, plus any `@RepositoryMixin` interfaces

The repository is a Spring Data interface. Prefab generates custom query methods for `@Filter` fields
and `@Multicast` event handlers.

---

## 6.4 Request/Response Records

For each `@Create` constructor and `@Update` method, one request record is generated:

- `CreateOrderRequest` — fields from the `@Create` constructor parameters
- `UpdateOrderRequest` / `AddOrderLineRequest` — fields from each `@Update` method parameters

One response record:

- `OrderResponse` — all fields of the aggregate, including `@Transient` fields, with `Reference<T>` serialized as
  `String`

Nested value objects (inner records) are also represented as nested response records.

### Copying generated records

Generated records, including REST request and response records, AVSC-generated events and nested
records, union branches, and event snapshots, expose `builder()` and `toBuilder()`. The latter
starts a new builder with the record's current component values:

```java
var updated = request.toBuilder().name("Alice").build();
```

Builder setter names use the `prefab.builder.setterPrefix` annotation-processor option; the default
is empty, so the setter above is `name`. For example, the `with` prefix produces `withName`.
Calling `build()` invokes the record's canonical constructor and its validation. Each call to
`toBuilder()` is independent: list components are copied into new lists so list adders cannot
mutate the original. Other reference components are copied shallowly; nested objects and their
mutable contents are not cloned.

Prefab cannot add methods to handwritten records, including code-first `@Event` records. Their
generated test fixtures may provide standalone builders, but those records do not gain `toBuilder()`.
Sealed interfaces do not have a builder; their generated record implementations do.

`@ByReference` resolves only the named event field or public zero-argument accessor. Unrelated
methods, including `toBuilder()`, are not inspected as reference properties. Processor type
manifests preserve unbounded, upper-bounded and lower-bounded wildcard arguments rather than
erasing them to `Object`.

---

## 6.5 Event Consumer

When an aggregate has `@EventHandler` methods, one consumer class is generated per messaging platform:

| Platform | Generated Class               | Type              |
|----------|-------------------------------|-------------------|
| Kafka    | `{Aggregate}EventConsumer`    | `@KafkaListener`  |
| Pub/Sub  | `{Aggregate}PubSubSubscriber` | `MessageReceiver` |
| SNS/SQS  | `{Aggregate}SqsConsumer`      | `@SqsListener`    |

The consumer:
- Deserializes the event from the topic
- Routes to the correct `@EventHandler` method in the service
- Handles retries and dead-lettering according to `@EventHandlerConfig`
- Generates per-topic executor fields to prevent cross-topic deadlocks
- Ignores messages whose event type is not registered locally and continues processing subsequent known events

For repeatable generic `@Event` declarations, registrars and fixture builders are
generated separately for every concrete binding, including publisher-only events.
Their names append the full SHA-256 digest of the canonical type identity, and member
types are substituted before generation. AsyncAPI components and Avro schemas/converters
also retain independent binding identities; ordinary names remain unchanged.
Generated handlers use concrete parameter types and guard the binding metadata before
dispatch. Pub/Sub subscriptions and SQS queues are named from the complete owner,
canonical type and resolved topic, so different bindings sharing a topic stay isolated.

At startup, configured brokers provision every resolved generic topic and fan-out target.
GCP Terraform emits deduplicated generic topic fragments and exact handler subscription
fragments alongside `pubsub.tf`; separate files allow bindings discovered in later
processing rounds to be included without rewriting existing resources. Generic topic
property/SpEL expressions are provisioned at runtime: Terraform generation emits an
explicit warning rather than embedding unresolved expressions as invalid HCL. Supply
resolved Terraform resources separately when managing those expressions declaratively.

---

## 6.6 Database Migration Scripts

Prefab generates database migration files at compile time; the database-specific lifecycle, ownership rules,
manual migration examples, and deployment/recovery limitations are documented in the
[Database Migration Lifecycle guide](database-migrations.md).

### PostgreSQL (Flyway)

**Generated location:** `db/migration/V{n}__generated.sql` in compiler output (normally
`target/classes/db/migration/`). Copy reviewed migrations to `src/main/resources/db/migration/` to version them in
the application source. Flyway executes pending SQL migrations at application startup.

```sql
CREATE TABLE "order" (
  "id" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "customer_name" VARCHAR (255),
  PRIMARY KEY("id")
);
```

This is representative output; exact columns, types, indexes, and constraints follow the aggregate model. See
[PostgreSQL lifecycle and SQL evolution examples](database-migrations.md#postgresql-generated-sql-and-flyway).

PostgreSQL column type mapping:

| Java Type                 | SQL Type           | Notes             |
|---------------------------|--------------------|-------------------|
| `String`                  | `VARCHAR(255)`     | Default           |
| `String` + `@Text`        | `TEXT`             | Unbounded         |
| `String` + `@Size(max=N)` | `VARCHAR(N)`       |                   |
| `int` / `Integer`         | `INT`              |                   |
| `long` / `Long`           | `BIGINT`           |                   |
| `double` / `Double`       | `DOUBLE PRECISION` |                   |
| `boolean` / `Boolean`     | `BOOLEAN`          |                   |
| `Instant`                 | `TIMESTAMP`        |                   |
| `LocalDate`               | `DATE`             |                   |
| `Reference<T>`            | `VARCHAR(36)`      | Foreign key       |
| `List<X>` (value type)    | Array column       | PostgreSQL array  |
| `List<X>` + `@DbDocument` | `JSONB`            |                   |
| `enum`                    | `VARCHAR(255)`     | Enum name         |
| `Binary`                  | Omitted            | Stored externally |

### MongoDB

**Generated location:** `mongo/migration/V{n}__generated.js` in compiler output. Prefab generates collection and
field rename scripts when `@DbRename` is used; it does not execute them or manage MongoDB migration history.
See the [MongoDB lifecycle](database-migrations.md#mongodb-generated-rename-scripts) for the script format and
operational responsibilities.

Fields annotated with `@Transient` are omitted from generated persistence metadata and migration columns but remain in
REST and event contracts.

---

## 6.7 Event Consumer Assertions

The `EventConsumerWhereStep<V>` interface (returned by `EventConsumerAssert.assertThat(consumer)...within(...)`)
supports two overloads of `where()` for asserting received events:

### Standard `where(Consumer<ListAssert<V>>)`

```java
EventConsumerAssert.assertThat(userConsumer)
    .hasReceivedMessages(1)
    .within(5, TimeUnit.SECONDS)
    .where(events -> events.extracting(UserEvent::name).containsExactly("Alice"));
```

### Custom assertion class `where(Class<A> assertClass, Consumer<A> assertion)`

Instantiates a custom AssertJ assertion class over the received event list. The class must have a
constructor that accepts `List<V>`.

```java
// Custom assertion class
public class UserEventAssert extends AbstractAssert<UserEventAssert, List<UserEvent>> {
    public UserEventAssert(List<UserEvent> events) {
        super(events, UserEventAssert.class);
    }

    public UserEventAssert hasCreatedUserWithName(String name) {
        assertThat(actual)
            .filteredOn(UserEvent.Created.class::isInstance)
            .extracting(e -> ((UserEvent.Created) e).name())
            .contains(name);
        return this;
    }
}

// In test
EventConsumerAssert.assertThat(userConsumer)
    .hasReceivedMessages(1)
    .within(5, TimeUnit.SECONDS)
    .where(UserEventAssert.class, assert_ -> assert_.hasCreatedUserWithName("Alice"));
```

---

## 6.8 Generated Assertion Classes

For each response record, each `@Event`-annotated type in the current compilation, and each nested record,
Prefab generates an AssertJ assertion class in the same package as the type (written to
`target/prefab-test-sources/`).

Prefab does not regenerate event assertion classes for dependency-provided events discovered on the classpath.
This includes events generated upstream from `@Avsc` contracts and consumed from another module's main or test
artifacts.

### `{Type}ResponseAssert`

Generated for every aggregate. Extends `AbstractAssert<SELF, {Type}Response>` with a self-referential
`SELF` type parameter that enables subclassing. Exposes one assertion method per record component,
plus a static `assertThat()` factory method.

- Non-list fields generate `has{FieldName}(FieldType expected)` returning `SELF`.
- List fields generate `has{FieldName}Satisfying(Consumer<ListAssert<ElementType>> requirements)` returning `SELF`.
- The constructor is `protected` to allow subclasses to call `super(actual, MyAssert.class)`.

```java
// Generated: assertion.infrastructure.http.ProductResponseAssert
public class ProductResponseAssert<SELF extends ProductResponseAssert<SELF>>
        extends AbstractAssert<SELF, ProductResponse> {

    public static ProductResponseAssert assertThat(ProductResponse actual) { ... }

    protected ProductResponseAssert(ProductResponse actual) { super(actual, ProductResponseAssert.class); }

    public SELF hasId(String expected) { ... }
    public SELF hasName(String expected) { ... }
    public SELF hasPrice(Double expected) { ... }
    public SELF hasTagsSatisfying(Consumer<ListAssert<String>> requirements) { ... }
}
```

#### Extending a generated assert class

Because every generated assert class is parameterised with `SELF`, you can add domain-specific
assertion methods that return the correct subtype without casting:

```java
public class MyProductResponseAssert
        extends ProductResponseAssert<MyProductResponseAssert> {

    public MyProductResponseAssert(ProductResponse actual) {
        super(actual, MyProductResponseAssert.class);
    }

    /** Custom domain assertion — returns MyProductResponseAssert for fluent chaining. */
    public MyProductResponseAssert isPremium() {
        isNotNull();
        if (actual.price() < 100.0) {
            failWithMessage("Expected product to be premium (price ≥ 100) but price was <%s>", actual.price());
        }
        return myself;
    }
}

// Usage in a test
new MyProductResponseAssert(product)
    .hasName("Luxury Widget")
    .isPremium(); // returns MyProductResponseAssert, no cast needed
```
```

### `{EventName}Assert`

Generated for every `@Event`-annotated record in the current module. Follows the same `SELF`-parameterised
pattern as response assert classes, supporting subclassing and custom assertion methods.

```java
// Generated: com.example.event.OrderCreatedAssert
public class OrderCreatedAssert<SELF extends OrderCreatedAssert<SELF>>
        extends AbstractAssert<SELF, OrderCreated> {

    public static OrderCreatedAssert assertThat(OrderCreated actual) { ... }

    protected OrderCreatedAssert(OrderCreated actual) { ... }

    public SELF hasOrderId(String expected) { ... }
    public SELF hasCustomerName(String expected) { ... }
    public SELF hasItemsSatisfying(Consumer<ListAssert<String>> requirements) { ... }
}
```

### `{NestedType}Assert`

If a response record or event has fields of a record type (multi-field, not a single-value wrapper), an assertion
class is generated recursively for that nested record type as well.

### `Assertions` factory class

One `Assertions` class is generated per package that contains assertion classes:

```java
// Generated: assertion.infrastructure.http.Assertions
public class Assertions {
    private Assertions() {}

    public static ProductResponseAssert assertThat(ProductResponse actual) {
        return ProductResponseAssert.assertThat(actual);
    }
}
```

Use it in tests:

```java
import static assertion.infrastructure.http.Assertions.assertThat;

assertThat(client.getProductById(id))
    .hasName("Widget")
    .hasPrice(9.99)
    .hasTagsSatisfying(list -> list.contains("featured"));
```

---

## 6.9 Generated Test Client

For each aggregate, Prefab generates a `{Aggregate}Client` helper class written to
`target/prefab-test-sources/` in the same package as the aggregate.

```java
// Generated: be.appify.example.ProductClient
public class ProductClient {

    public ProductClient(String baseUrl) { ... }

    public ProductResponse createProduct(CreateProductRequest request) { ... }
    public ProductResponse getProductById(String id) { ... }
    public Page<ProductResponse> listProducts(String nameFilter) { ... }
    public ProductResponse updateProductDetails(String id, UpdateProductDetailsRequest request) { ... }
    public void deleteProduct(String id) { ... }
}
```

### Manual Override (Skip Mechanism)

If you place a hand-crafted version of `{Aggregate}Client.java` under
`src/test/java/<package>/`, Prefab will **not** overwrite it. On every build the processor
checks whether the file already exists under `src/test/java` and, if so, skips generation
and emits an `INFO` note:

```
Prefab: Skipping generation of be.appify.example.ProductClient — manual override found at
        src/test/java/be/appify/example/ProductClient.java
```

> **Note:** The manual override file is never auto-updated by Prefab. If the aggregate's API
> changes (new `@Create`, `@Update`, etc.) you must update the manual file yourself.

---

## 6.11 Overriding a Generated Class (Escape Hatch)

Every class Prefab writes through the annotation-processing `Filer` (controller, service, repository,
request/response records, event consumers, etc.) is **skippable**: place a hand-crafted source file
with the same qualified name under `src/main/java/` and the processor will detect it, emit a compiler
`NOTE`, and leave your file untouched.

### How it works

1. Copy the generated file from `target/generated-sources/annotations/` to the matching path under
   `src/main/java/` — keeping the original package declaration.
2. Modify it as needed (add business logic, change return types, wire extra beans, etc.).
3. Rebuild. The annotation processor now emits:

```
Note: Skipping generation of com.example.order.OrderService: a source file with this name already
      exists and will be used as-is. Remove it to let the annotation processor regenerate it.
```

The manually maintained file is compiled instead of the generated one.

### What to keep in mind

| Concern                 | Guidance                                                                                            |
|-------------------------|-----------------------------------------------------------------------------------------------------|
| **Staying in sync**     | Your file is **never** auto-updated. If the aggregate gains a new `@Create` or `@Update` method you must update the override yourself. |
| **Reverting**           | Delete the override from `src/main/java/` and the processor regenerates the class on the next build. |
| **Partial customisation** | Prefer lighter-weight extension points first: `@RepositoryMixin` for extra queries, `PrefabPlugin` for structural generation changes. Reserve the escape hatch for one-off tweaks the framework does not yet model. |
| **Applies to all Filer-written files** | Controller, `*Service`, `*Repository`, `*Request`, `*Response`, `*EventConsumer`, and any other class generated via `JavaFileWriter`. |

> **Tip:** The source-file override works for both `src/main/java/` files and — for test-support classes written
> to `target/prefab-test-sources/` — overrides placed under `src/test/java/`. See [§6.9](#69-generated-test-client)
> for the test-client variant.

---

## 6.10 Generated Object Mothers (Test Sources)

Prefab generates `*Mother` classes in `target/prefab-test-sources/` for generated request records,
event records, and nested reachable record types.

For AVSC union fields generated as sealed interfaces, Prefab generates mothers only for the
**permitted branch wrapper records**, not for the sealed interface itself.

- Example: union field `ExactValue` with permitted wrappers `ExactValueDouble` and
  `ExactValueString` produces `ExactValueDoubleMother` and `ExactValueStringMother`.
- For `@Example` values on union-typed fields, mother defaults select the matching permitted branch
  wrapper value type.

### Mother API

Each generated `XxxMother` class exposes:

| Method                              | Description                                              |
|-------------------------------------|----------------------------------------------------------|
| `static MotherBuilder builder()`    | Returns a pre-populated builder with sensible defaults.  |
| `static Xxx createXxx()`            | Shorthand for `builder().build()`.                       |
| `static Xxx createXxx(Consumer<MotherBuilder> customiser)` | Creates an instance after applying the customiser. |

### MotherBuilder inner class

`MotherBuilder` extends the record's own `Builder` (or the standalone generated `XxxBuilder` for
hand-written event records) and adds higher-level convenience methods for nested types.

#### Nested record fields

For every field whose type is itself a record, `MotherBuilder` generates a `Consumer` overload so
callers can customise the nested object inline:

```java
// Instead of:
OrderMother.builder().address(AddressMother.createAddress(b -> b.city("Brussels"))).build();

// You can write:
OrderMother.builder().address(b -> b.city("Brussels")).build();
```

#### Nullable record fields — `withoutX()`

When a record field is annotated with JSpecify `@Nullable` (or a legacy annotation named `Nullable`), calling the
raw setter with `null` would be ambiguous with the `Consumer` overload above. A dedicated `withoutX()` method is
generated to avoid this:

```java
// Unambiguously set a nullable nested record to null:
OrderMother.builder().withoutAddress().build();
```

The method casts `null` to the concrete field type before delegating to the setter, eliminating the
compiler ambiguity.

#### List-of-record fields — varargs `Consumer` overload

When a field is typed `List<SomeRecord>` and `SomeRecord` is itself a record, a varargs overload is
generated so multiple entries can be built inline with individual customisers:

```java
// Build a list with two customised items:
CartMother.builder()
    .items(b -> b.name("Widget").quantity(2),
           b -> b.name("Gadget").quantity(1))
    .build();
```

Each varargs element is streamed through `SomeRecordMother::createSomeRecord` and collected into a
`List`.

#### List-of-record fields — `emptyX()`

To set a list field to an empty list without triggering the varargs overload, use `emptyX()`:

```java
CartMother.builder().emptyItems().build();
```

#### Nullable list-of-record fields — `withoutX()`

When a `List<SomeRecord>` field is additionally annotated `@Nullable`, a `withoutX()` method is
also generated to set the list to `null` unambiguously:

```java
ShipmentMother.builder().withoutOptionalItems().build();
```

### Default value strategy

| Field type          | Generated default                                        |
|---------------------|----------------------------------------------------------|
| `String`            | Field name as string literal                             |
| `int` / `Integer`   | `1`                                                      |
| `long` / `Long`     | `1L`                                                     |
| `double` / `Double` | `1.0`                                                    |
| `boolean`           | `false`                                                  |
| `Instant`           | `Instant.now()`                                          |
| `LocalDate`         | `LocalDate.now()`                                        |
| `LocalDateTime`     | `LocalDateTime.now()`                                    |
| `Duration`          | `Duration.ofSeconds(1)`                                  |
| `BigDecimal`        | `BigDecimal.ONE`                                         |
| `List<T>`           | `List.of(defaultT)` (one element for plain types); varargs overload for record elements |
| `Map<K,V>`          | `Map.of()`                                               |
| Nested record       | `NestedMother.createNested()`                            |
| Enum                | `EnumType.values()[0]`                                   |
| `@Nullable` field   | Same as non-nullable default (use `withoutX()` for null) |
| `MultipartFile`     | `new MockMultipartFile(...)` (zero-byte)                 |

`@Example` annotations on constructor parameters override these defaults.

### Output target

By default, all mother classes are written to `TARGET/prefab-test-sources` (test scope).
Use `@Generate(plugin = MotherPlugin.class, target = OutputTarget.MAIN)` on an event type to write
the mother to main sources instead.

---

## 6.12 Agent Context File

At the end of every compilation that contains aggregates, Prefab writes a compact Markdown summary meant for AI
coding agents. It describes only what applies to *this* project, so an agent does not have to read generic
documentation or browse `target/generated-sources` (and is not tempted to edit generated files).

| Compilation       | Location                          |
|-------------------|-----------------------------------|
| Main sources      | `target/prefab/agent-context.md`  |
| Test sources      | `target/prefab/agent-context-test.md` |

The location is stable and lives outside `target/classes`, so the file is never packaged in the jar.

### Contents

- **Active capabilities**: derived from the compile classpath (Postgres, MongoDB, Kafka, Pub/Sub, SNS/SQS,
  Spring Security, OpenAPI, AsyncAPI, Kafka Streams). Capabilities that are not on the classpath are omitted.
- **Aggregates**: per aggregate the HTTP endpoints (method and path), the events it handles with their topics,
  the table or collection it is stored in, and the generated test helpers (`{Name}Client`, Mothers, assertions).
- **Events**: every `@Event` type of the compilation with its topics and, for Avro, the serialization.
- **Required configuration**: the application properties the active capabilities need at runtime.

Events *published* by an aggregate are not attributed to it (publishing happens in method bodies); look them up
in the **Events** section instead.

### Determinism and size

All lists are sorted and the file contains no timestamps or absolute paths, so compiling the same model twice
gives identical output. Size budget: roughly 2.5 KB fixed plus 0.7 KB per aggregate; a test in
`AgentContextPluginTest` enforces it on a representative project.

### Turning it off

```
-Aprefab.plugin.agentcontext.enabled=false
```

See [Plugin selection](annotation-reference.md#generate) for the other ways to select plugins.

### How agents should use it

Read `target/prefab/agent-context.md` first, before opening generated sources. Treat it as the index of what
exists: change behaviour by editing the annotated domain types and recompiling, never by editing generated code.

---

## 6.13 Hand-Written Code Checks

A common mistake, by AI assistants and new users alike, is to write by hand what Prefab already generates for an
aggregate: a `@RestController`, a JPA `@Entity`, a Spring Data repository or a Flyway script. The code often compiles
and may pass simple tests, but the application ends up with two implementations, clashing endpoints or a drifting
schema. At the end of every compilation that contains aggregates, Prefab inspects the project's own sources (and
migrations) for these patterns and reports them in the same `Suggested fix:` style as its other diagnostics.

| What is reported | Severity | Example finding |
|------------------|----------|-----------------|
| A `@RestController`/`@Controller` handler that maps a method and path Prefab already exposes for an aggregate. A path variable of any name matches; a handler without an HTTP method matches every method | error | `[Prefab] get: this hand-written handler maps GET /orders/{id}, which Prefab already exposes for aggregate Order (get by id)` |
| An `@Entity` (Jakarta or javax), or a Spring Data `@Table`/`@Document`, that has the name of an aggregate or maps its table or collection | error | `[Prefab] OrderRow: this hand-written entity duplicates aggregate Order, which Prefab already persists (table order)` |
| A Spring Data repository (any interface or class extending `Repository<T, ID>`) whose domain type is an aggregate | error | `[Prefab] LegacyOrderRepository: this hand-written repository duplicates the OrderRepository that Prefab generates for aggregate Order` |
| A versioned Flyway migration in `db/migration` that creates, alters or drops a table Prefab manages for an aggregate, or creates an index on it | warning | `[Prefab] db/migration/V6__add_priority.sql is a hand-written migration that changes tables Prefab already manages: ALTER TABLE order (aggregate Order)` |

Every finding names what Prefab already provides and the recommended alternative or extension point.

### What is not reported

- **Additional endpoints.** A controller that maps paths Prefab does not expose, for example
  `GET /orders/{id}/summary`, is fine.
- **Documented extension points.** `@RepositoryMixin` interfaces, `PrefabPlugin` and provider overrides never trigger
  a finding.
- **The source-file override ([§6.11](#611-overriding-a-generated-class-escape-hatch)).** A controller or repository
  named like the generated class (`OrderController`, `OrderRepository`) in the aggregate's package or below it
  replaces the generated one on purpose.
- **Migrations that opt out.** A migration is skipped when its name ends in `generated`, `manual` or `baseline`
  (`V3__generated.sql`, `V4__add_status_manual.sql`, `V1__baseline.sql`) or when it contains the comment
  `-- prefab:manual`. Statements that only change data (`INSERT`, `UPDATE`, `DELETE`) and migrations of tables that
  Prefab does not manage never trigger a finding. See
  [Database Migration Lifecycle](database-migrations.md#hand-written-migrations-for-managed-tables).
- **Types that opt out.** Add `@SuppressWarnings("prefab.handwritten")` to a class, or to a single handler method, to
  keep a legacy implementation while an application moves to Prefab.

### Scope and limitations

Only sources handed to the compiler are inspected; code generated by other annotation processors is not. Aggregates
from other modules are not known, so a controller that clashes with an aggregate in a dependency is not reported.
Endpoints of polymorphic aggregates are not compared (their entities and repositories are). Migrations are warnings
because Prefab cannot tell a hand-written script from a renamed copy of a generated one.

### Options

| Option | Effect |
|--------|--------|
| `-Aprefab.plugin.handwrittencode.enabled=false` | Turns all of these checks off for the project (the standard [plugin switch](annotation-reference.md#generate)) |
| `-Aprefab.handwritten.severity=warning` or `=error` | Reports every finding with that severity instead of the defaults above, for example `warning` while an existing application is migrated to Prefab |

The checks run as part of every normal build; they need no extra goal.

### How agents should use it

A finding means Prefab already covers the code: delete it and change the annotated domain types instead. Only add
`@SuppressWarnings("prefab.handwritten")` or `-- prefab:manual` when a human has decided the duplicate is intentional.
