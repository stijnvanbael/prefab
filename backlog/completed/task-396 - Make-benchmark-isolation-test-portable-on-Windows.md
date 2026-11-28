---
id: TASK-396
title: Make benchmark isolation test portable on Windows
status: Done
assignee: []
created_date: '2026-11-26 12:00'
labels:
  - testing
  - benchmark
dependencies: []
priority: low
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
During the TASK-310 post-rebase full reactor test on Windows,
`IsolationTest.wrapsTheCommandInAPrivateMountNamespace` failed because its expected command
contains Unix path strings (`/work/run/task/project`, `/tmp/stage`, `/repo`, `/work`).
`Path.of(...)` resolves those inputs to Windows path strings (`C:\work\run\task\project`,
`C:\tmp\stage`, `\repo`, `\work`) before `Isolation.wrap` builds the command.
The test assumes Unix path rendering even though it runs on Windows; the production
namespace command is intended for a Linux runner. Keep this test failure separate
from the IntelliJ/JPS incremental-processing changes.
<!-- SECTION:DESCRIPTION:END -->

## Analysis

The mount-namespace test only inspects the generated argument list, so it can cover the Linux command contract without running Linux utilities. Use JUnit's temporary directory for all paths and compare against normalized `Path` strings, which preserves platform-specific Windows and Linux spellings. Replace the hard-coded `/tmp` file creation with a temporary directory supplied by JUnit and verify linked spellings only where the platform's resolved temp path actually uses a supported macOS alias.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The isolation command test passes on both Windows and Linux while asserting the correct platform-specific path arguments.
- [x] #2 The Linux private mount namespace contract remains covered without requiring Linux tools on Windows.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- `IsolationTest.wrapsTheCommandInAPrivateMountNamespace` now builds all fixture paths under JUnit's `@TempDir` and compares the generated arguments with normalized platform-native path strings. It still asserts the `unshare --mount --propagation private` wrapper and never executes Linux utilities.
- The linked-system-folder test now creates its fixture under the class's JUnit temp root and checks both spellings only when the resolved temp path uses a supported macOS `/private` alias.
- Verification on Windows: `mvn -pl benchmark -am '-Dtest=IsolationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` passed (7 tests), and `mvn -pl benchmark -am test` passed (70 tests; 2 environment-conditional skips). Linux CI can execute the same path-native assertions without changes.
<!-- SECTION:NOTES:END -->
