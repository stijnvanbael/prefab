---
id: doc-001
title: Prefab Agent Guide
---

# Prefab Agent Guide

This document provides detailed guidance for AI agents and developers working with the Prefab framework. It covers
Maven POM setup, application configuration, and writing tests.

---

## 1. Maven POM Setup

### 1.1 Parent POM

Always declare the Prefab parent POM to inherit dependency management and plugin configuration:

```xml
<parent>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-parent</artifactId>
    <version>0.5.0-SNAPSHOT</version>
</parent>
```

### 1.2 Required Dependencies

Every Prefab project needs **all three** of these:

```xml
<dependencies>
    <!-- Core annotations and runtime -->
    <dependency>
        <groupId>be.appify.prefab</groupId>
        <artifactId>prefab-core</artifactId>
    </dependency>

    <!-- Choose ONE persistence backend (see section 1.3) -->
    <dependency>
        <groupId>be.appify.prefab</groupId>
        <artifactId>prefab-postgres</artifactId>  <!-- OR prefab-mongodb -->
    </dependency>

    <!-- Annotation processor — must be provided scope -->
    <dependency>
        <groupId>be.appify.prefab</groupId>
        <artifactId>prefab-annotation-processor</artifactId>
        <scope>provided</scope>
    </dependency>

    <!-- Test support -->
    <dependency>
        <groupId>be.appify.prefab</groupId>
        <artifactId>prefab-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 1.3 Persistence Module — Pick Exactly One

| Module | Backend | Migrations |
|---|---|---|
| `prefab-postgres` | PostgreSQL via Spring Data JDBC | Flyway (use `@DbMigration`) |
| `prefab-mongodb` | MongoDB via Spring Data MongoDB | None — schemaless |

### 1.4 Optional Modules

Add only what the application actually needs:

```xml
<!-- Kafka event streaming -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-kafka</artifactId>
</dependency>

<!-- Google Cloud Pub/Sub -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-pubsub</artifactId>
</dependency>

<!-- AWS SNS/SQS -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-sns-sqs</artifactId>
</dependency>

<!-- OpenAPI / Swagger UI -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-openapi</artifactId>
</dependency>

<!-- Security (OAuth2) -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-security</artifactId>
</dependency>
```

### 1.5 Required Build Plugins

Both plugins below are **mandatory** for annotation processing and test client generation to work correctly.

```xml
<build>
    <plugins>

        <!-- 1. Expose generated test-client sources to the test compiler -->
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>build-helper-maven-plugin</artifactId>
            <version>3.4.0</version>
            <executions>
                <execution>
                    <id>add-main-as-test-source</id>
                    <phase>generate-test-sources</phase>
                    <goals>
                        <goal>add-test-source</goal>
                    </goals>
                    <configuration>
                        <sources>
                            <source>target/prefab-test-sources</source>
                        </sources>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- 2. Wire the Prefab annotation processor -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <source>${maven.compiler.release}</source>
                <target>${maven.compiler.release}</target>
                <annotationProcessors>
                    <annotationProcessor>be.appify.prefab.processor.PrefabProcessor</annotationProcessor>
                </annotationProcessors>
            </configuration>
        </plugin>

    </plugins>
</build>
```

> **Why `target/prefab-test-sources`?**  
> The annotation processor generates `*Client` test helper classes into `target/prefab-test-sources` at
> compile time. The `build-helper-maven-plugin` adds that directory as a test source root so those classes are
> available inside your `src/test/java` code. Without this, test compilation fails with "cannot find symbol: class XxxClient".

---

## 2. Application Configuration

### 2.1 Main `application.yml`

```yaml
spring:
  application:
    name: my-app        # Required when using events — used to namespace topics

# PostgreSQL example (when using prefab-postgres)
  datasource:
    url: jdbc:postgresql://localhost:5432/my_db
    username: ${DB_USER}
    password: ${DB_PASS}

# MongoDB example (when using prefab-mongodb)
  data:
    mongodb:
      uri: ${MONGO_URI:mongodb://localhost:27017/my_db}
      auto-index-creation: true

# OAuth2 (when using prefab-security)
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_OAUTH2_CLIENT_ID}
            client-secret: ${GOOGLE_OAUTH2_CLIENT_SECRET}

