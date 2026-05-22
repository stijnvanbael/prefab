---
id: TASK-195
title: Bake safe event-scope defaults into plugin APIs
status: Done
assignee: []
created_date: '2026-05-11 17:27'
updated_date: '2026-05-21 06:21'
labels:
  - prefab
  - plugins
  - api
  - codegen
  - events
dependencies: []
references:
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/annotation-processor/src/main/java/be/appify/prefab/processor/PrefabContext.java
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/annotation-processor/src/main/java/be/appify/prefab/processor/PrefabPlugin.java
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/annotation-processor/src/main/java/be/appify/prefab/processor/assertion/AssertionPlugin.java
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/backlog/tasks/task-193 -
    Investigate-duplicate-test-code-generation-when-consuming-events-main-tests-JAR.md
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/backlog/tasks/task-151 -
    No-infrastructure-generated-for-events-imported-from-dependencies.md
documentation:
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/backlog/docs/generated-artefacts.md
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/backlog/docs/developer-guide.md
priority: high
ordinal: 31200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`TASK-193` fixed duplicate assertion generation by switching one plugin from `PrefabContext.eventElements()` to `eventElementsFromCurrentCompilation()`, but the API still makes it easy for plugins to choose the wrong scope accidentally. Introduce clearer framework-level defaults or helper APIs so plugin authors naturally get the correct event set for local code generation, while explicitly opting into classpath-inclusive discovery only when needed.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Introduce a framework-level API or naming convention that clearly distinguishes current-compilation events from classpath-inclusive consumed events.
- [x] #2 Update existing plugin call sites to use the new API consistently.
- [x] #3 Preserve intended classpath-inclusive behavior for plugins that generate infrastructure for consumed dependency events.
- [x] #4 Prevent local test-support/codegen plugins from accidentally regenerating artefacts for dependency events by default.
- [x] #5 Add regression tests that prove the safe default and the explicit opt-in behavior.
- [x] #6 Update relevant developer documentation to explain which event scope plugin authors should use and when.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Started follow-up design: inspect all `eventElements()` call sites and introduce clearer `PrefabContext` event-scope APIs so plugins default to local/current-compilation events and opt into consumed/classpath events explicitly.

Introduced `PrefabContext.EventScope` plus a safe default: `eventElements()` now means current-compilation events only, while `eventElementsIncludingConsumedDependencies()` is the explicit opt-in for plugins that need dependency events referenced by local handlers.

Added `PrefabPlugin.additionalFileEventScope()` so event-triggered `writeAdditionalFiles(...)` reruns are baked into the plugin contract. The default is local/current-compilation; Kafka, Pub/Sub, SNS/SQS, and AsyncAPI documentation explicitly opt into consumed dependency events.

Updated built-in plugins to use the new API consistently: local codegen/test-support plugins (`AssertionPlugin`, `MotherPlugin`, `AvroPlugin`) now rely on the safe default, while infrastructure/documentation paths use the explicit consumed-dependency scope.

Added Kafka regressions proving imported dependency events and imported AVSC dependency events still generate publishers/registrars, while the existing assertion regressions continue to prove dependency test assertions are not regenerated.

Validated with full affected suites: `mvn -pl annotation-processor,avro-processor,kafka,pubsub,sns-sqs -am test` passed.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Baked event-scope behavior into the plugin API by making `PrefabContext.eventElements()` local/current-compilation only, adding `eventElementsIncludingConsumedDependencies()` for explicit opt-in, and introducing `PrefabPlugin.additionalFileEventScope()` so `writeAdditionalFiles(...)` reruns respect the right scope automatically. Updated built-in plugins to use the correct scope and added regressions covering both the safe default and explicit dependency-event infrastructure generation.
<!-- SECTION:FINAL_SUMMARY:END -->
