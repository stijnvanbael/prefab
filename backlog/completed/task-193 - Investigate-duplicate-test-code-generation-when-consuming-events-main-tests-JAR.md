---
id: TASK-193
title: >-
  Investigate duplicate test-code generation when consuming events main + tests
  JAR
status: Done
assignee: []
created_date: '2026-05-11 16:58'
updated_date: '2026-05-21 06:21'
labels:
  - prefab
  - codegen
  - avsc
  - tests
  - maven
dependencies: []
references:
  - /Users/stijnvanbael/IdeaProjects/appify/prefab/avro-processor
  - /Users/stijnvanbael/IdeaProjects/appify/prefab/annotation-processor
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/backlog/docs/generated-artefacts.md
documentation:
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/backlog/docs/developer-guide.md
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/backlog/docs/generated-artefacts.md
priority: high
ordinal: 32200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When an application module depends on an events module that already publishes generated test artefacts (from AVSC) in its test JAR, Prefab regenerates the same test code in the application module. Investigate why code generation does not recognize or reuse upstream generated test artefacts and define a fix that prevents redundant generation while preserving existing workflows.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Reproduce the issue in a minimal two-module Maven setup (events + application) where application imports events main JAR and test JAR.
- [x] #2 Document the exact generation path that causes duplicate generation (processor/module/phase and triggering inputs).
- [x] #3 Identify whether the root cause is classpath scanning, source set handling, artifact classification, or generation guard logic.
- [x] #4 Implement a fix so application module does not regenerate test artefacts already supplied by dependencies.
- [x] #5 Add or update automated tests that fail before the fix and pass after the fix.
- [x] #6 Verify that existing scenarios that rely on local generation in modules without upstream test artefacts continue to work.
- [x] #7 Update relevant developer documentation in backlog/docs to describe expected behavior for cross-module generated test artefacts.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Started root-cause analysis: inspect avro-processor and annotation-processor generation flow, then reproduce with existing multi-module fixtures or examples.

RCA finding: `AssertionPlugin.writeAdditionalFiles(...)` iterates `context.eventElements()` (all events incl classpath-derived), unlike `MotherPlugin`/`AvroPlugin` which use `eventElementsFromCurrentCompilation()`. This makes consumer modules regenerate event assertion classes for dependency events referenced by local `@EventHandler` methods.

`PrefabContext.eventElements()` explicitly merges `eventElementsFromClasspath()` (derived from `@EventHandler` parameter types), so dependency events can enter assertion generation even without local event declarations.

Likely impact scope: duplicate generated test assertions (`*Assert`, package `Assertions`) in consumer module `target/prefab-test-sources`, especially when dependency also publishes generated test artefacts via test JAR. Next step: add failing regression in `AssertionPluginTest` mirroring existing dependency-consumer tests in `MotherPluginTest` and `AvscPluginTest`.

Implemented fix: `AssertionPlugin` now iterates `eventElementsFromCurrentCompilation()` instead of all `eventElements()`, preventing consumer modules from regenerating assertion test sources for dependency events discovered through local `@EventHandler` signatures.

Added regression coverage in `annotation-processor` for plain dependency events and in `avro-processor` for AVSC-generated dependency event classes consumed from another module.

Validated with targeted regressions and full module suites: `mvn -pl annotation-processor,avro-processor -am test` passed after the change.

Updated `backlog/docs/generated-artefacts.md` to document that event assertion classes are generated only for events in the current compilation and not regenerated for dependency-provided events.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Scoped event assertion generation to the current compilation so consumer modules no longer regenerate `*Assert` and package `Assertions` classes for dependency events discovered via `@EventHandler` parameters. Added regression tests for both plain event dependencies and AVSC-generated event dependencies, and updated generated artefact documentation to describe the cross-module behavior.
<!-- SECTION:FINAL_SUMMARY:END -->
