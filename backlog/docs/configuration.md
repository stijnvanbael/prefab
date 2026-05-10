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

---

## Kafka Configuration

Prefab uses standard Spring Kafka properties (`spring.kafka.*`). The `KafkaConfiguration` class provides
dynamic JSON serializers/deserializers via `DynamicSerializer` and `DynamicDeserializer` that look up the
correct Avro or JSON schema from `SerializationRegistry`.

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

