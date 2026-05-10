---
id: task-172
title: Improve annotation processor error messages
status: To Do
assignee: []
created_date: '2026-05-08 16:37'
labels: []
dependencies: []
priority: medium
---

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
