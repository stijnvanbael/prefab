# Module Dependency Matrix

**Part of the [Prefab Developer Guide](developer-guide.md)**

---

## Prefab Modules

| Module                 | Artifact ID                   | Required?          | Description                                    |
|------------------------|-------------------------------|--------------------|------------------------------------------------|
| `core`                 | `prefab-core`                 | **Always**         | Framework types, annotations, interfaces       |
| `streams`              | `prefab-streams`              | Streams DSL        | Backend-neutral stream DSL and test API        |
| `streams-engine`       | `prefab-streams-engine`       | Streams without Kafka | In-process streams engine and in-memory test harness |
| `streams-kafka`        | `prefab-streams-kafka`        | Streams on Kafka   | Kafka Streams backend for the streams DSL      |
| `annotation-processor` | `prefab-annotation-processor` | **Always**         | Compile-time code generator (APT)              |
| `postgres`             | `prefab-postgres`             | PostgreSQL         | Spring Data JDBC + Flyway + PostgreSQL support |
| `mongodb`              | `prefab-mongodb`              | MongoDB            | Spring Data MongoDB support                    |
| `kafka`                | `prefab-kafka`                | Kafka events       | Kafka producer/consumer configuration          |
| `pubsub`               | `prefab-pubsub`               | GCP Pub/Sub        | Google Cloud Pub/Sub support                   |
| `sns-sqs`              | `prefab-sns-sqs`              | AWS SNS/SQS        | AWS SNS publisher + SQS consumer support       |
| `avro`                 | `prefab-avro`                 | Avro serialization | Avro serialization/deserialization support     |
| `avro-processor`       | `prefab-avro-processor`       | AVSC-first events  | AVSC schema → Java record code generation      |
| `security`             | `prefab-security`             | Security           | Spring Security + OAuth2 integration           |
| `openapi`              | `prefab-openapi`              | OpenAPI docs       | SpringDoc OpenAPI / Swagger UI                 |
| `async-api`            | `prefab-async-api`            | AsyncAPI docs      | AsyncAPI specification generation              |
| `test`                 | `prefab-test`                 | Testing            | Testcontainers-based integration test support  |
| `terraform`            | `prefab-terraform`            | GCP infra          | Compile-time GCP Terraform configuration generation |

---

## Feature → Module Mapping

| Feature                 | Minimum Required Modules                                          |
|-------------------------|-------------------------------------------------------------------|
| REST CRUD (PostgreSQL)  | `core`, `annotation-processor`, `postgres`                        |
| REST CRUD (MongoDB)     | `core`, `annotation-processor`, `mongodb`                         |
| Kafka JSON events       | `core`, `annotation-processor`, `kafka`                           |
| Kafka Avro events       | `core`, `annotation-processor`, `kafka`, `avro`                   |
| Kafka AVSC-first events | `core`, `annotation-processor`, `kafka`, `avro`, `avro-processor` |
| Kafka Streams source/sink DSL | `core`, `annotation-processor`, `kafka`, `streams`, `streams-kafka` |
| In-process Streams engine | `core`, `streams`, `streams-engine` |
| GCP Pub/Sub events      | `core`, `annotation-processor`, `pubsub`                          |
| AWS SNS/SQS events      | `core`, `annotation-processor`, `sns-sqs`                         |
| Audit trail             | `core` (no extra module needed)                                   |
| Multi-tenancy           | `core` (no extra module needed)                                   |
| Endpoint security       | `core`, `annotation-processor`, `security`                         |
| Binary uploads          | `core`, `annotation-processor`, persistence module                |
| OpenAPI documentation   | `core`, `annotation-processor`, `openapi`                         |
| AsyncAPI documentation  | `core`, `annotation-processor`, `async-api`                       |
| Integration testing     | `test`, plus one or more messaging modules                        |

---

## GCP Terraform Infrastructure Generation

Add `prefab-terraform` to the annotation-processor classpath to generate Terraform files under
`terraform/gcp/` in the compiler output. The generator emits a baseline for Cloud Run and Artifact Registry,
then adds selected resources according to the persistence, messaging, and REST modules present in the compilation.
For Pub/Sub event handlers it can also emit topics and subscriptions for literal topic names.

This is infrastructure-as-code generation, not provisioning: review and manage the output with your normal Terraform
workflow. Pub/Sub topic names supplied as runtime property or SpEL expressions cannot be resolved at compile time;
Prefab warns and those resources must be managed separately. See the
[Terraform generator implementation](../../terraform/src/main/java/be/appify/prefab/processor/terraform/gcp/GcpTerraformWriter.java)
and [generation tests](../../terraform/src/test/java/be/appify/prefab/processor/terraform/gcp/GcpTerraformWriterTest.java)
for the emitted resources and limits.

---

## Repository Example Modules

| Module Path                         | Purpose                                                                                  |
|-------------------------------------|------------------------------------------------------------------------------------------|
| `examples/showcase`                 | Shop showcase on the reference stack (Postgres + Kafka, JSON)                            |
| `examples/showcase-variant-parent`  | Shared build that recompiles the showcase sources and tests per variant (decision-007)   |
| `examples/showcase-mongodb`         | Showcase variant on MongoDB + Kafka (configuration only)                                 |
| `examples/avro`                     | Avro and AVSC-first event examples                                                       |
| `examples/kafka`                    | Kafka aggregate and projection examples                                                  |
| `examples/streams`                  | Kafka streams DSL source/sink forwarding example                                         |
| `examples/pubsub`                   | Google Cloud Pub/Sub examples                                                            |
| `examples/sns-sqs`                  | AWS SNS/SQS examples                                                                     |
| `examples/mongodb`                  | MongoDB aggregate examples                                                               |

---

## Maven Dependency Snippet

Replace `LATEST_VERSION` with the current release version:

```xml
<!-- Always required -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-core</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-annotation-processor</artifactId>
    <version>LATEST_VERSION</version>
    <scope>provided</scope>
</dependency>

<!-- PostgreSQL persistence -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-postgres</artifactId>
    <version>LATEST_VERSION</version>
</dependency>

<!-- Kafka events -->
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-kafka</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```
