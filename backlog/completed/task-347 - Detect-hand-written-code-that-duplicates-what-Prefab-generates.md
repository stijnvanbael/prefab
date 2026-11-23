---
id: TASK-347
title: Detect hand-written code that duplicates what Prefab generates
status: To Do
assignee: []
created_date: '2026-11-20 20:14'
labels:
  - agents
  - dx
  - annotation-processor
dependencies: []
priority: medium
type: feature
ordinal: 192000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The most common way AI models (and new users) misuse Prefab is to ignore it: they write a @RestController, a JPA @Entity, a Spring Data repository or a migration script by hand for an aggregate that Prefab already covers. The result often compiles and may even pass simple tests, so nothing tells the model it went wrong. The app ends up with two competing implementations, conflicting endpoints or drifting schemas.

Documentation alone (TASK-344) reduces this but cannot rule it out. A fast, automatic check that flags these patterns gives agents a clear signal to correct course, and benefits human developers too.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Hand-written controllers that map paths already exposed for a Prefab aggregate are reported, naming the aggregate and the clashing endpoint
- [ ] #2 JPA entities or hand-written repositories for Prefab aggregates are reported
- [ ] #3 Hand-written database migrations that alter tables Prefab manages are reported, unless they use a documented escape hatch
- [ ] #4 Each finding explains what Prefab already provides and the recommended alternative or extension point, in the same 'Suggested fix' style as existing processor diagnostics
- [ ] #5 Legitimate customisation through documented extension points (repository mixins, plugins, provider overrides, the source-file escape hatch) is not reported
- [ ] #6 The checks run as part of a normal build or test run, can be turned off per project, and are covered by positive and negative tests
- [ ] #7 The checks are documented in the developer guide and mentioned in the agent cheat sheet
<!-- AC:END -->
