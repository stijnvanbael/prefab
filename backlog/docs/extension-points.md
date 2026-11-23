# Extension Point Guide

**Part of the [Prefab Developer Guide](developer-guide.md)**

---

## 8.0 Customization Coverage

Prefab's opinions can be adjusted at four levels. Prefer the first one that fits:

1. **Declarative**: annotations, `-Aprefab.*` compiler options and `prefab.*` properties.
2. **Supported hook**: a documented interface, plugin callback or bean.
3. **Whole-class override**: take over one generated class (§8.6). You maintain it from then on.
4. **Not customizable**: identity, optimistic locking and applied migration history are protected invariants.

[decision-009](../decisions/decision-009%20-%20Stable-extension-contracts-versus-internals.md) defines which of
these are stable contracts and the rules every extension point follows (no-op default, deterministic order,
defined conflict behaviour, regeneration safety). Everything not listed there, such as generated method bodies,
`*Writer` classes and undocumented bean names, is internal.

| Area | Declarative | Supported hooks | Escape hatch / gap |
|---|---|---|---|
| Plugin selection | `-Aprefab.plugin.<id>.enabled`, `@Generate(plugin, enabled, target)` ([feature guide](feature-guides.md)) | Custom `PrefabPlugin` (§8.1) | — |
| REST contracts and controllers | `@Aggregate(exposure, plural)`, operation `path` / `method` / `exposure`, `@Filter`, `@GetList(sorted)`, `@Doc`, `@ViewOf` ([annotation reference](annotation-reference.md)) | `PrefabPlugin.writeController`, `requestBodyParameter`, `mapRequestParameter`; handwritten adapters (§8.7) | §8.6 override |
| Application services | `Exposure.INTERNAL` operations, `@Computed` | `PrefabPlugin.writeService`, `getServiceDependencies`; domain events and `@EventHandler`s for side effects | §8.6 override; no per-operation metrics yet (TASK-366) |
| Repositories and queries | `@Filter`, `@Indexed`, `@Autocomplete` | `@RepositoryMixin` (§8.2), `PrefabPlugin.writeRepository`, replaceable `RangeQueryExecutor` / `FullTextQueryExecutor` beans | Row-level read scoping is planned (TASK-322) |
| Persistence and migrations | `@DbColumn`, `@DbRename`, `@DbDefaultValue`, `@DbDocument`, `@DbMigration(enabled)`, `@CustomType` | `PrefabPlugin.dataTypeOf`; manual `V{n}__*.sql` migrations ([database migrations](database-migrations.md)) | Applied generated migrations are immutable |
| Messaging and serialization | `@Event(topic, platform, serialization, publishTo, version, compatibility)`, `@EventHandlerConfig`, `prefab.dlt.*` ([configuration](configuration.md)) | `EventRegistryCustomizer` (§8.5), event `Migration` beans, Kafka producer/consumer factory customizers, `ContainerCustomizer`, `CommonErrorHandler`; `PrefabPlugin.avroSchemaOf` / `toAvroValueOf` / `fromAvroValueOf` | New brokers need a provider SPI (TASK-180) |
| Security | `@Security(enabled, authority, role)` | `HttpSecurityCustomizer`; a full chain replacement named `prefabSecurityFilterChain` | Field and row policies are planned (TASK-322) |
| Tenancy and audit | `@TenantId`, audit fields | `TenantContextProvider` (§8.4), `AuditContextProvider` (§8.3) | — |
| Tests | `@Generate(plugin = ..., target = OutputTarget.TEST)` | `PrefabPlugin.writeTestClient`; `prefab.test.*` container properties | Override `{Aggregate}Client` under `src/test/java` ([§6.9](generated-artefacts.md#69-generated-test-client)) |
| Observability | `prefab.streams.tracing.*` for streams | Spring Boot HTTP and Kafka container observations via the beans above | Generated services and consumers only log; TASK-366 |
| Infrastructure | Terraform generation through `GcpTerraformPlugin` | `PrefabPlugin.writeGlobalFiles` for other targets | — |

---

## 8.1 PrefabPlugin Interface

All Prefab code generation is driven by `PrefabPlugin` implementations loaded via the Java `ServiceLoader`
(see `META-INF/services/be.appify.prefab.processor.PrefabPlugin`).

To create a custom plugin:

1. Implement `be.appify.prefab.processor.PrefabPlugin`
2. Register it in `META-INF/services/be.appify.prefab.processor.PrefabPlugin`

```java
package com.example.processor;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.TypeSpec;

public class MyCustomPlugin implements PrefabPlugin {

    @Override
    public void writeController(ClassManifest manifest, TypeSpec.Builder builder) {
        // Add custom methods to the generated controller
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        // Add custom methods to the generated service
    }

    @Override
    public void writeAdditionalFiles(List<ClassManifest> manifests) {
        // Generate entirely new source files
    }
}
```

**`META-INF/services/be.appify.prefab.processor.PrefabPlugin`:**
```
com.example.processor.MyCustomPlugin
```

### PrefabPlugin Callback Methods

| Method                                              | When Called                | Purpose                                          |
|-----------------------------------------------------|----------------------------|--------------------------------------------------|
| `order()`                                           | Once at startup            | Position relative to other plugins (default `0`) |
| `initContext(PrefabContext)`                        | Once at startup            | Inject processing environment                    |
| `writeController(manifest, builder)`                | Per aggregate              | Add methods to controller                        |
| `writeService(manifest, builder)`                   | Per aggregate              | Add methods to service                           |
| `writeRepository(manifest, builder)`                | Per aggregate              | Add methods to repository                        |
| `writeTestClient(manifest, builder)`                | Per aggregate              | Add methods to test REST client                  |
| `writeAdditionalFiles(manifests)`                   | Once, after all aggregates | Generate extra source files                      |
| `additionalFileEventScope()`                        | With `writeAdditionalFiles`| Declare whether event-triggered reruns use local or consumed dependency events |
| `writeGlobalFiles(manifests, polymorphicManifests)` | Once, all rounds done      | Generate files spanning all aggregates           |
| `writeEventFiles()`                                 | Round 1 only               | Generate event types (before aggregate code)     |
| `getServiceDependencies(manifest)`                  | Per aggregate              | Add Spring beans injected into service           |
| `requestBodyParameter(parameter)`                   | Per method parameter       | Override how a parameter maps to request body    |
| `mapRequestParameter(parameter)`                    | Per method parameter       | Override how a request param maps to domain type |
| `dataTypeOf(typeManifest)`                          | Per `@CustomType` field    | Provide SQL column type for custom types         |
| `avroSchemaOf(typeManifest)`                        | Per `@CustomType` field    | Provide Avro schema for custom types             |
| `toAvroValueOf(type, value)`                        | Per `@CustomType` field    | Serialize custom type to Avro                    |
| `fromAvroValueOf(type, value)`                      | Per `@CustomType` field    | Deserialize Avro to custom type                  |

### Plugin order and member conflicts

Plugins run in ascending `order()`, then by fully-qualified class name, whatever the order of jars on the
processor path. All built-in plugins use `PrefabPlugin.DEFAULT_ORDER` (`0`). Return a positive value to run after
them, so your methods appear later in generated types, or a negative value to run first and win hooks where the
first non-empty result is used (`requestBodyParameter`, `mapRequestParameter`, `dataTypeOf`, `avroSchemaOf`, ...).

Plugins only add to generated types. Adding a method with the same name and parameter types, a field or a nested
type that Prefab or an earlier plugin already added to a generated controller, service, repository or test client
fails compilation with an error on the aggregate:

```
Plugin com.example.processor.MetricsPlugin adds method metrics() to generated OrderService, but
com.example.processor.AuditTrailPlugin already contributes it. Disable one of them for this aggregate with
@Generate(plugin = ..., enabled = false) or give the member a distinct signature.
```

javac's own "already defined" error for the generated file follows it. A plugin that overrides no hook leaves
generated output byte-identical, and recompiling unchanged sources regenerates identical files.

### Event discovery scope for plugins

`PrefabContext.eventElements()` is the safe default for plugin authors. It returns only `@Event` types that belong
to the current compilation, including AVSC-generated records emitted in the same module.

Use `PrefabContext.eventElementsIncludingConsumedDependencies()` only when a plugin intentionally generates
infrastructure for dependency events referenced from the current module, such as messaging producers or
documentation for consumed events.

If your plugin's `writeAdditionalFiles(...)` depends on consumed dependency events, also override
`PrefabPlugin.additionalFileEventScope()` and return
`PrefabContext.EventScope.CURRENT_COMPILATION_AND_CONSUMED_DEPENDENCIES`. Otherwise the default
`CURRENT_COMPILATION` scope is correct and avoids accidental regeneration of dependency-owned artefacts.

### Built-in Plugins

The following plugins are included in `prefab-annotation-processor`:

| Plugin                           | Handles                                                   |
|----------------------------------|-----------------------------------------------------------|
| `DbMigrationPlugin`              | `@DbMigration` → Flyway SQL scripts                       |
| `MongoMigrationPlugin`           | `@DbMigration` on MongoDB → JS migration scripts          |
| `MongoIndexPlugin`               | `@Indexed` for MongoDB                                    |
| `SerializationPlugin`            | `@Event` → `SerializationRegistryConfiguration`           |
| `EventSchemaDocumentationPlugin` | `@Event` → AsyncAPI schema                                |
| `StaticEventHandlerPlugin`       | Static `@EventHandler` methods                            |
| `ByReferenceEventHandlerPlugin`  | `@ByReference` event handlers                             |
| `MulticastEventHandlerPlugin`    | `@Multicast` event handlers                               |
| `CreatePlugin`                   | `@Create` → controller + service create method            |
| `GetByIdPlugin`                  | `@GetById` → controller + service getById method          |
| `DeletePlugin`                   | `@Delete` → controller + service delete method            |
| `GetListPlugin`                  | `@GetList` + `@Filter` → controller + service list method |
| `UpdatePlugin`                   | `@Update` → controller + service update method            |
| `BinaryPlugin`                   | `Binary` fields + `@Download`                             |
| `AggregateParameterPlugin`       | Handles `@Aggregate`-typed method parameters              |
| `TenantPlugin`                   | `@TenantId` → tenant filtering code                       |
| `AuditPlugin`                    | Audit field population code                               |
| `MotherPlugin`                   | Test object mother generation                             |

---

## 8.2 Repository Mixins

`@RepositoryMixin` is the **simplest extension point** for adding custom query methods to a generated
repository — no plugin authoring required.

Annotate an interface with `@RepositoryMixin(YourAggregate.class)` and Prefab adds it as a
super-interface of the generated `YourAggregateRepository`:

```java
// Simple Spring Data derived query
@RepositoryMixin(ChannelSummary.class)
public interface ChannelSummaryRepositoryMixin {
    List<ChannelSummary> findByChannel(Reference<Channel> channel);
}
```

For queries that cannot be expressed as derived method names, use `@Query`:

```java
@RepositoryMixin(UserStatus.class)
public interface UserStatusRepositoryMixin {
    @Query("""
            SELECT *
            FROM user_status
            WHERE "user" IN (
                SELECT id FROM "user"
                WHERE EXISTS (
                    SELECT 1 FROM UNNEST(channel_subscriptions) AS cs
                    WHERE cs = :channel
                )
            )
            """)
    List<UserStatus> findUserStatusesInChannel(Reference<Channel> channel);
}
```

The mixin is picked up automatically at compile time — no additional wiring or `@Component`
annotation is needed. Spring Data resolves derived queries and `@Query` methods through the
generated repository interface at startup.

> **When to use `@RepositoryMixin` vs. `@Filter`**
>
> | Need                                              | Use              |
> |---------------------------------------------------|------------------|
> | Standard equality / range filter on a REST `GET /list` endpoint | `@Filter` on the aggregate field |
> | Custom SQL, multi-table joins, or aggregations    | `@RepositoryMixin` + `@Query` |
> | Custom method used by an `@EventHandler @Multicast` | `@RepositoryMixin` (required) |

---

## 8.3 AuditContextProvider

Implement and register as a `@Bean` to integrate audit with your authentication mechanism. See
[Built-in Types — AuditContextProvider](built-in-types.md#auditcontextprovider).

---

## 8.4 TenantContextProvider

Implement and register as a `@Component` or `@Bean` to provide the tenant ID. See
[Built-in Types — TenantContextProvider](built-in-types.md#tenantcontextprovider).

---

## 8.5 EventRegistryCustomizer

Implement as a bean to register topic → event type and serialization mappings that Prefab does not derive from
`@Event`, for example for events owned by another service:

```java
@Bean
EventRegistryCustomizer legacyTopics() {
    return registry -> registry.register("legacy-orders", EventType.of(LegacyOrderPlaced.class),
            Event.Serialization.JSON);
}
```

Customizers are applied in `@Order` order before the `EventRegistry` is exposed. See
[Built-in Types — EventRegistryCustomizer](built-in-types.md#eventregistrycustomizer).

---

## 8.6 Source-File Override (Escape Hatch)

When `@RepositoryMixin`, `PrefabPlugin`, and the provider extension points are not sufficient,
you can take full ownership of any generated main-source class by placing a hand-crafted file
with the same qualified name under `src/main/java/`.

The annotation processor detects the existing source file, emits a compiler `NOTE`, and skips
generation — your file is compiled instead:

```
Note: Skipping generation of com.example.order.OrderService: a source file with this name already
      exists and will be used as-is. Remove it to let the annotation processor regenerate it.
```

To revert, delete the file from `src/main/java/` and the processor regenerates it on the next build.

> **Keep overrides in sync manually.** Prefab never updates an override file. If the aggregate's API
> changes (new `@Create`, `@Update`, etc.) you must update the override yourself.

Prefab reports hand-written controllers, entities, repositories and migrations that *duplicate* generated code,
but not overrides like this one or other documented extension points; see
[Hand-Written Code Checks](generated-artefacts.md#613-hand-written-code-checks).

For full details, worked examples, and a decision matrix on when to use this approach, see
[Generated Artefacts — §6.11 Overriding a Generated Class](generated-artefacts.md#611-overriding-a-generated-class-escape-hatch).

## 8.7 Application-Service Composition

Frontend-specific read DTOs and multi-step use cases can live in handwritten Spring `@Service` adapters that
inject Prefab-generated aggregate services. Use generated services for ordinary aggregate operations and a
`@RepositoryMixin` for a purpose-built batch read; keep presentation DTOs out of aggregate write records and
avoid per-item repository lookups. The [BFF composition decision](../decisions/decision-005%20-%20Backend-for-frontend-composition.md)
records transaction, authorization, tenant, timeout and deployment boundaries. Runnable examples and focused
tests are in `examples/kafka` under `dashboard` and `product/application`.

This is the escape hatch. The preferred direction is declarative: an aggregate-bound view model
(planned in TASK-326, with reference expansion in TASK-327) will generate the mapper, endpoint and tests so
that no handwritten adapter is needed for common screens. Generated services are
in-process collaborators; a separately deployed BFF must use versioned network contracts and cannot share
their local database transaction. In-process composition also does not grant authorization automatically:
derive identity from the authenticated principal, and explicitly scope custom repository queries for tenant
and row-level access.

## 8.8 Compiler Diagnostics and Failure Handling

The annotation processor never lets an expected problem surface as a raw processor stack trace. Every failure is
classified at the closest boundary that knows its context (the aggregate being generated, or the plugin hook being
called) and reported as a compiler diagnostic through `ErrorReporter`.

| Category | Examples | Reported as |
|----------|----------|-------------|
| Expected input | unsupported field type, type without a database column mapping, invalid `@Computed` or `@Security` use, circular foreign keys, `@AvscInterface` type that cannot be resolved | One `ERROR` on the offending element: `[tag] Element: rule violated` + `Suggested fix: ...` |
| Resource | malformed or unreadable AVSC file, existing `db/migration` SQL that Prefab cannot interpret | One `ERROR` that names the file, the cause and the fix |
| Infrastructure | generated file cannot be written (disk full, read-only output directory), migration or `asyncapi.json` cannot be written | One `ERROR` with the I/O cause and a hint to check the output directory |
| Programming / plugin bug | any other `RuntimeException` from Prefab or a plugin | One `ERROR` `[Prefab] Internal error in plugin <FQCN>` (or `the Prefab annotation processor`) with the full cause chain |
| Fatal JVM failure | `OutOfMemoryError`, `StackOverflowError`, `LinkageError`, `AssertionError` | Never caught; javac reports it and stops |

Compilation always fails when an error is reported; there is no success-shaped fallback.

**Independent errors are all reported, consequent errors are suppressed.** Each aggregate, polymorphic aggregate,
view and plugin hook runs behind its own `ProcessingGuard`, so a failure in one aggregate does not hide a failure
in another. The files of an aggregate are buffered while it is generated and are only emitted when all of its layers
succeeded, so a failed aggregate does not trigger a wave of `cannot find symbol` errors from half-generated code.
Identical errors reached through several code paths are reported once.

### Getting detailed evidence for an internal failure

Internal errors show the exception chain but not the stack trace. To get the full stack trace, re-run the
compilation with `-Aprefab.diagnostics.stacktrace=true`:

```xml
<compilerArgs>
    <arg>-Aprefab.diagnostics.stacktrace=true</arg>
</compilerArgs>
```

Include that output when reporting a bug. The cause chain and the failing plugin or component are always printed.

### Obligations for plugin authors

* **Report invalid user input with `PrefabProcessingException`**, not with `IllegalStateException`,
  `IllegalArgumentException` or a manual `Messager` call followed by a return:

  ```java
  throw new PrefabProcessingException(
          element,                       // where the error is shown (null = the aggregate being processed)
          "@MyAnnotation",               // tag in brackets
          "value() must not be empty",   // what is wrong
          "set value() to a topic name", // how to fix it (may be null)
          cause);                        // optional underlying exception
  ```

  The processor turns it into exactly one `ERROR`. Do not also print your own message.
* **Wrap I/O and resource failures** (unreadable files, `IOException` from the `Filer`) in a
  `PrefabProcessingException` with the file name and a corrective action. Never swallow them, never print them to
  `System.err`, and never continue generating output that depends on the failed step.
* **Let programming errors escape.** Do not catch `RuntimeException` to "keep going": the processor reports it as an
  internal error that names your plugin and keeps the cause. Never catch `Error`s.
* **Use `context.guard()`** when you run user-supplied or independent work yourself and want one failure not to stop
  the rest: `context.guard().run("plugin " + getClass().getName(), element, () -> ...)` reports failures with the same
  rules and returns `false` instead of throwing.
* **Write files through `context.fileWriter(...)` / `testFileWriter(...)`**, so that output of a failed aggregate is
  discarded.
