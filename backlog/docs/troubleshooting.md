# Troubleshooting

**Part of the [Prefab Developer Guide](developer-guide.md)**

---

## Startup failure reports

When a Prefab application is misconfigured, startup fails with a short report instead of a bare Spring stack trace:

```
***************************
APPLICATION FAILED TO START
***************************

Description:

The property spring.application.name is not set. ...

Action:

Set spring.application.name in application.yml, ...
```

The reports are Spring Boot `FailureAnalyzer`s, registered through `META-INF/spring.factories` in the module that owns
the failing feature. The original exception is not lost: start the application with `--debug` (or set
`logging.level.org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter=DEBUG`) to see its full stack trace.
Failures Prefab has no report for keep Spring Boot's own output.

Spring Boot already reports a missing JDBC URL itself (`Failed to configure a DataSource: 'url' attribute is not
specified`), so Prefab has no report for that. Set `spring.datasource.url`, `spring.datasource.username` and
`spring.datasource.password`.

| Module            | Report (start of the description)                                  | Cause and fix                                                                                                                                           |
|-------------------|--------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `prefab-core`     | `Prefab is not enabled: no bean of type be.appify.prefab…`         | A Prefab bean is missing because `@EnablePrefab` is not on the application class. Add it.                                                               |
| `prefab-core`     | `The property spring.application.name is not set`                  | Kafka, Pub/Sub and SNS/SQS name consumer groups, dead-letter destinations and subscriptions after it. Set `spring.application.name`.                     |
| `prefab-core`     | `Multi-tenancy needs exactly one TenantContextProvider`            | The application defines two or more `TenantContextProvider` beans. Remove all but one, or mark one `@Primary`. With none, Prefab registers a no-op one. |
| `prefab-core`     | `The class [x] could not be loaded`                                | `prefab.dlt.non-retryable-exceptions` lists a class that is not on the classpath. Use fully qualified names, with `$` for nested classes.                |
| `prefab-core`     | `Missing Kafka property 'schema.registry.url' for AVRO event serialization` | An AVRO topic is registered without a schema registry. Set `spring.kafka.properties.schema.registry.url`. Tests may opt into the mock registry. |
| `prefab-core`     | `Prefab needs Kafka connection details`                            | Spring Boot's Kafka auto-configuration is missing. Add `spring-boot-starter-kafka` and set `spring.kafka.bootstrap-servers`.                             |
| `prefab-core`     | `The property spring.cloud.gcp.project-id is not set`              | Pub/Sub topics and subscriptions are created in a project. Set `spring.cloud.gcp.project-id` (any name works against the emulator).                      |
| `prefab-core`     | `Prefab needs a Google Cloud Pub/Sub bean`                         | Spring Cloud GCP did not create its Pub/Sub beans. Check `spring.cloud.gcp.pubsub.enabled`, the project id and the credentials or emulator host.        |
| `prefab-core`     | `Prefab needs an SNS client`                                       | Spring Cloud AWS did not create its clients. Add `spring-cloud-aws-starter-sns` and `-sqs`, set `spring.cloud.aws.region.static` and the credentials.    |
| `prefab-core`     | `Prefab needs an SQS client`                                       | As above, for SQS. Against LocalStack set `spring.cloud.aws.sns.endpoint` and `spring.cloud.aws.sqs.endpoint`.                                          |
| `prefab-postgres` | `prefab-postgres needs a JDBC DataSource`                          | There is no `DataSource` bean. Do not exclude `DataSourceAutoConfiguration`; set the `spring.datasource.*` properties.                                  |
| `prefab-postgres` | `The application could not connect to PostgreSQL`                  | The database is down or unreachable. Check the host and port in `spring.datasource.url`. The report never prints the URL or the password.                |
| `prefab-postgres` | `PostgreSQL rejected the credentials of the application`           | Wrong `spring.datasource.username` or `spring.datasource.password`, or the user may not connect.                                                        |
| `prefab-postgres` | `The PostgreSQL database named in spring.datasource.url does not exist` | Create the database, or correct its name in the URL.                                                                                               |
| `prefab-postgres` | `Unknown PostgreSQL text search configuration 'x'`                 | `prefab.full-text.language` names a configuration the database does not have. Use `simple`, `english`, … (`\dF` in psql lists them).                     |
| `prefab-mongodb`  | `prefab-mongodb needs MongoDB beans`                               | Spring Boot's MongoDB auto-configuration is missing. Do not exclude it; set `spring.mongodb.uri`.                                                       |
| `prefab-mongodb`  | `The application could not connect to MongoDB within the server selection timeout` | MongoDB is down or unreachable. Check the host, port and credentials in `spring.mongodb.uri`. The report never prints them.            |
| `prefab-mongodb`  | `The MongoDB connection string in spring.mongodb.uri is invalid`   | Use `mongodb://[user:password@]host[:port]/<database>` or `mongodb+srv://…`, and percent-encode special characters in credentials.                       |
| `prefab-security` | `prefab-security validates bearer tokens as JWTs`                  | There is no `JwtDecoder`. Set `spring.security.oauth2.resourceserver.jwt.issuer-uri` or `…jwk-set-uri`, or define a `JwtDecoder` bean.                  |

