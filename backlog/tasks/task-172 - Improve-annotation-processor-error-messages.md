---
id: TASK-172
title: Improve annotation processor error messages
status: Done
assignee: []
created_date: '2026-05-08 16:37'
updated_date: '2026-10-03 12:00'
labels: []
dependencies: []
priority: medium
ordinal: 153000
---

## Implementation Notes

### Completed Work
✅ Created ErrorReporter utility class in `be.appify.prefab.processor.error` package
  - Provides structured error reporting with annotation, element name, rule, and corrective action
  - Includes ErrorCollector helper for batch error collection
  - All messages route through Messager.printMessage() with proper element attachment

✅ Updated validation error messages across all validators:
  - GenerateAnnotationValidator: @Generate plugin, OutputTarget, and plugin class validation
  - PrefabProcessor: @Computed method and @Security validation
  - ClassManifest: @Parent, abstract class, @Id, and constructor validation
  - TenantPlugin: @TenantId uniqueness, nullability, and parameter validation
  - DeletePlugin: @Delete placement and method parameter validation
  - CreatePlugin: @AsyncCommit return type and duplicate mapping validation
  - StreamServiceWriter: @Streaming terminal field validation
  - PartitioningKeySupport: @PartitioningKey parameter and return type validation
  - ConsumerWriterSupport: @EventHandler target type and hierarchy conflict validation

✅ Standardized error message format:
  - Format: `[@Annotation] ElementName: Rule violated. Suggested fix: corrective action`
  - All errors include actionable suggestions for fixing the issue
  - Element attachment ensures IDE integration with error highlighting

✅ Updated test expectations to match new error message format

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Improve the quality of compile-time error messages emitted by the annotation processor. Every validation failure should pinpoint the offending element, state the rule violated, and suggest a corrective action — similar to how Lombok or MapStruct report errors.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 All annotation processor validation errors use processingEnv.getMessager().printMessage(ERROR, ..., element) with the offending element attached
- [x] #2 Error messages include the annotation, the element name, the rule violated, and a corrective action
- [x] #3 At least one test per validation rule asserts the exact error message text
- [x] #4 No generic 'Annotation processor threw an unchecked exception' errors remain for known invalid inputs
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->
