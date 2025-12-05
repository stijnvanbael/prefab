---
id: task-016
title: Multicast events
status: Done
assignee: []
created_date: '2025-10-10 13:36'
updated_date: '2025-12-12 15:38'
labels: []
dependencies: []
ordinal: 12000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Deliver events to all entities matching a custom query.

Any aggregate root method annotated with @EventHandler.Multicast will have its emitted events delivered to all entities
matching the provided query.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A method annotated with `@EventHandler.Multicast` on an aggregate root should generate an event handler.
- [x] #2 The generated event handler should be triggered when the corresponding event is published.
- [x] #3 The generated event handler should call the query method on the specified repository with parameters from the event.
- [x] #4 The generated event handler should call the event handler method on each entity returned by the query method.
- [x] #5 An error should be reported if the specified repository does not exist.
- [x] #6 An error should be reported if the specified query method does not exist on the repository.
- [x] #7 An error should be reported if the parameters of the query method do not match the properties of the event.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1.  **Create a new package** `annotation-processor/src/main/java/be/appify/prefab/processor/eventhandler/multicast`.
2.  **Create `MulticastEventHandlerPlugin.java`** in the new package. This class will implement `PrefabPlugin` and will be responsible for finding methods annotated with `@EventHandler.Multicast`. It will be based on `BroadcastEventHandlerPlugin.java`.
3.  **Create `MulticastEventHandlerManifest.java`** in the new package. This class will represent the data needed for code generation, including the event, the target entity, the repository, and the query method to call. It will be based on `BroadcastEventHandlerManifest.java`.
4.  **Create `MulticastEventHandlerWriter.java`** in the new package. This class will be responsible for generating the Java code for the event handler. It will generate a method annotated with `@EventListener` that:
    *   Takes the event as a parameter.
    *   Calls the repository's `queryMethod` with parameters mapped from the event.
    *   Iterates over the returned entities and calls the event handler method on each entity.
5.  **Update `META-INF/services/be.appify.prefab.processor.PrefabPlugin`** to include the new `be.appify.prefab.processor.eventhandler.multicast.MulticastEventHandlerPlugin`.
6.  **Write tests** for the new functionality. This will likely involve:
    *   Creating a test annotation processor setup.
    *   Defining a test case with an entity, a repository with a custom query method, and a method annotated with `@EventHandler.Multicast`.
    *   Asserting that the generated event handler code is correct.
<!-- SECTION:PLAN:END -->