# Topic names (when using Kafka, Pub/Sub, or SNS/SQS)
topics:
  order.name: my-app.order
  invoice.name: my-app.invoice

# Error handling (recommended)
error.handling:
  exception-logging: WITH_STACKTRACE
```

### 2.2 Test `application-test.yml`

Place this file in `src/test/resources/`. The `@IntegrationTest` annotation automatically activates the `test` profile.

**PostgreSQL (Testcontainers JDBC URL)**

```yaml
spring:
  datasource:
    # TC_REUSABLE=true reuses the container across test runs for speed
    url: jdbc:tc:postgresql:16.1:///my_db?TC_REUSABLE=true&currentSchema=my_schema
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  flyway:
    clean-disabled: false   # allow schema reset between test runs
```

**MongoDB (auto-configured by `@IntegrationTest`)**

For MongoDB, no datasource configuration is needed in the test profile. The `@IntegrationTest` annotation
automatically starts a MongoDB Testcontainer and drops all collections before each test when
`prefab-mongodb` is on the classpath.

### 2.3 Topic name placeholders

When your aggregate events use Spring property placeholders for topic names (recommended), define the topic names in
`application.yml`:

```yaml
topics:
  order.name: my-app.order
```

Reference them in the `@Event` annotation:

```java
@Event(topic = "${topics.order.name}", platform = Event.Platform.KAFKA)
public record OrderCreated(Reference<Order> orderId, Instant createdAt) {}
```

### 2.4 OpenAPI metadata (optional)

```yaml
prefab:
  openapi:
    title: My API
    description: REST API for my application
    version: 1.0.0
```

---

## 3. Writing Tests

### 3.1 The `@IntegrationTest` Annotation

Annotate every integration test class with `@IntegrationTest`. This single annotation:

- Starts the full Spring Boot application context (`@SpringBootTest`)
- Configures `MockMvc` (`@AutoConfigureMockMvc`)
- Activates the `test` profile (`@ActiveProfiles("test")`)
- Auto-starts Testcontainers for Kafka, Pub/Sub, SNS/SQS, and MongoDB as needed
- Resets the database state between tests (drops and recreates the schema for PostgreSQL / drops all collections for MongoDB)

```java
@IntegrationTest
class OrderIntegrationTest {
    // ...
}
```

### 3.2 Using Generated Test Clients

The annotation processor generates a `*Client` class for every aggregate that has at least one REST endpoint. These
clients wrap `MockMvc` and provide strongly-typed methods matching every generated endpoint.

After adding the `build-helper-maven-plugin` (section 1.5), the clients are available as Spring beans in tests:

```java
@IntegrationTest
class OrderIntegrationTest {

    @Autowired
    OrderClient orders;     // generated by the annotation processor

    @Test
    void createAndRetrieveOrder() throws Exception {
        var orderId = orders.createOrder(Instant.now(), BigDecimal.valueOf(99.99));

        var order = orders.getOrderById(orderId);

        assertThat(order.amount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
    }
}
```

**Generated client method naming convention:**

| Endpoint type | Generated client methods |
|---|---|
| `@Create` | `create<Aggregate>(field1, field2, …)` and `create<Aggregate>(<AggregateRequest>)` |
| `@GetById` | `get<Aggregate>ById(id)` |
| `@GetList` | `find<Aggregates>(pageable, filterField, …)` |
| `@Update` | `<methodName>(id, field1, …)` and `<methodName>(id, <UpdateRequest>)` |
| `@Delete` | `delete<Aggregate>(id)`, `whenDeleting<Aggregate>(id)`, `given<Aggregate>Deleted(id)` |

### 3.3 Using `MockMvc` Directly

Inject `MockMvc` when you need raw HTTP control (e.g. testing validation errors):

```java
@Autowired
MockMvc mockMvc;

@Test
void createOrder_withoutAmount_returns400() throws Exception {
    mockMvc.perform(post("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"start\":\"2024-01-01T00:00:00Z\"}"))
        .andExpect(status().isBadRequest());
}
```

### 3.4 Testing Events

#### Unified API (platform-agnostic — preferred)

Use `@TestEventConsumer` + `EventConsumer<T>` + `EventAssertions` for tests that should work regardless of
whether the underlying broker is Kafka, Pub/Sub, or SNS/SQS:

```java
@IntegrationTest
class OrderIntegrationTest {

