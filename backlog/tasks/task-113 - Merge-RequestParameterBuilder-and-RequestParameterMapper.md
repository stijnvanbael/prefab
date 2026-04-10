---
id: TASK-113
title: Merge RequestParameterBuilder and RequestParameterMapper into a single class
status: To Do
assignee: []
created_date: '2026-04-10 05:00'
labels:
  - "\U0001F527refactor"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`RequestParameterBuilder` and `RequestParameterMapper` are two separate classes that always appear together:

- Both are constructed with the same `List<PrefabPlugin>` argument.
- Both are always stored together in `PrefabContext` (as `requestParameterBuilder` and `requestParameterMapper`).
- Both are accessed together throughout the code generation writers (e.g., `CreateServiceWriter` calls `context.requestParameterBuilder()` and `context.requestParameterMapper()` in the same method).

Keeping them as two separate classes adds conceptual overhead without any architectural benefit. They can be merged into a single `RequestParameterHandler` (or similar name) class that exposes both `buildBodyParameter()`, `buildMethodParameter()`, and `mapRequestParameter()` methods.

The `PrefabContext` would then expose a single accessor instead of two, simplifying call sites throughout the codebase.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 RequestParameterBuilder and RequestParameterMapper are merged into a single class (e.g., RequestParameterHandler) that exposes all three methods: buildBodyParameter(), buildMethodParameter(), and mapRequestParameter()
- [ ] #2 PrefabContext exposes a single accessor for the merged class and the two separate accessors (requestParameterBuilder() and requestParameterMapper()) are removed
- [ ] #3 All call sites throughout the annotation-processor module are updated to use the new single accessor
- [ ] #4 All existing annotation-processor tests continue to pass after the refactoring
<!-- AC:END -->
