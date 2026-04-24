---
id: TASK-112
title: Extract abstract base class for REST operation plugins
status: To Do
assignee: []
created_date: '2026-04-10 05:00'
updated_date: '2026-04-24 06:57'
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
- [ ] #1 A non-public abstract base class (e.g., RestOperationPlugin) is introduced in the annotation-processor module that captures the shared lifecycle and context-holding logic for REST plugins
- [ ] #2 CreatePlugin, UpdatePlugin, DeletePlugin, GetByIdPlugin, and GetListPlugin all extend this base class and no longer duplicate the context-storage or writer-initialization boilerplate
- [ ] #3 The PrefabPlugin interface remains unchanged so that non-REST plugins are unaffected
- [ ] #4 All existing annotation-processor tests continue to pass after the refactoring
<!-- AC:END -->
