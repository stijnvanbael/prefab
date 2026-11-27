---
id: TASK-370
title: Fix agent context processor test on Windows
status: Done
assignee: []
created_date: '2026-11-23 18:47'
labels:
  - annotation-processor
  - testing
dependencies: []
priority: low
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
During TASK-176, the annotation-processor test suite failed on Windows in
`AgentContextPluginTest.writesBelowTargetWhenOutputIsAFileSystemDirectory`.
`copyTo` calls `Path.of(source.toUri().getPath())`, which turns a file URI into a path like
`/C:/.../OrderEvent.java` and throws `InvalidPathException` on Windows. The failure occurs before
the test compiles its fixture; the same URI handling should work on Windows and Unix.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The agent context file-system output test passes on Windows and Unix
- [x] #2 File names are derived from file URIs without treating a URI path as a platform path
<!-- AC:END -->

## Implementation Notes
<!-- SECTION:NOTES:BEGIN -->
- Extracted the filename from the URI path using URI `/` separators instead of converting the URI path directly to a platform `Path`.
- Verified the regression on Windows: `AgentContextPluginTest` passes all 7 tests, including the filesystem-output case.
<!-- SECTION:NOTES:END -->
