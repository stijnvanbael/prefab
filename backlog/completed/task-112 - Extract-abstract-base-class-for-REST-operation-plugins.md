---
id: TASK-112
title: Extract abstract base class for REST operation plugins
status: Done
assignee:
  - '@copilot'
created_date: '2026-04-10 05:00'
updated_date: '2026-10-03 08:22'
labels:
  - "\U0001F527refactor"
dependencies: []
ordinal: 134000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The five REST operation plugins (`CreatePlugin`, `UpdatePlugin`, `DeletePlugin`, `GetByIdPlugin`, `GetListPlugin`) all follow the same structural pattern:

- They each hold a set of writer objects (`ControllerWriter`, `ServiceWriter`, `TestClientWriter`, optionally `RepositoryWriter` and `RequestRecordWriter`).
- They each implement `writeController()`, `writeService()`, `writeTestClient()`, and optionally `writeAdditionalFiles()` with a near-identical flow: look up the annotation (or method), and if present delegate to the corresponding writer.
- They each store a reference to `PrefabContext`.

This repetition means that any cross-cutting change (e.g., adding a new hook, changing logging, changing how annotations are looked up) must be applied to every plugin independently.

Introduce an abstract base class (e.g., `RestOperationPlugin`) that captures the common lifecycle and delegates to abstract methods for the operation-specific parts. The concrete plugins would override only what is unique to their operation (annotation class, writer method signatures, etc.).

Example outline:
```java
public abstract class RestOperationPlugin implements PrefabPlugin {
    protected PrefabContext context;

    @Override
    public final void initContext(PrefabContext context) {
        this.context = context;
        initWriters(context);
    }

    protected abstract void initWriters(PrefabContext context);
}
```
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A non-public abstract base class (e.g., RestOperationPlugin) is introduced in the annotation-processor module that captures the shared lifecycle and context-holding logic for REST plugins
- [x] #2 CreatePlugin, UpdatePlugin, DeletePlugin, GetByIdPlugin, and GetListPlugin all extend this base class and no longer duplicate the context-storage or writer-initialization boilerplate
- [x] #3 The PrefabPlugin interface remains unchanged so that non-REST plugins are unaffected
- [x] #4 All existing annotation-processor tests continue to pass after the refactoring
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Inspect the five REST operation plugins and isolate the shared context/writer lifecycle.
2. Introduce a non-public abstract base class in the annotation-processor REST package that centralises context storage and writer initialisation hooks.
3. Refactor CreatePlugin, UpdatePlugin, DeletePlugin, GetByIdPlugin, and GetListPlugin to extend the base class with minimal behavioural change.
4. Run targeted annotation-processor tests, then the annotation-processor module test suite if needed.
5. Update task notes with implementation details and verification results.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Introduced a non-public `RestOperationPluginSupport` lifecycle base in the annotation-processor REST package to centralise `PrefabContext` storage and initialisation hooks for REST operation plugins.
- Added a thin public `RestOperationPlugin` bridge so sibling REST plugin subpackages can inherit the shared lifecycle without duplicating `initContext(...)` boilerplate.
- Refactored `CreatePlugin`, `UpdatePlugin`, `DeletePlugin`, `GetByIdPlugin`, and `GetListPlugin` to extend the shared REST base structure and keep their existing generation behaviour unchanged.
- Added `RestOperationPluginStructureTest` to lock in the shared inheritance structure for the five REST operation plugins.
- Verified the change with focused REST tests and the full `annotation-processor` Maven test suite.
<!-- SECTION:NOTES:END -->
