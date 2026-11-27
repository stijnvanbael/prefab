---
id: TASK-406
title: Complete build phase split and post-change benchmark for TASK-384
status: To Do
assignee: []
created_date: '2026-11-27T10:00:00Z'
labels:
  - dx
  - performance
  - benchmark
dependencies:
  - TASK-384
priority: low
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-384 shipped the build-loop improvements (`-T 2`, context-cache regression check, documented commands) but left
two measurements open; see "Prefab edit/compile/test measurements" in `backlog/docs/benchmark.md`.

1. Split the `mvn verify` time into annotation processing, compilation, context start and tests (for example with
   `-Dmaven.compiler.showWarnings`, javac `-Xlint`/`-XDverboseCompilePolicy`, or a profiler), on an unloaded host.
2. Rerun the TASK-343 Haiku 5.5 `cheat-sheet` benchmark and compare median Maven builds (baseline 4) and agent time
   (baseline 1m19s) with the post-change guidance.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The time split per build phase is recorded in `benchmark.md` for the showcase and the benchmark template
- [ ] #2 A post-change benchmark run is recorded and compared with the TASK-343 baseline
<!-- AC:END -->
