---
id: TASK-323
title: Provide actionable compiler diagnostics for processor failure paths
status: To Do
assignee: []
created_date: '2026-11-16 15:43'
labels: [dx, annotation-processor, bug]
dependencies: []
priority: high
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Audit exception-producing paths in all Prefab processors and plugins so expected invalid inputs and generation failures yield actionable compiler diagnostics instead of uncaught processor stack traces. Preserve unexpected internal failures as explicit failures with diagnostic context, not broad swallowed exceptions or successful fallback output.

Related: completed TASK-007 (IDE errors), completed TASK-172 (validation messages) and TASK-307 (wildcard crash). Extend existing `ErrorReporter`; do not recreate the already-completed validation-message story.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Inventory explicit throws, parsing/conversion errors, file/resource failures and plugin boundaries across processor modules; classify expected input, infrastructure, programming and fatal JVM failures.
- [ ] #2 Expected failures emit one useful ERROR diagnostic with source element/annotation where available, cause context and corrective action; suppress only consequent duplicate diagnostics, not independent errors.
- [ ] #3 Unexpected programming/plugin failures fail compilation explicitly with processor/plugin identity and preserved cause for debugging; do not catch fatal JVM errors or silently continue emitting invalid artifacts.
- [ ] #4 Add representative negative compilation tests per failure category, including unsupported types, invalid metadata, resources and generation I/O; no success-shaped fallback or raw unchecked escape remains for known invalid inputs.
- [ ] #5 Document diagnostic conventions and plugin-author obligations, with a reproducible way to obtain detailed internal failure evidence.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Build a failure-path inventory, reuse the standard reporter at the closest context-bearing boundary, add negative fixtures and review remaining uncaught paths by classification.
<!-- SECTION:PLAN:END -->
