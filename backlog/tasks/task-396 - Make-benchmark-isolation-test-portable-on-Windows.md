---
id: TASK-396
title: Make benchmark isolation test portable on Windows
status: To Do
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

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The isolation command test passes on both Windows and Linux while asserting the correct platform-specific path arguments.
- [ ] #2 The Linux private mount namespace contract remains covered without requiring Linux tools on Windows.
<!-- AC:END -->
