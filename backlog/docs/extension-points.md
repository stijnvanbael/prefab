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
| `writeGlobalFiles(manifests, polymorphicManifests)` | Once, all rounds done      | Generate files spanning all aggregates           |
| `writeEventFiles()`                                 | Round 1 only               | Generate event types (before aggregate code)     |
| `getServiceDependencies(manifest)`                  | Per aggregate              | Add Spring beans injected into service           |
| `requestBodyParameter(parameter)`                   | Per method parameter       | Override how a parameter maps to request body    |
| `mapRequestParameter(parameter)`                    | Per method parameter       | Override how a request param maps to domain type |
| `dataTypeOf(typeManifest)`                          | Per `@CustomType` field    | Provide SQL column type for custom types         |
| `avroSchemaOf(typeManifest)`                        | Per `@CustomType` field    | Provide Avro schema for custom types             |
| `toAvroValueOf(type, value)`                        | Per `@CustomType` field    | Serialize custom type to Avro                    |
| `fromAvroValueOf(type, value)`                      | Per `@CustomType` field    | Deserialize Avro to custom type                  |

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

See [Feature Guides — Repository Mixins](feature-guides.md#710-repository-mixins). This is the simplest
extension point for adding custom queries without writing a plugin.

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

