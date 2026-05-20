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

- `OrderResponse` — all fields of the aggregate, with `Reference<T>` serialized as `String`

Nested value objects (inner records) are also represented as nested response records.

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

---

## 6.6 Database Migration Scripts

### PostgreSQL (Flyway)

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

Generated for every aggregate. Extends `AbstractAssert<{Type}ResponseAssert, {Type}Response>` and exposes one
assertion method per record component, plus a static `assertThat()` factory method.

- Non-list fields generate `has{FieldName}(FieldType expected)`.
- List fields generate `has{FieldName}Satisfying(Consumer<ListAssert<ElementType>> requirements)`.

```java
// Generated: assertion.infrastructure.http.ProductResponseAssert
public class ProductResponseAssert
        extends AbstractAssert<ProductResponseAssert, ProductResponse> {

    public static ProductResponseAssert assertThat(ProductResponse actual) { ... }

    public ProductResponseAssert hasId(String expected) { ... }
    public ProductResponseAssert hasName(String expected) { ... }
    public ProductResponseAssert hasPrice(Double expected) { ... }
    public ProductResponseAssert hasTagsSatisfying(Consumer<ListAssert<String>> requirements) { ... }
}
```

### `{EventName}Assert`

Generated for every `@Event`-annotated record in the current module.

```java
// Generated: com.example.event.OrderCreatedAssert
public class OrderCreatedAssert
        extends AbstractAssert<OrderCreatedAssert, OrderCreated> {

    public static OrderCreatedAssert assertThat(OrderCreated actual) { ... }

    public OrderCreatedAssert hasOrderId(String expected) { ... }
    public OrderCreatedAssert hasCustomerName(String expected) { ... }
    public OrderCreatedAssert hasItemsSatisfying(Consumer<ListAssert<String>> requirements) { ... }
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

## 6.10 Generated Object Mothers (Test Sources)

Prefab generates `*Mother` classes in `target/prefab-test-sources/` for generated request records,
event records, and nested reachable record types.

For AVSC union fields generated as sealed interfaces, Prefab generates mothers only for the
**permitted branch wrapper records**, not for the sealed interface itself.

- Example: union field `ExactValue` with permitted wrappers `ExactValueDouble` and
  `ExactValueString` produces `ExactValueDoubleMother` and `ExactValueStringMother`.
- For `@Example` values on union-typed fields, mother defaults select the matching permitted branch
  wrapper value type.

