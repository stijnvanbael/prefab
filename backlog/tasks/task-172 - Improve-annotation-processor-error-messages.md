---
id: TASK-172
title: Improve annotation processor error messages
status: In Progress
assignee: []
created_date: '2026-05-08 16:37'
updated_date: '2026-05-21 06:22'
labels: []
dependencies: []
priority: medium
ordinal: 153000
---

## Analysis

### Current State
Found 11 major validation areas with errors:
1. @Generate annotation validation (GenerateAnnotationValidator)
2. @Computed method validation
3. @Security annotation validation
4. Class structure validation (ClassManifest)
5. @Create annotation validation
6. @Delete annotation validation
7. @Streaming validation
8. @TenantId validation
9. Event handler validation
10. @PartitioningKey validation
11. Core exception handling (scattered IllegalArgumentException/IllegalStateException)

**Issues Identified:**
- Some validation uses Messager.printMessage() already (good), but many throw unchecked exceptions
- Error messages lack the offending element reference
- No consistent format (annotation name, element name, rule, corrective action)
- Missing test coverage for exact error message text
- Generic exception messages that don't guide users on fixes

### Implementation Plan
1. Create a centralized error reporting utility (ErrorReporter class)
2. Convert all thrown exceptions to use Messager with proper element attachment
3. Standardize error message format: "[Annotation] {ElementName}: {Rule violated}. Suggested fix: {corrective action}"
4. Write parametrized tests for each validation rule
5. Verify all messages route through Messager with Kind.ERROR

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Improve the quality of compile-time error messages emitted by the annotation processor. Every validation failure should pinpoint the offending element, state the rule violated, and suggest a corrective action — similar to how Lombok or MapStruct report errors.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All annotation processor validation errors use processingEnv.getMessager().printMessage(ERROR, ..., element) with the offending element attached
- [ ] #2 Error messages include the annotation, the element name, the rule violated, and a corrective action
- [ ] #3 At least one test per validation rule asserts the exact error message text
- [ ] #4 No generic 'Annotation processor threw an unchecked exception' errors remain for known invalid inputs
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
