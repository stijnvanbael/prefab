---
id: TASK-306
title: Support java.util.UUID in Avro and AVSC plugins
status: Done
assignee: []
created_date: '2026-11-09 11:00'
labels:
  - avro
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`AvroPlugin` has no mapping for `java.util.UUID`, and `AvscPlugin` rejects the AVSC `uuid` logical type ("Unsupported logical type"). Add native support: a `UUID` event field maps to an Avro `string` with `logicalType: uuid`, and an AVSC string field with `logicalType: uuid` generates a `UUID` record component.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A `UUID` field in an `@Event(serialization = AVRO)` record produces a `string`/`uuid` schema field.
- [x] #2 Generated converters serialise `UUID` to a string and deserialise it back (including nullable fields).
- [x] #3 An AVSC field with `logicalType: uuid` generates a `java.util.UUID` record component.
- [x] #4 Unit tests cover schema, round-trip conversion and AVSC generation.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Treat `UUID` as a logical type next to `Instant`/`LocalDate`/`Duration`/`BigDecimal`:
- `AvroPlugin.isLogicalType`, `EventSchemaFactoryWriter.isLogicalType`/`createLogicalSchema`.
- `EventToGenericRecordConverterWriter.logicalType`: `value.toString()`.
- `GenericRecordToEventConverterWriter`: top-level `SchemaSupport.getUuid(...)`, nested `UUID.fromString(value.toString())`.
- `AvscEventWriter.toLogicalTypeName`: `uuid` -> `UUID`.
<!-- SECTION:PLAN:END -->

## Implementation Notes

UUID is treated as a logical type (string/uuid). Added `SchemaSupport.getUuid`, schema/converter support in the Avro writers, and `uuid` -> `UUID` in `AvscEventWriter`. Tests: `SchemaSupportTest`, `EventSchemaFactoryWriterTest.uuidFields`, `AvscPluginTest.uuidLogicalTypeAvscEvent`.
