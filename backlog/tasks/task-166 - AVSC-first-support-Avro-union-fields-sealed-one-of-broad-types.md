---
id: TASK-166
title: 'AVSC-first: support Avro union fields (sealed one-of, broad types)'
status: Done
assignee: []
created_date: '2026-05-08 08:16'
updated_date: '2026-05-21 06:21'
labels: []
dependencies: []
ordinal: 46200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The AVSC-first code generator (AvscEventWriter) currently rejects any field whose Avro schema is a union with more than one non-null branch (e.g. [double, string] or [null, RecordA, RecordB]). This prevents nested records that contain such fields from being generated at all. We need to support all valid Avro union shapes as type-safe sealed interfaces in generated Java code, and wire up schema factories and converters accordingly.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 AvscEventWriter generates a sealed interface for each multi-branch union field
- [x] #2 Sealed interface has one permitted record per union branch (scalar, record, enum, array)
- [x] #3 Nullable unions (null + one type) remain as before: @Nullable field, no sealed interface
- [x] #4 EventSchemaFactoryWriter emits a union schema for sealed union fields
- [x] #5 EventToGenericRecordConverterWriter serialises each sealed branch via a switch expression
- [x] #6 GenericRecordToEventConverterWriter deserialises each Avro branch to the correct sealed subtype
- [x] #7 AvroPlugin.isNestedRecord and allNestedTypes traverse sealed union branch types
- [x] #8 Unit tests cover scalar unions, record unions, enum unions, nullable multi-branch unions, and the original assetTopologyCondition fixture
- [x] #9 Developer guide updated with AVSC union field semantics and the generated sealed interface pattern
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extend AvscEventWriter: detect multi-branch unions; generate a sealed interface + one record wrapper per branch; wire the field type to the sealed interface; recurse into record/enum/array branches via collectNamedTypes.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2. Update AvroPlugin: isNestedRecord must recognise sealed interfaces generated from unions; allNestedTypes must walk the permittedSubtypes of those sealed interfaces so schema factories and converters are generated for every branch record.

3. Update EventSchemaFactoryWriter: when generating a field whose TypeManifest is a sealed interface (union origin), emit Schema.createUnion(List.of(...)) containing each branch's schema, resolved recursively via the existing maybeNested/createSchema dispatch.

4. Update EventToGenericRecordConverterWriter: for a sealed union field emit a switch expression over the permitted subtypes; each branch unwraps the wrapper record and writes the inner value. 5. Update GenericRecordToEventConverterWriter: inspect the raw Avro value (GenericRecord, String, Number, Boolean) and construct the matching sealed wrapper subtype.

6. Add test fixtures: (a) scalar union [double,string], (b) record union [RecordA,RecordB], (c) nullable multi-branch [null,RecordA,RecordB], (d) enum union, (e) array-item union. 7. Add test cases in AvscPluginTest covering each fixture. 8. Update developer-guide.md AVSC section with union semantics.
<!-- SECTION:NOTES:END -->
