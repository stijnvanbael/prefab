# Getting Started

**Part of the [Prefab Developer Guide](developer-guide.md)**

---

## Spring Boot Application Setup

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

`@EnablePrefab` imports `PrefabCoreConfiguration` (component scan for core beans). It is the single opt-in switch: every other Prefab configuration is a Spring Boot auto-configuration that only activates when `PrefabCoreConfiguration` is present, the relevant library is on the classpath and any required beans exist. This includes the test auto-configurations of `prefab-test`.

Auto-configurations (all gated on `PrefabCoreConfiguration`):
- `AuditConfiguration` – registers default `AuditContextProvider`
- `PrefabRegistryConfiguration` – `EventRegistry` populated from `EventRegistryCustomizer` beans
- `TenantConfiguration` – default no-op `TenantContextProvider`
- `KafkaConfiguration` – Kafka serializer/deserializer (if Kafka is on classpath)
- `PubSubConfiguration` – Pub/Sub support (if GCP library is on classpath)
- `SnsConfiguration` – SNS/SQS support (if AWS library is on classpath)
- `PrefabJdbcConfiguration`, `PrefabMongoConfiguration`, `OpenApiAutoConfiguration`, `AsyncApiConfiguration`, `WebSecurityConfiguration`, `StreamsConfiguration` – activated by adding the matching module

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

## Fast Build Feedback

During development, compile the application without starting test contexts:

```bash
mvn -DskipTests compile
```

Then run only the test class for the change (replace the example class with yours):

```bash
mvn -Dtest=ProductIntegrationTest test
```

Run the complete verification before sharing the change:

```bash
mvn verify
```

For multi-module projects with independent modules, `mvn -T 2 verify` can build modules concurrently. It does not
speed up a single-module application; use it only when the project's tests are safe to run across modules in parallel.

---

## Run the application locally without infrastructure

Add `prefab-test` as a test-scoped dependency (as you would for `@IntegrationTest`), then start the application from its
Maven project directory:

```shell
mvn test-compile spring-boot:test-run
```

Spring Boot runs the application with the test classpath. Prefab's in-memory repositories and event bus replace the
database and broker; no Docker, database, broker, generated launcher, or extra configuration is needed. State is
ephemeral and disappears when the process stops. The startup log prints the base URL, available OpenAPI/Swagger and
AsyncAPI URLs, and the active in-memory backends.

The local run does not weaken application security. Use the application's own local authentication setup; the showcase
defaults to its `local` profile, which uses HTTP Basic and Spring Boot's generated development password. Never use a
local profile as a production security configuration. To start with example data, add a test-source `ApplicationRunner`
and activate an app-specific profile such as `local-demo`; the showcase variant demonstrates this pattern.

---

## Next Steps

- [Annotation Reference](annotation-reference.md) — full reference for every annotation
- [Built-in Types](built-in-types.md) — `Reference<T>`, `Binary`, `AuditInfo`, and more
- [Generated Artefacts](generated-artefacts.md) — what gets generated and how
- [Feature Guides](feature-guides.md) — hands-on guides for every feature
- [Module Dependency Matrix](modules.md) — which modules to add for each feature
