---
id: TASK-270
title: Add Avro decimal logical type support
status: Done
assignee: []
created_date: '2026-08-03 00:00'
labels:
  - avro
  - avro-processor
  - annotation-processor
dependencies: []
priority: medium
---

## Description

Add support for Avro `decimal` logical type in AVSC-driven event generation and generated converters.

## Acceptance Criteria

- [x] #1 AVSC fields with logical type `decimal` generate `BigDecimal` fields in Java records (including nullable and list fields)
- [x] #2 Generated `Event -> GenericRecord` converter serialises decimal values with schema-aware decimal conversion
- [x] #3 Generated `GenericRecord -> Event` converter deserialises decimal values with schema-aware decimal conversion
- [x] #4 Regression tests cover generated code for decimal mappings and conversions

## Analysis

- Decimal logical types are currently not recognised by `AvscEventWriter` (unsupported logical type error).
- Existing converter writers handle temporal logical types only; decimal requires explicit conversion using Avro decimal semantics (`bytes`/`fixed` + precision/scale).
- `SchemaSupport` lacks reusable decimal conversion helpers.
- Follow-up: code-first `@Event(serialization = AVRO)` schema generation still treated `BigDecimal` as a primitive standard type, yielding an "Unsupported standard type" error in `EventSchemaFactoryWriter`.

## Progress

- Added decimal mapping in AVSC writer to `BigDecimal`.
- Added schema-aware decimal conversion helpers in `SchemaSupport`.
- Updated generated converter writers to use decimal conversion helpers.
- Added AVSC decimal fixtures and tests validating generated record/converter sources.

## Implementation Notes

- `AvscEventWriter` now maps Avro logical type `decimal` to `java.math.BigDecimal`.
- `SchemaSupport` now exposes:
  - `toDecimalAvro(BigDecimal, Schema)` for serialisation to `bytes`/`fixed`
  - `fromDecimalAvro(Object, Schema)` for deserialisation from `ByteBuffer`/`GenericData.Fixed`
  - `getFieldSchema(GenericRecord, String)` for schema-aware generated conversion code.
- `EventToGenericRecordConverterWriter` now serialises `BigDecimal` via `SchemaSupport.toDecimalAvro(...)`.
- `GenericRecordToEventConverterWriter` now deserialises `BigDecimal` via `SchemaSupport.fromDecimalAvro(...)`, including list elements where decimal item schema is derived from array schema.
- Added decimal AVSC fixture and tests in `AvscPluginTest` to verify generated record signatures and converter code paths.
- Follow-up: `EventSchemaFactoryWriter` now treats `BigDecimal` as a logical type for code-first AVRO events, generating `bytes` schemas annotated with `LogicalTypes.decimal(19, 4)`.
- Verification: `mvn -pl avro,avro-processor test` passes.


