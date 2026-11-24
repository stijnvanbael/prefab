---
id: TASK-365
title: '@Multicast event handler fails when the query finds no aggregates'
status: Done
assignee: []
created_date: '2026-11-23T07:10:00Z'
updated_date: '2026-11-24T14:30:09Z'
labels:
  - events
dependencies: []
modified_files:
  - core/src/main/java/be/appify/prefab/core/annotations/Multicast.java
  - >-
    annotation-processor/src/main/java/be/appify/prefab/processor/event/handler/multicast/MulticastEventHandlerManifest.java
  - >-
    annotation-processor/src/main/java/be/appify/prefab/processor/event/handler/multicast/MulticastEventHandlerPlugin.java
  - >-
    annotation-processor/src/main/java/be/appify/prefab/processor/event/handler/multicast/MulticastEventHandlerWriter.java
  - >-
    annotation-processor/src/test/java/be/appify/prefab/processor/event/handler/EventHandlerWriterTest.java
  - >-
    annotation-processor/src/test/resources/event/handler/multicast/source/Channel.java
  - >-
    examples/showcase/src/main/java/be/appify/prefab/example/showcase/ordering/Order.java
  - >-
    examples/showcase/src/test/java/be/appify/prefab/example/showcase/ordering/OrderCancellationOnDeactivationIntegrationTest.java
  - >-
    examples/showcase/src/test/java/be/appify/prefab/example/showcase/support/Shop.java
  - backlog/docs/annotation-reference.md
  - backlog/docs/feature-guides.md
  - backlog/docs/recipes/event-handling-multicast.md
  - backlog/docs/agent-cheat-sheet.md
  - benchmark/tasks/events-multicast/prompt.md
priority: medium
type: bug
ordinal: 199000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A `@Multicast` handler without a static companion method throws `IllegalStateException("No aggregates found for
event: ...")` when its query method returns no aggregates (`MulticastEventHandlerWriter`). The consumer retries and
then dead-letters the message.

That is the right outcome when the target should exist but is created by another event that has not been processed
yet. It is wrong when zero targets is a normal situation, e.g. "when a customer is deactivated, cancel all their
orders": a customer without orders dead-letters `CustomerDeactivated`.

Found while writing the ordering slice of the showcase (TASK-337.04). The showcase avoids it: its multicasts only
target aggregates that always exist (stock levels, order histories), and the deactivation test only deactivates
customers that have orders.

Options: an opt-in attribute on `@Multicast` (e.g. `failWhenEmpty = false`) that makes an empty result a no-op,
documented in the developer guide next to the static-companion behaviour.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A multicast handler can opt in to treating an empty query result as "nothing to do"
- [x] #2 The default behaviour (fail, retry, dead-letter) is unchanged and documented
- [x] #3 Annotation processor test and an integration test cover both behaviours
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Inspect the @Multicast annotation, processor writer, existing processor tests, and event-handler integration tests.
2. Add an opt-in failWhenEmpty attribute with the default preserving current behavior.
3. Generate a no-op for empty target results only when opted in; add processor and integration coverage for opt-in and default behavior.
4. Update developer guidance and run focused Maven tests.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
The generated multicast writer currently throws for every empty query when there is no static companion. Preserve this as the annotation default and add `failWhenEmpty = false` to generate an early return; static-companion create behavior remains unchanged by default. Coverage will assert both generated paths and exercise the showcase opt-out against an empty real repository query.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added `@Multicast.failWhenEmpty` (default `true`) so empty queries retain retry/dead-letter behavior, while opting out makes a no-companion handler return without saving. Static companion creation remains unchanged. Added processor and integration coverage, updated multicast guidance and benchmark prompt. Targeted processor, showcase integration, docs, and benchmark validation passed.
<!-- SECTION:FINAL_SUMMARY:END -->
