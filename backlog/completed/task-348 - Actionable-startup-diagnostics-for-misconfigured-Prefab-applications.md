---
id: TASK-348
title: Actionable startup diagnostics for misconfigured Prefab applications
status: Done
assignee: []
created_date: '2026-11-20 20:14'
labels:
  - agents
  - dx
dependencies: []
priority: medium
type: feature
ordinal: 193000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The annotation processor already gives actionable compile-time errors with a 'Suggested fix' (TASK-172, extended by TASK-323). Many failures only show up when the application starts, though: missing or wrong broker, database or tenant configuration, missing topics or subscriptions, missing provider beans. These surface as long Spring context stack traces with no Prefab-specific guidance, and troubleshooting.md has no examples for them.

AI agents fix problems quickly when the error says what is wrong and what to do; they flounder in generic stack traces. This task brings the same actionable style to startup failures.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The most common Prefab startup misconfigurations are inventoried for each runtime module (core, postgres, mongodb, kafka, pubsub, sns-sqs, security, tenancy)
- [x] #2 Each inventoried failure produces a short report naming the cause, the missing or wrong property or bean, and the fix, instead of only a raw stack trace
- [x] #3 The original exception stays available for debugging
- [x] #4 Each report is covered by a test that starts a context with the misconfiguration and checks the message
- [x] #5 troubleshooting.md lists every report with its cause and fix
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
**Approach.** Spring Boot `FailureAnalyzer`s, registered in `META-INF/spring.factories` of the module that owns the failing feature. Each returns a `FailureAnalysis` (description and action) whose cause is the original failure, so Spring Boot still logs the full stack trace at `DEBUG` (`--debug`). Analyzers match missing beans by class name, so they load without the optional library on the classpath, and they run at `HIGHEST_PRECEDENCE` but only for the exact cases they know. Everything else keeps Spring Boot's own output.

**Shared building blocks (`prefab-core`, `be.appify.prefab.core.diagnostics`).** `FailureReport` (description and action), `MissingBeanFailureAnalyzer` (public base: map of bean type name to report, reused by `prefab-postgres` and `prefab-mongodb`), `PrefabConfigurationException` (an `IllegalStateException` carrying description and action, printed by `PrefabConfigurationFailureAnalyzer`) and `UnknownClassException` (thrown by `Classes.classWithName`, still an `IllegalArgumentException`).

**Inventory (AC #1).**

| Module | Failure | Detected as |
|---|---|---|
| core | Prefab not enabled (no `@EnablePrefab`) | `NoSuchBeanDefinitionException` for a `be.appify.prefab` type while no `PrefabCoreConfiguration` bean exists |
| core | `spring.application.name` missing | `PlaceholderResolutionException` |
| core / tenancy | Several `TenantContextProvider` beans | `NoUniqueBeanDefinitionException` |
| kafka | Unknown class in `prefab.dlt.non-retryable-exceptions` | `UnknownClassException` |
| kafka | AVRO topic without `schema.registry.url` | `PrefabConfigurationException` (was a plain `IllegalStateException`, same message content) |
| kafka | Kafka connection details missing | missing `KafkaConnectionDetails` |
| pubsub | `spring.cloud.gcp.project-id` missing | `PlaceholderResolutionException` |
| pubsub | Pub/Sub beans missing | missing `PubSubAdmin`, `PubSubTemplate`, `PubSubSubscriberTemplate` |
| sns-sqs | SNS/SQS clients missing | missing `SnsTemplate`, `SnsClient`, `SqsAsyncClient` |
| postgres | DataSource missing | missing `DataSource`, `JdbcOperations`, `NamedParameterJdbcOperations` |
| postgres | Database unreachable, bad credentials, unknown database | `PSQLException` by SQLState `08001`, `28000`/`28P01`, `3D000` |
| postgres | Unknown `prefab.full-text.language` | `PrefabConfigurationException` |
| mongodb | MongoDB beans missing | missing `MongoDatabaseFactory`, `MongoOperations`, `MongoTemplate`, `MongoClient` |
| mongodb | MongoDB unreachable | `MongoTimeoutException` |
| mongodb | Invalid connection string | `IllegalArgumentException` from the driver |
| security | No `JwtDecoder` for the default chain | missing `JwtDecoder` |

**Left to Spring Boot.** A missing `spring.datasource.url` (Boot's `DataSourceBeanCreationFailureAnalyzer` already explains it, and its exception type is package-private). An unreachable Kafka broker does not fail startup and `spring.kafka.bootstrap-servers` defaults to `localhost:9092`, so there is nothing to report. Pub/Sub and SNS/SQS topics and subscriptions are created at startup by `PubSubUtil`/`SqsUtil`, so a missing one is not a startup failure.

**Tests (AC #4).** Every report has a `StartupDiagnosticsTest` in its module that starts an application with `SpringApplicationBuilder`, lets Spring Boot print the report through the registered analyzers and asserts on the output (so the `spring.factories` registration is covered too). Reports that print connection details are also asserted not to contain the password. `FailureAnalyzersTest` checks that the original exception is the analysis cause (AC #3) and that unknown beans are left alone.

**Docs (AC #5).** `backlog/docs/troubleshooting.md` has a "Startup failure reports" section listing every report with cause and fix, and how to add one.
<!-- SECTION:NOTES:END -->