    @Autowired
    OrderClient orders;

    @TestEventConsumer(topic = "${topics.order.name}")
    EventConsumer<OrderCreated> orderCreatedConsumer;

    @Test
    void createOrder_publishesEvent() throws Exception {
        orders.createOrder(Instant.now(), BigDecimal.valueOf(99.99));

        EventAssertions.assertThat(orderCreatedConsumer)
            .withTimeout(5, TimeUnit.SECONDS)
            .hasReceivedValueSatisfying(OrderCreated.class, event -> {
                assertThat(event.amount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
            });
    }
}
```

#### Kafka-specific assertions

```java
import static be.appify.prefab.test.kafka.asserts.KafkaAssertions.assertThat;

assertThat(kafkaConsumer)
    .withTimeout(5, TimeUnit.SECONDS)
    .hasReceivedValueSatisfying(OrderCreated.class, event -> { ... });
```

#### Asserting async side effects with Awaitility

When testing event-driven side effects (e.g. an event handler updates another aggregate), use Awaitility to
poll until the state is consistent:

```java
await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
    var result = otherClient.getOtherById(otherId);
    assertThat(result.status()).isEqualTo(Status.PROCESSED);
});
```

### 3.5 Complete PostgreSQL + Kafka Integration Test Example

```java
@IntegrationTest
class MessageIntegrationTest {

    @Autowired
    ChannelClient channels;
    @Autowired
    UserClient users;
    @Autowired
    MessageClient messages;

    @TestEventConsumer(topic = "${topics.message.name}")
    EventConsumer<MessageSent> messageSentConsumer;

    @Test
    void sendMessage_notifiesSubscribers() throws Exception {
        var channelId = channels.createChannel("general");
        var johnId = users.createUser("John");
        users.subscribeToChannel(johnId, channelId);

        var messageId = messages.createMessage(johnId, channelId, "Hello!");

        EventAssertions.assertThat(messageSentConsumer)
            .withTimeout(5, TimeUnit.SECONDS)
            .hasReceivedValueSatisfying(MessageSent.class, event ->
                assertThat(event.content()).isEqualTo("Hello!"));
    }
}
```

### 3.6 Complete MongoDB Integration Test Example

```java
@IntegrationTest
class ProductIntegrationTest {

    @Autowired
    ProductClient products;
    @Autowired
    CategoryClient categories;
    @Autowired
    MockMvc mockMvc;

    @Test
    void createAndRetrieveProduct() throws Exception {
        var categoryId = categories.createCategory("Gadgets");
        var productId = products.createProduct("Widget", "A useful widget",
                BigDecimal.valueOf(9.99), "USD", categoryId);

        var product = products.getProductById(productId);

        assertThat(product.name()).isEqualTo("Widget");
        assertThat(product.price().amount()).isEqualByComparingTo(BigDecimal.valueOf(9.99));
    }

    @Test
    void createProduct_withoutName_returns400() throws Exception {
        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"desc\",\"amount\":10,\"currency\":\"USD\"}"))
            .andExpect(status().isBadRequest());
    }
}
```

---

## 4. Common Pitfalls

| Symptom | Cause | Fix |
|---|---|---|
| `cannot find symbol: class XxxClient` | `build-helper-maven-plugin` missing or `target/prefab-test-sources` not added | Add the plugin (section 1.5) and run `mvn generate-test-sources` |
| `IllegalAccessException: final field has no write access` | Update method named `setX` or `withX` where X is a field name | Rename to `updateX` or another name (see readme Known Issues) |
| `java: Compilation failed: internal java compiler error` | IntelliJ partial compile skips annotation processor | Delegate build to Maven in IntelliJ settings |
| No Testcontainer started for MongoDB | `prefab-mongodb` not on classpath | Add `prefab-mongodb` dependency |
| Topics not created in tests | Missing `spring.application.name` | Set `spring.application.name` in `application.yml` |
| `prefab-postgres` + `prefab-mongodb` both on classpath | Ambiguous persistence module | Remove one; keep only the backend you need |