Kafka needs no property for its broker: Spring Boot defaults `spring.kafka.bootstrap-servers` to `localhost:9092`.

### Adding a report

A report is a `FailureAnalyzer` that returns a `FailureReport(description, action)`:

- To explain a missing bean, extend `MissingBeanFailureAnalyzer` (`be.appify.prefab.core.diagnostics`) and name the bean
  types by class name.
- To explain a misconfiguration Prefab detects itself, throw `PrefabConfigurationException(description, action)`.
  `PrefabConfigurationFailureAnalyzer` prints it. It is an `IllegalStateException`, so existing handling keeps working.
- Register the analyzer under `org.springframework.boot.diagnostics.FailureAnalyzer` in `META-INF/spring.factories`, and
  cover it with a test that starts an application with `SpringApplicationBuilder` and checks the printed report.
  Name the property or bean and the fix, and never print credentials.

---

## Error: `... has a native query that cannot run against the in-memory test backend`

**Cause:** An `@IntegrationTest` called a `@RepositoryMixin` method that has a store-specific `@Query` or
`@Aggregation`. The in-memory backend cannot execute SQL or Mongo pipelines. The method fails only when it is called; a
warning is logged at startup for each such method.

**Fix:** Run the test class on real infrastructure with `@IntegrationTest(containers = true)`, or replace the native
query by a derived query (`findByCustomer`, `countByCategory`, ...), which works in memory.

---

## Test fails with `N event handler(s) failed during the test`

**Cause:** In memory, an event handler (`@EventHandler`, `@KafkaListener`, Pub/Sub or SQS subscriber) threw while
handling an event published during the test. The publishing request is not affected, because a real broker would not
propagate the failure either, but the test fails after it finishes, with the handler's exception as the cause. With
containers the same failure would end in retries and the dead-letter topic, and go unnoticed.

**Fix:** Fix the handler. A test that expects a handler to fail calls `InMemoryEventBus.drainFailures()` to clear the
recorded failures.

---

## My tests behave differently after upgrading

**Cause:** `@IntegrationTest` runs in memory by default now: no Postgres, MongoDB or broker container is started.
Tests that depend on database constraints (unique indexes, foreign keys, column sizes), rollbacks, native queries,
broker retries or dead-letter topics, Prefab Streams or a real schema registry pass or fail differently.

