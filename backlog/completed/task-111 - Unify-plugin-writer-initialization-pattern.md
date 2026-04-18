---
id: TASK-111
title: Unify plugin writer initialization pattern
status: Done
assignee: []
created_date: '2026-04-10 05:00'
updated_date: '2026-04-17 07:03'
labels:
  - "\U0001F527refactor"
dependencies: []
ordinal: 20000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The REST operation plugins currently use two different patterns for initializing their writer objects:

1. **Field-declaration initialization** (e.g., `UpdatePlugin`, `DeletePlugin`, `GetByIdPlugin`, `GetListPlugin`, `BinaryPlugin`) – writer instances are created directly as field initializers:
   ```java
   private final UpdateControllerWriter updateControllerWriter = new UpdateControllerWriter();
   ```

2. **`initContext()` initialization** (e.g., `CreatePlugin`, `MulticastEventHandlerPlugin`) – writer instances are created inside `initContext()` because they require a `PrefabContext` argument:
   ```java
   @Override
   public void initContext(PrefabContext context) {
       this.context = context;
       controllerWriter = new CreateControllerWriter(context);
   }
   ```

This inconsistency makes the codebase harder to navigate because readers must check both places to understand dependencies. The split also means some writers implicitly depend on `context` being set before they are usable, while others are fully initialised at construction time.

The preferred approach is to **pass `PrefabContext` through the plugin constructor** (or, if the plugin is service-loaded with a no-arg constructor, accept it via `initContext()` and initialise writers there), so that all writers are created consistently in one place.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 All REST operation plugins (CreatePlugin, UpdatePlugin, DeletePlugin, GetByIdPlugin, GetListPlugin, BinaryPlugin) follow the same pattern for initialising their writer objects
- [ ] #2 Writer classes that require PrefabContext accept it at construction time rather than having it stored as a mutable field set after construction
- [ ] #3 No plugin stores an uninitialized writer field that can only be used after initContext() has been called
- [ ] #4 Existing unit and integration tests continue to pass after the refactoring
<!-- AC:END -->
