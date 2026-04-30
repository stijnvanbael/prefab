---
id: TASK-136
title: Extract AVRO module as stand-alone
status: In Progress
assignee: []
created_date: '2026-04-24 06:56'
updated_date: '2026-04-24 06:57'
labels: []
dependencies: []
priority: high
ordinal: 139500
---

## Goal

Slim down `prefab-core` and `prefab-annotation-processor` by extracting all Avro-related code into two new
top-level modules, and move transport-specific serializers/deserializers out of `core` into their respective
transport modules. After this task, `prefab-core` must have **zero** `org.apache.avro` dependencies and
`prefab-annotation-processor` must not reference Avro classes at all.

## Two New Avro Modules

| Module | Artifact | Purpose |
|--------|----------|---------|
| `avro/` | `prefab-avro` | **Runtime**: `SchemaSupport` utility class + `org.apache.avro:avro` compile dep |
| `avro-processor/` | `prefab-avro-processor` | **Annotation processor plugin**: Avro code-generation writers + ServiceLoader registration |

### Why two modules?

- **`prefab-avro`** is a runtime dependency needed by generated code and by transport serializers.
- **`prefab-avro-processor`** is a compile-time-only dependency (annotation processor). It must be on the
  `annotationProcessorPath`, not the application classpath. Mixing compile and processor concerns into one
  artifact forces every Avro user to carry processor code at runtime.

The `PrefabProcessor` discovers plugins via `ServiceLoader`. `prefab-avro-processor` registers its two
plugins (`AvscPlugin`, `AvroPlugin`) in its own
`META-INF/services/be.appify.prefab.processor.PrefabPlugin` file, so the main
`prefab-annotation-processor` no longer lists them.

## Current State

`prefab-core` has a hard compile-scope dependency on `org.apache.avro:avro`, forcing every consumer to pull
in Avro even when not using it. Avro is used in:

| File | Usage |
|------|-------|
| `core/avro/SchemaSupport.java` | Avro utility (schema construction, logical types) |
| `core/kafka/DynamicSerializer.java` | Uses `GenericRecord` |
| `core/kafka/DynamicDeserializer.java` | Uses `GenericRecord` |
| `core/pubsub/PubSubSerializer.java` | Uses `GenericDatumWriter`, `GenericRecord`, `EncoderFactory` |
| `core/pubsub/PubSubDeserializer.java` | Uses `GenericDatumReader`, `GenericRecord`, `DecoderFactory` |
| `core/sns/SnsSerializer.java` | Uses `GenericDatumWriter`, `GenericRecord`, `EncoderFactory` |
| `core/sns/SqsDeserializer.java` | Uses `GenericDatumReader`, `GenericRecord`, `DecoderFactory` |

`core/pom.xml` also carries a `provided` `kafka-streams-avro-serde` dep and a Confluent Maven repository.

`prefab-annotation-processor` lists `AvscPlugin` and `AvroPlugin` in its ServiceLoader file and uses
`kafka-streams-avro-serde` provided to compile the Avro writers.

## Target Architecture

```
prefab-core                 — no Avro imports whatsoever
prefab-avro                 — runtime: SchemaSupport + org.apache.avro:avro
prefab-avro-processor       — processor-time plugin: AvroPlugin, AvscPlugin, *Writer classes
prefab-kafka                — depends on prefab-avro; owns DynamicSerializer / DynamicDeserializer
prefab-pubsub               — depends on prefab-avro; owns PubSubSerializer / PubSubDeserializer
prefab-sns-sqs              — depends on prefab-avro; owns SnsSerializer / SqsDeserializer
prefab-annotation-processor — no Avro classes; ServiceLoader file no longer lists Avro plugins
```

## Plan

### Step 1 — Create `avro/` (runtime module)

1. Create `avro/pom.xml`:
   - artifactId: `prefab-avro`
   - compile dep: `org.apache.avro:avro:${avro.version}`
   - compile dep: `prefab-core`
   - publishing plugins (source, javadoc, gpg, central)

2. Create `avro/src/main/java/be/appify/prefab/avro/SchemaSupport.java`:
   - Copy from `core/avro/SchemaSupport.java`, change package to `be.appify.prefab.avro`.

3. Delete `core/src/main/java/be/appify/prefab/core/avro/SchemaSupport.java`.

### Step 2 — Create `avro-processor/` (processor plugin module)