**Fix:** Add `containers = true` to those test classes (`@IntegrationTest(containers = true)`), or set
`prefab.test.containers: true` in `application-test.yml` to keep the previous behaviour for the whole module. See
[Feature Guides, 7.20](feature-guides.md#720-integration-tests-in-memory-or-containers) for the list of differences.

---

## Error: `No serialization format registered for topic [X]`

**Cause:** An event is being consumed or produced but the topic has not been registered in
`SerializationRegistry`.

**Fix:** Ensure the `@Event`-annotated class is in a package scanned by the annotation processor and
that the `SerializationRegistryConfiguration` generated bean is loaded (check component scanning).

---

## Error: `[X] was not found` (HTTP 404)

**Cause:** `NotFoundException` is thrown when `findById(id)` returns empty.

**Fix:** Verify the `id` path parameter is correct. Check that the aggregate was created and the migration
ran. For multi-tenant setups, verify the `@TenantId` field matches the current tenant.

---

## Error: `OptimisticLockingFailureException` (HTTP 409)

**Cause:** Two concurrent requests updated the same aggregate simultaneously. The `@Version` field
mismatch triggers `ConflictException`.

**Fix:** Retry the request with the latest `version` value from a fresh `GET` response.

---

## Error: `Compilation error: @TenantId field must not appear in @Create or @Update parameters`

**Cause:** A `@TenantId` field was included in a `@Create` constructor or `@Update` method.

**Fix:** Remove the `@TenantId` field from constructor/method parameters. The generated service populates
it automatically from `TenantContextProvider`.

---

## Error: `Compilation error: Cannot map event property to query parameter`

**Cause:** A `@Multicast` `parameters` value references a field or zero-argument accessor that does not
exist on the event contract.

**Fix:** Verify the `parameters` values match the event record field names or contract accessor names
exactly (case-sensitive).

---

## Error: `IllegalStateException` during event handling (causes retry)

**Cause:** A `@Multicast` handler found no aggregates to update. Prefab throws `IllegalStateException`
intentionally to trigger retry (the target aggregate may not have been created yet due to event ordering).

**Fix:** This is expected behaviour. Ensure the dead-letter configuration is set so that events that
permanently fail are routed to the DLT rather than blocking the consumer.

---

## Binary upload returns `400 Bad Request` with content-type error

**Cause:** The uploaded file's MIME type is not in the `@ContentType` whitelist.

**Fix:** Either use an allowed MIME type, or expand the `@ContentType` values on the parameter.

---

## Binary upload returns `400 Bad Request` for a missing part

**Cause:** The request does not follow the generated multipart format. The non-binary fields must be in a JSON part
named `body` (not plain form fields) and the file in a part named after the `Binary` parameter.

**Fix:** See [Generated Artefacts, 6.1](generated-artefacts.md#61-controller) for the request format and a `curl`
example.

---

## Generated migration script conflicts with existing schema

**Cause:** A rename may lack `@DbRename`, or a generated delta may include a destructive change such as dropping a
column or table. The processor reconstructs the prior schema from versioned SQL on the compile classpath; it does
not inspect the live database.

**Fix:** Review the generated SQL against the source-controlled migration history and current schema. Use
`@DbRename("oldName")` for a column rename, and use an explicit manual migration for data-preserving transitions.
Never edit a migration that has already been applied. See the
[Database Migration Lifecycle guide](database-migrations.md) for generation, Flyway checksums, MongoDB behavior,
and test-only reset limitations.

---

## Error: `[Prefab] ...: this hand-written ... duplicates ...` (controller, entity or repository)

**Cause:** Prefab found a hand-written `@RestController` handler, `@Entity` or Spring Data repository for an aggregate
that Prefab already exposes, persists or generates. See
[Generated Artefacts: Hand-Written Code Checks](generated-artefacts.md#613-hand-written-code-checks).

**Fix:** Delete the hand-written class and change the `@Aggregate` type instead (`@Create`, `@Update`, `@GetList`,
`@Filter`, ...); use `@RepositoryMixin` for extra queries. Only take over a generated class on purpose, with the
[source-file override](generated-artefacts.md#611-overriding-a-generated-class-escape-hatch). To keep a legacy class
while migrating, annotate it with `@SuppressWarnings("prefab.handwritten")`, or set
`-Aprefab.handwritten.severity=warning`.

---

## Warning: `db/migration/V<n>__....sql is a hand-written migration that changes tables Prefab already manages`

**Cause:** A versioned migration changes a table that Prefab derives from an aggregate.

**Fix:** Change the aggregate and compile so that Prefab generates the migration. If the script is a copy of a
generated one, keep the `__generated` suffix; if the DDL cannot be expressed in the model, name it
`V<n>__<description>_manual.sql` or add the comment `-- prefab:manual`. See
[Database Migration Lifecycle](database-migrations.md#hand-written-migrations-for-managed-tables).

---

## Events not published in integration tests (silent drop)

**Cause:** A plain unit test (without `@ExtendWith(PublishedEventsExtension.class)`) called
`publish()` on an aggregate before any Spring context started. In older Prefab versions this
permanently cached a `null` publisher for the JVM process lifetime (static singleton poisoning),
causing all subsequent event publishes in integration tests to be silently dropped.

**Fix (Prefab ≥ 0.7.7):** The static poisoning bug is fixed. `DomainEventPublisher` now uses a
resettable `volatile` field. If you still observe missing events in integration tests, verify that:

1. Your unit tests use `@ExtendWith(PublishedEventsExtension.class)` so the publisher is reset
   after each test method.
2. The `SpringDomainEventPublisher` bean is loaded — check that `@EnablePrefab` is present on
   your Spring Boot application class.

---

## Consumer deadlock: events on topic A block consumption of topic B

**Cause:** A retry loop on topic A occupies all threads, preventing consumption from topic B.

**Fix:** This is handled automatically by Prefab's per-topic executor strategy in
`PubSubSubscriberWriter`. For Kafka, ensure `@EventHandlerConfig(concurrency = ...)` is set high enough
and that the dead-letter configuration is correct so retries do not block indefinitely.

---

## Maven annotation processor not running (no generated sources)

**Cause:** The annotation processor dependency is missing or not on the `provided` scope.

**Fix:** Ensure `prefab-annotation-processor` is declared with `<scope>provided</scope>` in `pom.xml`.
Verify `maven.compiler.release` is set to `21`. Run `mvn clean compile` to force regeneration.
