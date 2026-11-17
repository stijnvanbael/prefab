# Prefab

Prefab is a Java annotation processor for Spring Boot. Describe domain aggregates and their operations in Java; at
compile time, Prefab can generate selected application infrastructure such as REST endpoints and contracts,
repositories, migrations, event plumbing, and test helpers. Features are opt-in and depend on the modules you choose.

**Start here: [Prefab Developer Guide](backlog/docs/developer-guide.md)** — setup, module selection, feature guides,
generated artefacts, configuration, and extension points.

Prefab reduces repetitive scaffolding; it does not replace domain design or application architecture. Your team still
owns business rules, integrations that fall outside the supported modules, and production operations.

## Capabilities in the current source

This inventory describes functionality implemented in this repository, not a promise that every item is included in
every published artifact or enabled by the core dependency. Add only the modules you need, and check the version you
use for the features it contains. Items that appear only in the [backlog](backlog/tasks) are planned, not supported
features.

| Area | What Prefab provides |
|---|---|
| [Domain modeling](backlog/docs/annotation-reference.md#41-aggregate-annotations) | Aggregate-root annotations, typed references, [nested value objects](backlog/docs/feature-guides.md#79-nested-value-objects-and-embedded-types), and compile-time checks. |
| [REST and generated contracts](backlog/docs/feature-guides.md#71-rest-crud-operations) | Opt-in create, update, delete, get-by-ID, list, filters, and generated [request/response types](backlog/docs/generated-artefacts.md#64-requestresponse-records); optional [OpenAPI](backlog/docs/modules.md#prefab-modules). |
| [Persistence, migrations, and indexes](backlog/docs/generated-artefacts.md#66-database-migration-scripts) | PostgreSQL/Spring Data JDBC or MongoDB repositories; opt-in Flyway or MongoDB migration generation and [declared indexes](backlog/docs/annotation-reference.md#indexed). See [database annotations](backlog/docs/annotation-reference.md#45-database-annotations) and [persistence modules](backlog/docs/modules.md#prefab-modules). |
| [Messaging and schema evolution](backlog/docs/feature-guides.md#72-event-publishing) | Kafka, Google Pub/Sub, and AWS SNS/SQS event integrations; Kafka JSON/Avro support, generated event handlers, and [event version compatibility and migrations](backlog/docs/annotation-reference.md#event-versioning-and-migrations). Serializer/transport combinations vary by module; optional [AsyncAPI docs](backlog/docs/modules.md#prefab-modules). |
| [Kafka streams](backlog/docs/feature-guides.md#715-streams-dsl-baseline-kafka-sourcesink) | An opt-in source/sink DSL for building Kafka Streams topologies. |
| [Security, tenancy, and audit](backlog/docs/feature-guides.md#75-audit-trail) | Optional [Spring Security module](backlog/docs/modules.md#prefab-modules), [per-operation authorization](backlog/docs/annotation-reference.md#security), tenant-aware aggregate access, and audit fields with replaceable runtime providers. See [multi-tenancy](backlog/docs/feature-guides.md#76-multi-tenancy). |
| [Validation, binary files, and search](backlog/docs/annotation-reference.md#48-validation-annotations) | Bean Validation on domain operations, validated [binary uploads/downloads](backlog/docs/generated-artefacts.md#61-controller), PostgreSQL/MongoDB [full-text filters](backlog/docs/annotation-reference.md#filter), and generated [autocomplete](backlog/docs/feature-guides.md#717-autocomplete-endpoints). |
| [Test helpers](backlog/docs/generated-artefacts.md#69-generated-test-client) | Generated [test clients](backlog/docs/generated-artefacts.md#69-generated-test-client), [object mothers](backlog/docs/generated-artefacts.md#610-generated-object-mothers-test-sources), [assertions](backlog/docs/generated-artefacts.md#68-generated-assertion-classes), [event test helpers](backlog/docs/feature-guides.md#712-unit-testing-domain-events), and optional [Testcontainers support](backlog/docs/modules.md#prefab-modules). |
| [Observability](backlog/docs/configuration.md#application-properties) | Spring Boot Actuator through `prefab-core`, with baseline health and info endpoints; this is not a full observability platform. |
| [Infrastructure generation](backlog/docs/modules.md#gcp-terraform-infrastructure-generation) | The optional Terraform module generates selected GCP resources from the application model. It does not provision infrastructure or replace infrastructure review and operations. |
| [Extension points](backlog/docs/extension-points.md#81-prefabplugin-interface) | [Repository mixins](backlog/docs/extension-points.md#82-repository-mixins), code-generation plugins, runtime providers/customizers, and a documented [source-file override escape hatch](backlog/docs/extension-points.md#86-source-file-override-escape-hatch). |

## Quick start

Use Java 21 or newer, Spring Boot 4.x, and Maven. Add `prefab-core`, one persistence module, and the annotation
processor; add messaging or other modules only when needed. Use the same current released Prefab version for each
artifact (replace `RELEASE_VERSION` below).

```xml
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-core</artifactId>
    <version>RELEASE_VERSION</version>
</dependency>
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-postgres</artifactId>
    <version>RELEASE_VERSION</version>
</dependency>
<dependency>
    <groupId>be.appify.prefab</groupId>
    <artifactId>prefab-annotation-processor</artifactId>
    <version>RELEASE_VERSION</version>
    <scope>provided</scope>
</dependency>
```

For MongoDB, use `prefab-mongodb` instead of `prefab-postgres`. Add `@EnablePrefab` to the Spring Boot application and
annotate an aggregate and its operations:

In `Application.java`:

```java
import be.appify.prefab.core.spring.EnablePrefab;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePrefab
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

In `Product.java`:

```java
import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        String name
) {
    @Create
    public Product(String name) {
        this(Reference.create(), 0L, name);
    }
}
```

See [Getting Started](backlog/docs/getting-started.md) for a complete application setup, and the
[module matrix](backlog/docs/modules.md) for the dependencies required by each feature.

## Real-world customization

Prefab keeps generated infrastructure customizable without making generated sources the normal place for application
code:

- **Event-driven projections and custom queries:** [`ChannelSummary`](examples/kafka/src/main/java/be/appify/prefab/example/kafka/channelsummary/ChannelSummary.java)
  combines event handlers, multicast updates, and a list endpoint. Its
  [`@RepositoryMixin`](examples/kafka/src/main/java/be/appify/prefab/example/kafka/channelsummary/ChannelSummaryRepositoryMixin.java)
  adds the `findByChannel` query used by those handlers. See [repository mixins](backlog/docs/extension-points.md#82-repository-mixins).
- **Per-aggregate code generation:** [`@Generate`](backlog/docs/feature-guides.md#716-per-aggregate-plugin-overrides)
  enables, disables, or redirects a generator for one aggregate. The
  [compile-time integration tests](annotation-processor/src/test/java/be/appify/prefab/processor/GeneratePluginOverrideIntegrationTest.java)
  exercise these overrides; implement a [`PrefabPlugin`](backlog/docs/extension-points.md#81-prefabplugin-interface)
  when you need to add generated files or extend generated types.
- **Runtime tenant, audit, and security policy:** mark the aggregate's tenant discriminator with `@TenantId`, then supply
  [`TenantContextProvider`](backlog/docs/built-in-types.md#tenantcontextprovider) and
  [`AuditContextProvider`](backlog/docs/built-in-types.md#auditcontextprovider) beans,
  register an [`HttpSecurityCustomizer`](security/src/main/java/be/appify/prefab/core/security/HttpSecurityCustomizer.java),
  and declare endpoint rules with [`@Security`](backlog/docs/annotation-reference.md#security). The
  [Customer example](examples/avro/src/main/java/be/appify/prefab/example/avro/customer/Customer.java) combines
  operation-level rules with domain events; generated service and security code remains processor-owned.

For a last-resort, hand-maintained replacement, see the [generated-class escape hatch](backlog/docs/generated-artefacts.md#611-overriding-a-generated-class-escape-hatch)
and its maintenance trade-offs.

## Scope and trade-offs

Prefab generates infrastructure for supported aggregate shapes and module integrations; it is not a general-purpose
application generator. Put business behavior in your domain methods, use repository mixins for queries beyond the
generated filters, and manage integrations or infrastructure outside the supported modules explicitly. Generated
source overrides require manual synchronization when the domain model changes. Consult the guide for exact behavior
and the [backlog](backlog/tasks) for work that is planned but not yet implemented.

## IDE support

Enable annotation processing in your IDE. IntelliJ IDEA can delegate builds to Maven under
`Settings > Build, Execution, Deployment > Build Tools > Maven > Runner`; IDE partial compilation may not run the
processor in the same way as a Maven build.