1. Create `avro-processor/pom.xml`:
   - artifactId: `prefab-avro-processor`
   - compile dep: `prefab-annotation-processor` (for `PrefabPlugin`, `PrefabContext`, etc.)
   - compile dep: `prefab-avro` (for `SchemaSupport`)
   - compile dep: `org.apache.avro:avro:${avro.version}` (for `Schema` used in writers)
   - `provided` dep: `io.confluent:kafka-streams-avro-serde` (needed by `AvscPlugin`)
   - Confluent repository
   - publishing plugins

2. Move all 6 Java files from `annotation-processor/.../event/avro/` to
   `avro-processor/src/main/java/be/appify/prefab/avro/processor/`:
   - `AvroPlugin.java`
   - `AvscPlugin.java`
   - `AvscEventWriter.java`
   - `EventSchemaFactoryWriter.java`
   - `EventToGenericRecordConverterWriter.java`
   - `GenericRecordToEventConverterWriter.java`
   - Update package declarations to `be.appify.prefab.avro.processor`.
   - Update `SchemaSupport` imports to `be.appify.prefab.avro.SchemaSupport`.

3. Create `avro-processor/src/main/resources/META-INF/services/be.appify.prefab.processor.PrefabPlugin`
   containing:
   ```
   be.appify.prefab.avro.processor.AvscPlugin
   be.appify.prefab.avro.processor.AvroPlugin
   ```

4. Move the Avro test resources from `annotation-processor/src/test/resources/event/avro/` to
   `avro-processor/src/test/resources/event/avro/` and update expected file imports:
   - `be.appify.prefab.core.avro.SchemaSupport` → `be.appify.prefab.avro.SchemaSupport`

### Step 3 — Update `annotation-processor/pom.xml`

- Remove `io.confluent:kafka-streams-avro-serde` provided dep.
- Remove Confluent `<repositories>` block.
- Remove `AvscPlugin` and `AvroPlugin` entries from
  `src/main/resources/META-INF/services/be.appify.prefab.processor.PrefabPlugin`.

### Step 4 — Move transport serializers out of `core`

| Source (core) | Destination |
|---------------|-------------|
| `core/kafka/DynamicSerializer.java` | `kafka/src/.../be/appify/prefab/kafka/` |
| `core/kafka/DynamicDeserializer.java` | `kafka/src/.../be/appify/prefab/kafka/` |
| `core/pubsub/PubSubSerializer.java` | `pubsub/src/.../be/appify/prefab/pubsub/` |
| `core/pubsub/PubSubDeserializer.java` | `pubsub/src/.../be/appify/prefab/pubsub/` |
| `core/sns/SnsSerializer.java` | `sns-sqs/src/.../be/appify/prefab/snssqs/` |
| `core/sns/SqsDeserializer.java` | `sns-sqs/src/.../be/appify/prefab/snssqs/` |

Update package declarations and any intra-`core` imports in each moved file.

### Step 5 — Update `core/pom.xml`

Remove:
- `org.apache.avro:avro` compile dependency
- `io.confluent:kafka-streams-avro-serde` provided dependency
- Confluent `<repositories>` block
- `spring-boot-starter-kafka`, `spring-cloud-gcp-starter-pubsub`, `spring-cloud-aws-starter-sns/sqs` provided
  dependencies (these belong in their respective transport modules, not in core)

### Step 6 — Update transport module poms

**`kafka/pom.xml`**: add `prefab-avro` compile dep.

**`pubsub/pom.xml`**: add `prefab-avro` compile dep.

**`sns-sqs/pom.xml`**: add `prefab-avro` compile dep.

### Step 7 — Update root `pom.xml`

1. Add `<module>avro</module>` and `<module>avro-processor</module>` to the modules list
   (after `annotation-processor`, before `kafka`).
2. Add `prefab-avro` and `prefab-avro-processor` entries to `<dependencyManagement>`.

### Step 8 — Update `examples/avro/pom.xml`

Add `prefab-avro` and `prefab-avro-processor` as explicit dependencies. The processor should be scoped
to `provided` or declared in the annotation processor path.

### Step 9 — Verify

Run `mvn clean install` from the root. Fix any remaining compilation errors.

## Breaking Change Notice

- `SchemaSupport` moves from `be.appify.prefab.core.avro` to `be.appify.prefab.avro` — public-API break.
- Transport serializer classes change package — breaking for consumers referencing them in config.
- Generated code imports `be.appify.prefab.avro.SchemaSupport` — any hand-written code using the old import
  must update.

All breaking changes are acceptable for a pre-1.0 library at SNAPSHOT version.
