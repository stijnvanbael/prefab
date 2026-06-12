---
id: TASK-250
title: Fix streams example compile failure in StreamTopologyConfiguration
status: To Do
assignee: []
created_date: '2026-06-12 05:24'
labels:
  - streams
  - examples
  - build
dependencies: []
references:
  - >-
    /Users/stijnvanbael/IdeaProjects/appify/prefab/examples/streams/src/main/java/be/appify/prefab/example/streams/StreamTopologyConfiguration.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The full `mvn test` run currently fails in `examples/streams` because `examples/streams/src/main/java/be/appify/prefab/example/streams/StreamTopologyConfiguration.java` has a missing return statement. This is outside TASK-249 but blocks a fully green repository build.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `examples/streams` compiles successfully during the root `mvn test` build.
- [ ] #2 `StreamTopologyConfiguration` returns a valid `StreamDefinition` from the affected bean method.
- [ ] #3 The repository-wide Maven test run completes without this compile failure.
<!-- AC:END -->
