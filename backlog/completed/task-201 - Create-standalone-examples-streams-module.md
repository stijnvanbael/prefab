---
id: TASK-201
title: Create standalone examples-streams module
status: Done
assignee: []
created_date: '2026-05-17 09:14'
updated_date: '2026-05-17 09:47'
labels:
  - feature
  - streams
  - kafka
  - examples
milestone: m-0
dependencies: []
references:
  - examples/streams
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
priority: high
ordinal: 20100
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a standalone `examples/streams` Maven module for Prefab Streams work, independent from existing `examples/kafka`, and wire it into the root build.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A new standalone Maven module exists at `examples/streams` with its own `pom.xml` and source roots
- [x] #2 The root build includes `examples/streams` so it is compiled and tested in normal reactor builds
- [x] #3 `examples/streams` builds with `mvn -pl examples/streams -am test` without depending on `examples/kafka`
- [x] #4 A minimal runnable application entrypoint exists in `examples/streams` for subsequent stories
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1) Inspect existing example modules (especially examples/kafka) and root reactor structure.
2) Create standalone examples/streams Maven module with pom, source/test roots, minimal Spring Boot entrypoint, and README.
3) Wire examples/streams into root pom modules list without coupling to examples/kafka.
4) Run mvn -pl examples/streams -am test and capture results.
5) Update backlog docs if module matrix/feature mapping should mention examples/streams; then update TASK-201 AC checks and notes.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Created a new standalone Maven module at `examples/streams` by mirroring existing example-module conventions.

Implemented:
- `examples/streams/pom.xml` with parent linkage, module-specific artifact metadata, core/provided-processor/test dependencies, and standard plugins used by other examples.
- `examples/streams/src/main/java/be/appify/prefab/example/streams/StreamsExampleApplication.java` as minimal runnable Spring Boot + `@EnablePrefab` entrypoint.
- `examples/streams/src/test/java/be/appify/prefab/example/streams/StreamsExampleApplicationTest.java` as minimal passing module test.
- `examples/streams/README.md` with quick run/test commands.
- Added `examples/streams` to root reactor modules in `pom.xml`.
- Updated `backlog/docs/modules.md` with a repository example-modules section including `examples/streams`.

Validation:
- Ran `mvn -pl examples/streams -am test` from repo root.
- Reactor built `prefab-parent`, `prefab-core`, `prefab-annotation-processor`, `prefab-test`, and `streams-example` with `BUILD SUCCESS`.
- `streams-example` test result: 1 test run, 0 failures, 0 errors, 0 skipped.
<!-- SECTION:NOTES:END -->
