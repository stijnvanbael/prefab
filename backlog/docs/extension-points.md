# Extension Point Guide

**Part of the [Prefab Developer Guide](developer-guide.md)**

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

## 8.5 SerializationRegistryCustomizer

Implement to programmatically register topic → serialization format mappings:

```java
@Bean
public SerializationRegistryCustomizer myCustomizer() {
    return registry -> registry.register("my-topic", Event.Serialization.AVRO);
}
```

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

For full details, worked examples, and a decision matrix on when to use this approach, see
[Generated Artefacts — §6.11 Overriding a Generated Class](generated-artefacts.md#611-overriding-a-generated-class-escape-hatch).

