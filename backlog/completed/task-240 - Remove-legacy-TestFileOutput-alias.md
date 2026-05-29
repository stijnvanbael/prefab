---
id: TASK-240
title: Remove legacy TestFileOutput alias
status: Done
assignee: []
created_date: '2026-05-28 14:45'
updated_date: '2026-05-28 14:48'
labels:
  - refactor
  - codegen
  - cleanup
dependencies:
  - TASK-239
references:
  - >-
    annotation-processor/src/main/java/be/appify/prefab/processor/TestFileOutput.java
  - >-
    annotation-processor/src/main/java/be/appify/prefab/processor/FileOutput.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Delete the deprecated `TestFileOutput` interface and migrate all remaining code in the annotation processor to the neutral `FileOutput` abstraction without changing output-target behavior.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `TestFileOutput` is removed from the codebase.
- [x] #2 All annotation-processor sources compile against `FileOutput` directly.
- [x] #3 Targeted output-routing and mother-plugin regressions pass after the refactor.
- [x] #4 The full `annotation-processor` module test suite passes after the cleanup.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Removed the legacy `TestFileOutput` compatibility alias entirely and migrated the remaining annotation-processor code to `FileOutput`.

Scope:
- Replaced `TestFileOutput` field/parameter/import usage in production writers and plugin helpers with `FileOutput`.
- Updated test doubles in `RestWriterTest`, `AutocompletePluginTest`, and `MultipleAsyncCreateTestClientTest` to implement `FileOutput` directly.
- Deleted `annotation-processor/src/main/java/be/appify/prefab/processor/TestFileOutput.java`.

Notes:
- Several touched source files were normalized from CRLF to LF while being edited, which aligns with repository guidance in `AGENTS.md`.
- No behavioral routing changes were introduced beyond removing the obsolete alias type.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Deleted the deprecated `TestFileOutput` interface and migrated all remaining production/test code to `FileOutput`.

Validated with targeted writer/routing tests plus the full `mvn -pl annotation-processor -am -Dsurefire.failIfNoSpecifiedTests=false test` suite; no surefire failures were recorded.
<!-- SECTION:FINAL_SUMMARY:END -->
