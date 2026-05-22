# Contributing to Prefab

Thank you for taking an interest in Prefab! This guide covers everything you need to become an
effective contributor: the repository layout, how to build and test the project, and how to write
your own code-generation plugin.

---

## Table of Contents

- [Project Structure](#project-structure)
- [Building the Project](#building-the-project)
- [Running the Tests](#running-the-tests)
- [Writing a PrefabPlugin](#writing-a-prefabplugin)
- [Coding Standards](#coding-standards)
- [Submitting Changes](#submitting-changes)

---

## Project Structure

The repository is a multi-module Maven project. Every module produces a separate artifact under the
`be.appify.prefab` group ID.

```
prefab/
├── core/               # Framework types, annotations (@Aggregate, @Event, …), interfaces
├── annotation-processor/  # Compile-time code generator — PrefabProcessor + all plugins
├── postgres/           # Spring Data JDBC + Flyway + PostgreSQL persistence support
├── mongodb/            # Spring Data MongoDB persistence support
├── kafka/              # Apache Kafka producer/consumer configuration
├── avro/               # Avro serialization/deserialization runtime support
├── avro-processor/     # AVSC schema → Java record code generation (annotation processor)
├── pubsub/             # Google Cloud Pub/Sub support
├── sns-sqs/            # AWS SNS publisher + SQS consumer support
├── streams/            # Kafka Streams DSL (source/sink baseline)
├── security/           # Spring Security + OAuth2 integration
├── openapi/            # SpringDoc OpenAPI / Swagger UI
├── async-api/          # AsyncAPI specification generation
├── test/               # Testcontainers-based integration test support + test utilities
├── terraform/          # GCP Terraform configuration generation
├── examples/           # Runnable example projects (avro, kafka, mongodb, pubsub, sns-sqs, streams)
└── backlog/            # Project backlog, decisions, and developer guide
    └── docs/           # Developer Guide (index: backlog/docs/developer-guide.md)
```

### Key source directories in `annotation-processor`

```
annotation-processor/src/main/java/be/appify/prefab/processor/
├── PrefabProcessor.java      # Main annotation processor entry point
├── JavaFileWriter.java       # Writes generated main-source files via the APT Filer
├── TestJavaFileWriter.java   # Writes generated test-support files to target/prefab-test-sources/
├── PrefabPlugin.java         # Plugin interface (ServiceLoader-based extension point)
├── PrefabContext.java        # Processing context passed to every plugin
├── ClassManifest.java        # Rich model of a single @Aggregate class
├── rest/                     # HTTP layer plugins (Create, Update, Delete, GetById, GetList, …)
├── event/                    # Event consumer and schema plugins
├── dbmigration/              # Flyway / MongoDB migration plugins
├── audit/                    # Audit field population plugin
├── tenant/                   # Multi-tenancy plugin
└── mother/                   # Test object-mother generation plugin
```

---

## Building the Project

**Prerequisites:** Java 21+, Maven 3.9+, Docker (required only for integration tests).

```bash
# Full build — skips integration tests
mvn install -DskipTests

# Full build including all tests
mvn verify
```

### Building a single module

```bash
cd annotation-processor
mvn test
```

---

## Running the Tests

### Unit tests (fast, no Docker required)

```bash
# All unit tests in the annotation-processor module
mvn -pl annotation-processor test

# A single test class
mvn -pl annotation-processor test -Dtest=RestWriterTest
```

The annotation-processor tests use [google/compile-testing](https://github.com/google/compile-testing)
to run the annotation processor inside an in-memory Java compiler. A typical test looks like this:

```java
@Test
void requestValidationAnnotationsAreGenerated() {
    var compilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("rest/validation/source/Product.java"));

    assertThat(compilation).succeeded();
    assertThat(compilation)
            .generatedSourceFile("rest.validation.application.CreateProductRequest")
            .contentsAsUtf8String()
            .contains("@NotNull");
}
```

Test source fixtures live under `annotation-processor/src/test/resources/` and are loaded by
`ProcessorTestUtil.sourceOf(path)`.

### Integration tests (require Docker)

Integration tests live in the `examples/` modules and use Testcontainers to spin up real PostgreSQL,
Kafka, or MongoDB instances:

```bash
# Run all integration tests — Docker must be running
mvn verify -pl examples/kafka,examples/mongodb,examples/avro
```

---

## Writing a `PrefabPlugin`

`PrefabPlugin` is the primary extension point for adding code-generation behaviour to Prefab. Plugins
are discovered at compile time via the Java `ServiceLoader`, so they need no Spring wiring.

### Step 1 — Pick a module to host your plugin

A plugin is an annotation processor component. Locate it in:
- A **separate Maven module** that declares `prefab-annotation-processor` as a `provided` dependency.
- Or directly **inside `annotation-processor`** for built-in plugins that ship with the framework.

### Step 2 — Implement `PrefabPlugin`

Override only the callback methods you need — all of them have default no-op implementations:

```java
package com.example.processor;

import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import be.appify.prefab.processor.PrefabPlugin;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

public class MetricsPlugin implements PrefabPlugin {

    private PrefabContext context;

    @Override
    public void initContext(PrefabContext context) {
        this.context = context;          // store context for use in later callbacks
    }

    @Override
    public void writeService(ClassManifest manifest, TypeSpec.Builder builder) {
        // Add a custom method to every generated *Service class
        var method = MethodSpec.methodBuilder("metricsTag")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", manifest.simpleName())
                .build();
        builder.addMethod(method);
    }
}
```

### Important callbacks

| Method                                              | When called                | Typical use                                  |
|-----------------------------------------------------|----------------------------|----------------------------------------------|
| `initContext(PrefabContext)`                         | Once at startup            | Store `PrefabContext` for later callbacks     |
| `writeController(manifest, builder)`                | Per aggregate              | Add endpoints or fields to the controller    |
| `writeService(manifest, builder)`                   | Per aggregate              | Add helper methods to the service            |
| `writeRepository(manifest, builder)`                | Per aggregate              | Add custom query methods to the repository   |
| `writeTestClient(manifest, builder)`                | Per aggregate              | Extend the generated REST test client        |
| `writeAdditionalFiles(manifests)`                   | Once, after all aggregates | Generate entirely new source files           |
| `writeGlobalFiles(manifests, polymorphicManifests)` | Once, all rounds done      | Generate files that span all aggregates      |

See the full table in [Extension Point Guide §8.1](backlog/docs/extension-points.md#81-prefabplugin-interface).

### Step 3 — Register via `ServiceLoader`

Create the file:

```
src/main/resources/META-INF/services/be.appify.prefab.processor.PrefabPlugin
```

Containing the fully qualified class name:

```
com.example.processor.MetricsPlugin
```

Or, if you use `@AutoService` (from `com.google.auto.service:auto-service`), annotate the class:

```java
@AutoService(PrefabPlugin.class)
public class MetricsPlugin implements PrefabPlugin { … }
```

### Step 4 — Write a test

Use `compile-testing` to verify your plugin's output without running a full Spring Boot application:

```java
@Test
void metricsTagMethodIsGeneratedOnService() {
    var compilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("myfeature/source/Widget.java"));

    assertThat(compilation).succeeded();
    assertThat(compilation)
            .generatedSourceFile("myfeature.application.WidgetService")
            .contentsAsUtf8String()
            .contains("metricsTag");
}
```

Place the fixture Java file at
`annotation-processor/src/test/resources/myfeature/source/Widget.java`.

### Using `PrefabContext` in a plugin

`PrefabContext` is passed to every plugin via `initContext` and provides:

| Method                                     | Returns                                                          |
|--------------------------------------------|------------------------------------------------------------------|
| `processingEnvironment()`                  | The APT `ProcessingEnvironment` (elements, types, messager, filer) |
| `eventElements()`                          | `@Event`-annotated types in the current compilation              |
| `eventElementsIncludingConsumedDependencies()` | Events from the current module **and** its dependencies      |
| `roundEnvironment()`                       | The current APT `RoundEnvironment`                               |
| `fileWriter(packageSuffix)`                | A `JavaFileWriter` for generating main-source files              |
| `testFileWriter(packageSuffix)`            | A `TestJavaFileWriter` for generating test-support files         |

---

## Coding Standards

All code in this repository follows the standards defined in [AGENTS.md](AGENTS.md):

- **Java 25 target.** Use records, sealed interfaces, pattern matching, text blocks, `var`.
- **Clean Code** (Robert C. Martin): small focused methods, meaningful names, no comments that explain
  what the code already says.
- **Hexagonal architecture**: domain model → application services → infrastructure/adapters.
- **Tests first** where practical. Use JUnit 5 + AssertJ + Mockito; follow Arrange-Act-Assert.
- **Zero compiler warnings.** No raw types, no unused imports.
- **Conventional commits**: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`.

### Documentation rule

When you change or add any Prefab feature — annotation, generated artefact, module, extension point,
or configuration property — **update the relevant document** in `backlog/docs/` in the same commit.
The entry point is [backlog/docs/developer-guide.md](backlog/docs/developer-guide.md).

---

## Submitting Changes

1. **Fork** the repository and create a feature branch.
2. **Look for an existing backlog task** before starting work — check `backlog/tasks/`. If none
   exists, create one so the intent is on record.
3. **Write tests first** (TDD). The full test suite must remain green.
4. **Commit in small, focused increments** with conventional-commit messages.
5. **Update `backlog/docs/`** if your change affects any documented feature or extension point.
6. **Open a pull request** against `main` — include a brief description of *why* the change is needed
   and a reference to the backlog task ID.

