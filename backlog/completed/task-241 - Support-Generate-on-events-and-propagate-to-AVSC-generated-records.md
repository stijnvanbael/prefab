---
id: TASK-241
title: Support @Generate on events and propagate to AVSC-generated records
status: Done
assignee: []
created_date: '2026-05-28 15:16'
updated_date: '2026-05-28 15:21'
labels: []
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Currently, @Generate is only validated on @Aggregate-annotated types. This task adds @Event support so that developers can place @Generate overrides on event contract interfaces (annotated with @Event/@Avsc), and the AVSC code generator propagates those annotations onto the generated Java records.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 GenerateAnnotationValidator accepts @Event-annotated types without warning
- [x] #2 @Generate annotations placed on an @Event/@Avsc interface are propagated to the top-level generated record
- [x] #3 Only the top-level generated record inherits @Generate; nested records/enums do not
- [x] #4 Existing tests continue to pass
- [x] #5 New integration test verifies @Generate is present on the generated record when the contract interface declares it
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented @Generate support on event contract interfaces and propagation to AVSC-generated records.

**Changes:**
- `GenerateAnnotationValidator`: Updated to accept `@Event`-annotated types alongside `@Aggregate` types. Updated the warning message and Javadoc accordingly.
- `AvscPlugin`: Added `buildGenerateAnnotationSpecs()` to extract `@Generate` annotations from a `TypeElement` and `toAnnotationSpec()` to convert them to JavaPoet `AnnotationSpec` objects (handling `MirroredTypeException` for the plugin `Class<?>` reference). These specs are passed to `AvscEventWriter.writeAll()`.
- `AvscEventWriter`: Updated `writeAll()` signature to accept `List<AnnotationSpec> generateAnnotations`. The `buildTopLevelRecord()` method applies them to the generated top-level record. Nested records, enums, and union types are unaffected.
- Test fixtures: Added `GenerateAnnotatedAvsc.java` and `GenerateAnnotatedAvscEvent.avsc` under `event/avsc/generate/source/`.
- Integration test: `generateAnnotationOnEventContractIsPropagatedToGeneratedRecord` verifies `@Generate(plugin = AvscPlugin.class, enabled = false)` appears on the generated record.

All 395 tests pass (333 annotation-processor + 62 avro-processor).
<!-- SECTION:FINAL_SUMMARY:END -->
