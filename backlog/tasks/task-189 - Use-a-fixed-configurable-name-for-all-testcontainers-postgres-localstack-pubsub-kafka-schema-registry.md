---
id: TASK-189
title: >-
  Use a fixed configurable name for all testcontainers (postgres, localstack,
  pubsub, kafka, schema-registry)
status: In Progress
assignee: []
created_date: '2026-05-11 05:07'
updated_date: '2026-05-11 05:20'
labels:
  - test
  - testcontainers
  - dx
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

Currently none of the Prefab testcontainers (Postgres, LocalStack/SNS-SQS, PubSub emulator, Kafka, Schema Registry) are created with a fixed Docker container name. As a result:

- Containers appear in Docker Desktop / `docker ps` with auto-generated, meaningless names (e.g. `tender_hopper`), making debugging and log-tailing difficult.
- Because the reuse-key is a configuration hash (not a human-readable label), identifying which container belongs to which application is impossible when several apps run tests on the same machine.
- There is no way for a developer to pin a specific container to an application just by name.

## Current State

| Container | Class | Location | Reuse | Fixed name |
|-----------|-------|----------|-------|-----------|
| Postgres | `PostgresTestEnvironmentPostProcessor` | `persistence/` | ✅ | ❌ |
| LocalStack (SNS/SQS) | `SnsTestAutoConfiguration` | `sns/` | ✅ | ❌ |
| PubSub emulator | `PubSubTestAutoConfiguration` | `pubsub/` | ❌ | ❌ |
| Kafka | `KafkaTestAutoConfiguration.kafkaContainer()` | `kafka/` | ❌ | ❌ |
| Schema Registry | `KafkaTestAutoConfiguration.kafkaSchemaRegistryContainer()` | `kafka/` | ❌ | ❌ |

## Desired Behaviour

Each container must be started with a fixed, predictable Docker name that:

1. Defaults to `prefab-<type>-<appName>` (where `appName` is the sanitised `spring.application.name`, e.g.:
   - `prefab-postgres-myapp`
   - `prefab-localstack-myapp`
   - `prefab-pubsub-myapp`
   - `prefab-kafka-myapp`
   - `prefab-schema-registry-myapp`
2. Can be overridden per container type via an application property:
   - `prefab.test.postgres.container-name`
   - `prefab.test.localstack.container-name`
   - `prefab.test.pubsub.container-name`
   - `prefab.test.kafka.container-name`
   - `prefab.test.schema-registry.container-name`
3. Is set using `.withCreateContainerCmdModifier(cmd -> cmd.withName(resolvedName))` on the Testcontainers API.

### Reuse Policy

- **Postgres, LocalStack, Kafka, Schema Registry**: `.withReuse(true)` — containers survive between test runs and are identified by their fixed names.
- **PubSub**: `.withReuse(false)` — containers are destroyed and recreated for each test run to avoid state corruption issues inherent to the emulator.

Even though PubSub does not reuse containers, having a fixed Docker name still provides value for debugging and distinguishing which container belongs to which application during the test run.

## Analysis

### Postgres

`PostgresTestEnvironmentPostProcessor` currently generates a **TC JDBC URL** and relies on the `ContainerDatabaseDriver` reuse mechanism. This driver does not expose the Testcontainers `.withCreateContainerCmdModifier()` API via the URL, so a fixed Docker name **cannot** be set through the URL alone.

**Options:**

A. Keep the JDBC URL approach and accept that Postgres cannot have a fixed name (minimal change, but misses the goal).

B. Switch Postgres to a **programmatic** `PostgreSQLContainer` bean (similar to LocalStack) configured with `.withReuse(true).withCreateContainerCmdModifier(...)`. The `spring.datasource.*` properties would then be registered via `DynamicPropertyRegistrar`. This gives full control over the container name but requires refactoring `PostgresTestEnvironmentPostProcessor`.

**Recommended: Option B.** It aligns Postgres with the pattern already used for LocalStack and PubSub, simplifies the approach, and makes the name fully configurable.

### LocalStack (SNS/SQS)

`SnsTestAutoConfiguration` already creates a programmatic `LocalStackContainer` with `.withReuse(true)`. Adding `.withCreateContainerCmdModifier(cmd -> cmd.withName(name))` is straightforward.

The container name must be resolved from the environment **before** the container is instantiated (static initialiser). This requires reading the property from a `Properties` file or env-var at class-load time, or converting the static initialiser into a lazy Spring bean that is given the name via injection. **Converting the static field to a `@Bean` is the preferred approach** because it plays well with Spring's property resolution.

### PubSub

`PubSubTestAutoConfiguration` creates a static `PubSubEmulatorContainer` with `.withReuse(false)`. Change:
- Keep `.withReuse(false)` to avoid state corruption issues (do **not** enable reuse).
- Add `.withCreateContainerCmdModifier(cmd -> cmd.withName(name))` for a fixed name during the test run.
- Convert static field to a `@Bean` so the name can be injected.

### Kafka

`KafkaTestAutoConfiguration` already creates `kafkaContainer()` as a proper Spring `@Bean` (line 60). The container currently has no fixed name and no reuse policy. Change:
- Add `.withReuse(true)`.
- Add `.withCreateContainerCmdModifier(cmd -> cmd.withName(name))`.
- Inject the resolved container name via constructor or method parameter.

### Schema Registry

`KafkaTestAutoConfiguration` already creates `kafkaSchemaRegistryContainer()` as a proper Spring `@Bean` (line 69). Similar to Kafka:
- Add `.withReuse(true)`.
- Add `.withCreateContainerCmdModifier(cmd -> cmd.withName(name))`.
- Inject the resolved container name via constructor or method parameter.

**Important**: Schema Registry depends on Kafka (via `dependsOn(kafkaContainer)`), so its name should only be applied when the `prefab.test.schema-registry.enabled` property is `true`.

## Configuration Properties

Introduce a `PrefabTestContainerProperties` configuration-properties class (or reuse an existing properties record) to hold:

```
prefab.test.postgres.container-name         (default: prefab-postgres-<appName>)
prefab.test.localstack.container-name       (default: prefab-localstack-<appName>)
prefab.test.pubsub.container-name           (default: prefab-pubsub-<appName>)
prefab.test.kafka.container-name            (default: prefab-kafka-<appName>)
prefab.test.schema-registry.container-name  (default: prefab-schema-registry-<appName>)
```

## Impact on Reuse

Docker requires every container name to be unique. If `.withReuse(true)` is set and a container with the same name already exists, Testcontainers will reuse it. This is exactly the desired behaviour for Postgres, LocalStack, Kafka, and Schema Registry — developers get a stable, identifiable container that survives between test runs.

PubSub is intentionally excluded from reuse to protect against state corruption issues specific to the Google Cloud Pub/Sub emulator. Even without reuse, the fixed name provides value for debugging.

**Important**: when `.withCreateContainerCmdModifier` is combined with `.withReuse(true)`, Testcontainers still uses the configuration hash as the primary reuse key, not the name. The name is merely applied on creation. If two containers have the same hash but the old one was renamed, Docker may refuse to create a new one. This is acceptable and even desirable for this use-case.

## Migration / Backward Compatibility

- Existing tests that do not set `prefab.test.*.container-name` get the default name based on `spring.application.name`.
- No user-facing API changes; all changes are internal to the auto-configuration classes.
- The `TC_REUSABLE=true` JDBC URL approach for Postgres is replaced; users who had a custom `spring.datasource.url` override in their `application-test.yml` keep their override unaffected (the new bean is `@ConditionalOnMissingBean`).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Postgres testcontainer is created with a fixed Docker name derived from `spring.application.name` by default (e.g. `prefab-postgres-myapp`). Reuse is enabled.
- [ ] #2 LocalStack testcontainer is created with a fixed Docker name derived from `spring.application.name` by default (e.g. `prefab-localstack-myapp`). Reuse is enabled.
- [ ] #3 PubSub emulator testcontainer is created with a fixed Docker name derived from `spring.application.name` by default (e.g. `prefab-pubsub-myapp`). Reuse is disabled (due to state corruption issues with reuse).
- [ ] #4 Kafka testcontainer is created with a fixed Docker name derived from `spring.application.name` by default (e.g. `prefab-kafka-myapp`). Reuse is enabled.
- [ ] #5 Schema Registry testcontainer is created with a fixed Docker name derived from `spring.application.name` by default (e.g. `prefab-schema-registry-myapp`) when enabled. Reuse is enabled.
- [ ] #6 Each container name can be overridden via a dedicated property (`prefab.test.postgres.container-name`, `prefab.test.localstack.container-name`, `prefab.test.pubsub.container-name`, `prefab.test.kafka.container-name`, `prefab.test.schema-registry.container-name`).
- [ ] #7 Postgres is provisioned via a programmatic `PostgreSQLContainer` bean (replacing the TC JDBC URL approach) so the full Testcontainers API is available.
- [ ] #8 All existing integration tests continue to pass after the change.
- [ ] #9 The developer guide (`backlog/docs/`) is updated to document the new configuration properties.
<!-- AC:END -->
