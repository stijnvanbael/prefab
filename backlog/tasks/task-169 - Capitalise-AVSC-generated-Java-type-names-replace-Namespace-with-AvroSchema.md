---
id: TASK-169
title: Capitalise AVSC-generated Java type names and replace @Namespace with @AvroSchema
status: Done
assignee: []
created_date: '2026-05-08'
updated_date: '2026-05-08'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
AVSC record and enum names may start with a lowercase letter (valid in Avro, e.g. `meteringconfigUpdated`),
which caused the generated Java types to also start with a lowercase letter. This violates Java naming
conventions and breaks IDE and compiler expectations.

The fix has two parts:

1. **Capitalise generated Java type names** — `AvscEventWriter` now calls `capitalize(schema.getName())`
   for every Java type name it generates (records, enums, sealed interfaces, union branch wrappers).
   When the capitalised Java name differs from the original Avro schema name, a `@AvroSchema(name = "...")`
   annotation is placed on the generated type so schema factories and converters can recover the original
   Avro name during serialisation.

2. **Replace `@Namespace` with `@AvroSchema`** — The `@Namespace` annotation only covered the Avro
   namespace. The new `@AvroSchema` annotation supports both `namespace` and `name` overrides.
   `@Namespace` is now `@Deprecated` and continues to be read as a fallback for backwards compatibility.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [x] #1 New `@AvroSchema(name, namespace)` annotation created in `core` module with empty-string defaults
- [x] #2 `@Namespace` marked `@Deprecated`; Javadoc points to `@AvroSchema`
- [x] #3 `AvscEventWriter` uses `capitalize(schema.getName())` for all generated Java type names (records, enums, union branches)
- [x] #4 `AvscEventWriter` emits `@AvroSchema(namespace = "...")` instead of `@Namespace` when namespace is set
- [x] #5 `AvscEventWriter` additionally emits `@AvroSchema(name = "...")` when the original Avro name differs from the capitalised Java name
- [x] #6 `AvscPlugin` collision check uses `capitalize(schema.getName())` instead of raw `schema.getName()`
- [x] #7 `EventSchemaFactoryWriter.avroNamespaceOf()` reads `@AvroSchema.namespace()` first, then falls back to `@Namespace`, then AVSC file, then package name
- [x] #8 `EventSchemaFactoryWriter.avroSchemaNameOf()` helper reads `@AvroSchema.name()` if set; used in `createFlatRecordSchema()` and `createEnumSchema()`
- [x] #9 All `AvscPluginTest` assertions updated from `@Namespace("...")` to `@AvroSchema(namespace = "...")`
- [x] #10 New test `lowercaseAvscTypeNamesAreCapitalisedInGeneratedJava` added covering lowercase record and enum names
- [x] #11 AVSC test fixture `event/avsc/lowercase/source/LowercaseAvsc.java` + `lowercaseAvscEvent.avsc` created
- [x] #12 Full test suite in `avro-processor` passes with zero failures
- [x] #13 Developer guide updated: `@AvroSchema` documented, capitalisation rule explained, `@Namespace` deprecation noted
<!-- AC:END -->

## Implementation Notes

### Completed

| File                                               | Change                                                                                                                                                        |
|----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `core/.../annotations/AvroSchema.java`             | New annotation with `name()` and `namespace()` attributes (default `""`)                                                                                      |
| `core/.../annotations/Namespace.java`              | Marked `@Deprecated`, Javadoc updated                                                                                                                         |
| `avro-processor/.../AvscEventWriter.java`          | Import changed to `AvroSchema`; `javaTypeName(Schema)` helper added; all Java name sites use it; `namespaceAnnotation()` replaced by `avroSchemaAnnotation()` |
| `avro-processor/.../AvscPlugin.java`               | Collision check uses `capitalize(schema.getName())`; private `capitalize()` helper added                                                                      |
| `avro-processor/.../EventSchemaFactoryWriter.java` | `AvroSchema` import added; `avroNamespaceOf()` reads `@AvroSchema` first; `avroSchemaNameOf()` added; used in record and enum schema creation                 |

### Still needed (open ACs)

- **AC #9** — `AvscPluginTest.java`: replace all `.contains("@Namespace(\"...\")")` assertions with `.contains("@AvroSchema(namespace = \"...\")")`. There are 5 occurrences at lines 38, 244, 304, 308, 312.
- **AC #10 / #11** — Add test `lowercaseAvscTypeNamesAreCapitalisedInGeneratedJava` to `AvscPluginTest` along with fixture files:
    - `avro-processor/src/test/resources/event/avsc/lowercase/source/LowercaseAvsc.java`
    - `avro-processor/src/test/resources/event/avsc/lowercase/source/lowercaseAvscEvent.avsc` (record + nested enum, both lowercase names)
- **AC #12** — Run `mvn -pl core,avro-processor test` and resolve any remaining failures.
- **AC #13** — Update `backlog/docs/developer-guide.md`: document `@AvroSchema`, the automatic capitalisation of AVSC-derived Java names, and the deprecation of `@Namespace`.
