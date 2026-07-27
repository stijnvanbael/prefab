---
id: TASK-269
title: Support partitioningKey metadata for AVSC-generated events
status: Done
assignee: []
created_date: '2026-07-27 08:36'
updated_date: '2026-07-27 11:23'
labels:
  - annotation-processor
  - avro
  - events
  - kafka
dependencies: []
priority: medium
---

## Description

When an event contract uses `@Avsc`, the generated records cannot declare `@PartitioningKey` themselves, so there is currently no developer-facing way to configure a partitioning key for AVSC-generated event types. Add support for defining partitioning-key metadata on `@Avsc` so each referenced schema can declare which generated property should be used as the event partitioning key.

Prefer a shape that lives next to `value()` and supports multi-schema contracts explicitly, for example a list of entries where each entry declares the AVSC file and the property name to use as the partitioning key.

## Analysis

- The current `@Avsc` contract in `core` only exposes `String[] value()`, so every consumer assumes schema paths come exclusively from `value()`.
- The AVSC path is currently consumed in three places:
  - `avro-processor/.../AvscPlugin` to generate AVSC-backed event records
  - `avro-processor/.../EventSchemaFactoryWriter` to resolve AVSC-backed schema factories and named types
  - `kafka/.../KafkaPlugin` to generate AVSC `EventTypeRegistrar` classes
- Partitioning-key extraction for non-AVSC events is already centralized in generated registrars via `annotation-processor/.../EventTypeRegistrarWriter`, which can emit `registry.register(..., event -> ...)`.
- AVSC-generated records already flow through the same producer/runtime path (`EventRegistry#keyFor(...)`), so this task only needs to supply the missing registrar key-extractor metadata for generated AVSC event types.
- The new annotation contract should therefore:
  - keep `value()` as the preferred path when all referenced AVSC events share the same `@PartitioningKey` contract method,
  - add explicit `files = { @AvscFile(path = \"...\", keyProperty = \"...\") }` support,
  - normalize both inputs into one schema list with duplicate/conflict validation,
  - validate `keyProperty` against the referenced AVSC schema before code generation,
  - and pass the resolved property into Kafka AVSC registrar generation.

## Acceptance Criteria

- [ ] #1 `@Avsc` exposes a developer-facing way to configure a partitioning key for each referenced AVSC schema without editing the generated event classes
- [ ] #2 The configuration supports both single-schema and multi-schema `@Avsc` usage, and the mapping from AVSC file to generated property name is explicit and unambiguous
- [ ] #3 The annotation processor reports clear compile-time errors when the configuration references an AVSC file that is not listed in `value()`, a property that does not exist in the corresponding schema, or duplicate/conflicting mappings for the same schema
- [ ] #4 Generated event registration and producer infrastructure uses the configured property as the partitioning key extractor for AVSC-generated events wherever Prefab currently honours event partitioning keys
- [ ] #5 Documentation and at least one example show the final annotation shape and explain the fallback behaviour when no AVSC partitioning key is configured

## Implementation Notes

- Keep non-`@Avsc` event behaviour unchanged; this task is only about closing the AVSC gap.
- Reuse the existing event registry and partitioning-key extraction flow instead of introducing a separate AVSC-specific mechanism.
- The final API shape should be a nested annotation such as `files = { @AvscFile(path = "...", keyProperty = "...") }`
- If the final API shape materially affects the public annotation contract, capture the decision in `backlog/decisions/`.

## Completion Notes

- Added `@AvscFile` plus `Avsc#files()` while keeping `value()` as the preferred shared-key path.
- Added shared `AvscFiles.resolve(...)` normalization so AVSC schema paths are resolved consistently across AVSC generation, schema-factory lookup, and Kafka registrar generation.
- Added compile-time validation for:
  - empty/duplicate schema declarations across `value()` and `files()`
-  `keyProperty` values that do not match a field in the referenced AVSC schema
-  `@PartitioningKey` contract methods whose name is not present in a referenced AVSC schema
- Updated Kafka AVSC registrar generation to emit `EventRegistry` key extractors from either the contract `@PartitioningKey` method or per-file `keyProperty`.
  - Corrected the public contract so `@Avsc("...")` remains the preferred form when every generated event shares the same partitioning key via a contract interface method.
  - Updated the AVSC annotation docs and feature guide with the explicit `files = @AvscFile(...)` shape and partitioning-key behaviour.

## Verification

- Focused suite passed:
  - `mvn -pl core,annotation-processor,avro-processor,kafka -am -Dtest=AvscPluginTest,SerializationPluginTest,KafkaEventTypeRegistrarWriterTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Full repository suite was run with `mvn test` and failed outside this change in `examples/avro` because Testcontainers could not complete the `apache/kafka:4.0.2` Docker image pull for `SaleIntegrationTest`.
