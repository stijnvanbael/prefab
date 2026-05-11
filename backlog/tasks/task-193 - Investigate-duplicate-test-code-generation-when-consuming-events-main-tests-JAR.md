---
id: TASK-193
title: >-
  Investigate duplicate test-code generation when consuming events main + tests
  JAR
status: In Progress
assignee: []
created_date: '2026-05-11 16:58'
updated_date: '2026-05-11 17:12'
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
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When an application module depends on an events module that already publishes generated test artefacts (from AVSC) in its test JAR, Prefab regenerates the same test code in the application module. Investigate why code generation does not recognize or reuse upstream generated test artefacts and define a fix that prevents redundant generation while preserving existing workflows.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Reproduce the issue in a minimal two-module Maven setup (events + application) where application imports events main JAR and test JAR.
- [ ] #2 Document the exact generation path that causes duplicate generation (processor/module/phase and triggering inputs).
- [ ] #3 Identify whether the root cause is classpath scanning, source set handling, artifact classification, or generation guard logic.
- [ ] #4 Implement a fix so application module does not regenerate test artefacts already supplied by dependencies.
- [ ] #5 Add or update automated tests that fail before the fix and pass after the fix.
- [ ] #6 Verify that existing scenarios that rely on local generation in modules without upstream test artefacts continue to work.
- [ ] #7 Update relevant developer documentation in backlog/docs to describe expected behavior for cross-module generated test artefacts.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Started root-cause analysis: inspect avro-processor and annotation-processor generation flow, then reproduce with existing multi-module fixtures or examples.
<!-- SECTION:NOTES:END -->
