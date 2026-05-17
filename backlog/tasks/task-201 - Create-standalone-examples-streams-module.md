---
id: TASK-201
title: Create standalone examples-streams module
status: To Do
assignee: []
created_date: '2026-05-17 09:14'
updated_date: '2026-05-17 09:18'
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
- [ ] #1 A new standalone Maven module exists at `examples/streams` with its own `pom.xml` and source roots
- [ ] #2 The root build includes `examples/streams` so it is compiled and tested in normal reactor builds
- [ ] #3 `examples/streams` builds with `mvn -pl examples/streams -am test` without depending on `examples/kafka`
- [ ] #4 A minimal runnable application entrypoint exists in `examples/streams` for subsequent stories
<!-- AC:END -->
