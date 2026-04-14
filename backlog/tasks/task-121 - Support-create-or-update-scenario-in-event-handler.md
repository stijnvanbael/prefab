---
id: TASK-121
title: Support 'create or update' scenario in event handler
status: Done
assignee:
  - '@copilot'
created_date: '2026-04-14 14:34'
updated_date: '2026-04-14 14:44'
labels: []
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Often, an event will either result in a new aggregate root or update an existing one depending on whether it already exists or not. A new @CreateOrUpdate annotation should allow a static method to handle both cases: the method receives an Optional<AggregateType> (the existing aggregate, or empty if not found) and the event, and returns the resulting aggregate. The framework looks up the aggregate by a field on the event (like @ByReference) and always saves the returned aggregate.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 New @CreateOrUpdate annotation added to core module with 'property' attribute
- [x] #2 CreateOrUpdateEventHandlerPlugin discovers static methods annotated with @CreateOrUpdate
- [x] #3 Plugin validates the static method has two parameters: Optional<AggregateType> and EventType
- [x] #4 Plugin validates the static method returns AggregateType
- [x] #5 CreateOrUpdateEventHandlerWriter generates a service method that looks up the aggregate by event property, passes Optional<AggregateType> and event to the static method, and saves the result
- [x] #6 Plugin is registered in META-INF/services
- [x] #7 Test source files added for the create-or-update scenario
- [x] #8 Tests verify the generated service method looks up the aggregate, calls the static method, and saves the result
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create @CreateOrUpdate annotation in core module
2. Create CreateOrUpdateEventHandlerManifest record
3. Create CreateOrUpdateEventHandlerWriter that generates: look up aggregate by event property -> pass Optional<T> + event to static method -> save result
4. Create CreateOrUpdateEventHandlerPlugin to discover and validate annotated static methods
5. Register plugin in META-INF/services
6. Add test source files (aggregate + event)
7. Add tests in EventHandlerWriterTest
8. Run tests to verify
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented @CreateOrUpdate annotation for the create-or-update event handler scenario.

- Added `@CreateOrUpdate` annotation to `core/src/main/java/be/appify/prefab/core/annotations/CreateOrUpdate.java` with a `property` attribute
- Added `CreateOrUpdateEventHandlerManifest`, `CreateOrUpdateEventHandlerWriter`, and `CreateOrUpdateEventHandlerPlugin` under `annotation-processor/.../event/handler/createorupdate/`
- The plugin discovers public static methods annotated with `@CreateOrUpdate` that have exactly two parameters: `Optional<AggregateType>` and `EventType`
- The writer generates a service method that: (1) looks up the aggregate via `findById` using the event property, (2) passes the `Optional` and event to the static method, (3) saves the result
- Registered the plugin in `META-INF/services`
- Added test source files (`ChannelSummary.java`, `MessageSent.java`) and four new tests in `EventHandlerWriterTest`
<!-- SECTION:NOTES:END -->
