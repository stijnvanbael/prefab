# Troubleshooting

**Part of the [Prefab Developer Guide](developer-guide.md)**

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
