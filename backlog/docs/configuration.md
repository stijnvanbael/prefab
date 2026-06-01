# Configuration Reference

**Part of the [Prefab Developer Guide](developer-guide.md)**

---

## Application Properties

| Property                                | Default | Description                                                    |
|-----------------------------------------|---------|----------------------------------------------------------------|
| `prefab.dlt.topic.name`                 | —       | Dead-letter topic name (required if dead-lettering is enabled) |
| `prefab.dlt.retries.limit`              | `5`     | Default max retries before dead-lettering                      |
| `prefab.dlt.retries.minimum-backoff-ms` | `1000`  | Default minimum retry backoff (ms)                             |
| `prefab.dlt.retries.maximum-backoff-ms` | `30000` | Default maximum retry backoff (ms)                             |
| `prefab.dlt.retries.backoff-multiplier` | `1.5`   | Default exponential backoff multiplier                         |
| `management.endpoints.web.exposure.include` | `health,info` | Exposes baseline actuator web endpoints for generated applications |
| `management.endpoint.health.probes.enabled` | `true` | Enables liveness/readiness health probes |

Prefab includes Spring Boot Actuator by default through `prefab-core`.
If you need to expose more or fewer endpoints, override these with your own application properties.

---

## Kafka Configuration

Prefab uses standard Spring Kafka properties (`spring.kafka.*`). The `KafkaConfiguration` class provides
dynamic JSON serializers/deserializers via `DynamicSerializer` and `DynamicDeserializer` that look up the
correct Avro or JSON schema from `SerializationRegistry`.

By default, Prefab sets `auto.offset.reset=earliest` for production consumers when no explicit value is provided.

In test mode, generated Kafka listeners (those backing `@EventHandler` methods) default to `latest`
via the `testLatestOffsetResetCustomizer` bean provided by `KafkaTestAutoConfiguration`. This prevents
listeners from replaying historical events on each test run. The test infrastructure consumer
(`testConsumerFactory` — used by `@TestEventConsumer`) retains `earliest` so it reliably catches
any event published during a test, regardless of partition-assignment timing.

You can override the global consumer setting with `spring.kafka.consumer.auto-offset-reset`.
For generated Kafka listeners, `@EventHandlerConfig(autoOffsetReset = "...")` applies a
listener-level `@KafkaListener(properties = "auto.offset.reset=...")` override that takes precedence.

---

## Pub/Sub Configuration

Configure via `spring.cloud.gcp.pubsub.*` (standard Spring Cloud GCP properties). Prefab's
`PubSubConfiguration` auto-configures subscription and publishing beans.

---

## SNS/SQS Configuration

Configure via `spring.cloud.aws.*` (standard Spring Cloud AWS properties). Prefab's `SnsConfiguration`
auto-configures SNS publisher and SQS listener beans.

---

## `@EnablePrefab`

Add to your main application class. Imports all Prefab core beans. No attributes.

`@EnablePrefab` imports:
- `PrefabCoreConfiguration` – component scan for core beans
- `AuditConfiguration` – registers default `AuditContextProvider`
- `KafkaConfiguration` – Kafka serializer/deserializer (if Kafka is on classpath)
- `PubSubConfiguration` – Pub/Sub support (if GCP library is on classpath)
- `SnsConfiguration` – SNS/SQS support (if AWS library is on classpath)
- `SerializationRegistry` – topic-to-serialization-format registry

---

## Test Container Configuration

Prefab test infrastructure automatically provisions Docker containers for Postgres, Kafka, LocalStack (SNS/SQS), and Pub/Sub when running integration tests annotated with `@IntegrationTest`. By default, each container is assigned a stable, predictable Docker name and may be reused across test runs on the same machine.

### Container Names and Reuse

All testcontainers use a default Docker name pattern: **`<type>_<appName>`**, where `appName` is derived from `spring.application.name` with dots and dashes replaced by underscores. For example, with `spring.application.name=my-chat-app`, the Kafka container is named `kafka_my_chat_app`.

Kafka also uses a reusable Docker network with the same default naming pattern so repeated test runs attach to the same network instead of creating a fresh one each time.

| Container | Default name | Reuse | Notes |
|-----------|--------------|-------|-------|
| PostgreSQL | `postgres_<appName>` | ✅ | Programmatic `PostgreSQLContainer` bean with `.withReuse(true)` |
| Kafka | `kafka_<appName>` | ✅ | Configured with `.withReuse(true)` and a reusable named Docker network |
| Schema Registry | `schema_registry_<appName>` | ✅ | Enabled when `prefab.test.schema-registry.enabled=true`; disabled by default |
| LocalStack (SNS/SQS) | `localstack_<appName>` | ✅ | Configured with `.withReuse(true)` |
| Pub/Sub Emulator | `pubsub_<appName>` | ❌ | **Reuse disabled** to prevent state corruption; containers are destroyed after each test run |

### Configuration Properties

Each container name can be customized via a `prefab.test.*` property. If not set, the default name is used.

| Property                               | Default | Description |
|----------------------------------------|---------|-------------|
| `prefab.test.postgres.container-name` | `postgres_<appName>` | Override Postgres container name |
| `prefab.test.kafka.container-name` | `kafka_<appName>` | Override Kafka container name |
| `prefab.test.kafka.network-name` | `kafka_<appName>` | Override the reusable Kafka Docker network name |
| `prefab.test.schema-registry.container-name` | `schema_registry_<appName>` | Override Schema Registry container name |
| `prefab.test.localstack.container-name` | `localstack_<appName>` | Override LocalStack container name |
| `prefab.test.pubsub.container-name` | `pubsub_<appName>` | Override Pub/Sub Emulator container name |

### Example: Override Postgres Container Name

In `application-test.yml`:

```yaml
spring:
  application:
    name: my-service

prefab:
  test:
    postgres:
      container-name: my_postgres_test
```

With this configuration, the Postgres container will be named `my_postgres_test` instead of the default `postgres_my_service`.

### Benefits of Fixed Container Names

1. **Debugging**: Use `docker ps` to easily identify which container belongs to which application.
2. **Log Inspection**: Tail container logs by a human-readable name instead of a random ID.
3. **Container Reuse**: Testcontainers reuses containers across test runs when the reuse flag is enabled and the container name matches.
4. **Multi-app Testing**: Run multiple applications' test suites on the same machine and clearly distinguish their containers.

### Programmatic Container Access

In integration tests, containers are managed as Spring beans. For custom assertions or configuration:

```java
@IntegrationTest
class MyTest {
    @Autowired
    PostgreSQLContainer<?> postgresContainer;

    @Autowired
    KafkaContainer kafkaContainer;

    @Test
    void testWithCustomContainerAccess() {
        var jdbcUrl = postgresContainer.getJdbcUrl();
        var bootstrapServers = kafkaContainer.getBootstrapServers();
        // ... use in test
    }
}
```

