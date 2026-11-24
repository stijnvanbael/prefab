---
id: TASK-379
title: Measure and shorten the edit-compile-test loop
status: To Do
assignee: []
created_date: '2026-11-24 10:00'
labels:
  - dx
  - performance
  - productivity
dependencies:
  - TASK-175
priority: medium
type: task
ordinal: 205000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Agents work in a loop of edit, `mvn verify`, read the output. In the benchmark, Haiku with docs needs a median of 3-4
Maven builds per task and 6 without docs; Sonnet needs 2 (TASK-343). Each build pays for Maven startup, annotation
processing, compiling generated sources, Spring context start per distinct test configuration, and the tests. The
in-memory backend (TASK-175) removes container start-up, but nobody has measured where the rest of the time goes.
Seconds saved per build multiply over every turn of every agent and every developer.

Candidates to evaluate once measured:
- Spring test-context reuse: generated test support and `@IntegrationTest` produce identical context configurations
  so the context cache hits across test classes (count contexts started per run).
- JUnit 5 parallel execution, now possible because in-memory tests share no containers.
- Annotation processor time on a realistic model (TASK-168 was the last profiling pass).
- Lazy initialization or Spring AOT/CDS for test contexts.
- Documented build tooling: Maven daemon (`mvnd`), Maven build cache extension, `-o`, surefire `-Dtest=` filters, and
  a quick "compile and check only" command an agent can run before the full build.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A reproducible measurement of the benchmark template and the showcase records the time split (Maven startup, processing, compilation, context starts, tests) and the number of Spring contexts started, before and after
- [ ] #2 Changes that cut the median `mvn verify` time of the showcase in memory by a recorded percentage are implemented, each with its measured gain; candidates that did not help are recorded with numbers
- [ ] #3 A context-cache regression check fails the build when a change makes test classes start extra Spring contexts in the showcase
- [ ] #4 The fastest recommended commands are documented in `getting-started.md` and the agent cheat sheet (TASK-344)
- [ ] #5 Benchmark agent time and Maven builds are compared before and after (TASK-343)
<!-- AC:END -->
