---
id: TASK-384
title: Measure and shorten the edit-compile-test loop
status: In Progress
assignee: []
created_date: '2026-11-24T10:00:00Z'
updated_date: '2026-11-25T18:37:52Z'
labels:
  - dx
  - performance
  - productivity
dependencies:
  - TASK-175
modified_files:
  - examples/showcase-inmemory/pom.xml
  - >-
    examples/showcase-inmemory/src/test/java/be/appify/prefab/example/showcase/ZZZShowcaseContextCacheTest.java
  - backlog/docs/getting-started.md
  - backlog/docs/agent-cheat-sheet.md
  - backlog/docs/benchmark.md
  - backlog/tasks/task-384 - Measure-and-shorten-the-edit-compile-test-loop.md
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
- [x] #2 Changes that cut the median `mvn verify` time of the showcase in memory by a recorded percentage are implemented, each with its measured gain; candidates that did not help are recorded with numbers
- [x] #3 A context-cache regression check fails the build when a change makes test classes start extra Spring contexts in the showcase
- [x] #4 The fastest recommended commands are documented in `getting-started.md` and the agent cheat sheet (TASK-344)
- [ ] #5 Benchmark agent time and Maven builds are compared before and after (TASK-343)
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Reproduce current showcase and benchmark-template test build timings with Maven phase profiling and Spring context-cache statistics; record the measurement procedure and baseline.
2. Evaluate targeted build improvements against repeated showcase runs, retaining only changes with a repeatable median improvement and recording non-improvements.
3. Add a showcase regression check for the expected number of Spring test contexts, then rerun the measurements and compare benchmark agent time/build count against TASK-343.
4. Document the fastest verified developer/agent commands and finalize the task with measured results and evidence.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Baseline on Temurin JDK 25 / Maven 3.9.11 using `mvn --no-transfer-progress -pl examples/showcase-inmemory -am verify`: 88.00s, 86.03s, 85.93s (median 86.03s). With reactor parallelism `-T 2`: 64.05s, 64.53s, 64.74s (median 64.53s), a 25.0% wall-clock reduction. One `-T 4` run took 63.46s; one `-o -T 2` run took 64.77s, neither showing a meaningful gain over `-T 2`.

The `-T 2` improvement is documented for multi-module builds; no global Maven setting was added because single-module builds do not benefit. The showcase cache regression test passed at the end of the one-fork alphabetical suite: 104 tests, 0 failures, 0 errors, 4 skipped, and one context load. Its observed Spring Boot context startup was 1.485s. A post-change build with cache DEBUG logging took 65.27s, but is not comparable; two further samples took 260.24s and 487.79s under high host load, so no stable post-change median is claimed.

`mvn -N validate` startup/no-op median was 0.74s. Benchmark-template validation of `rest-create-and-get` passed its 3 hidden tests in 3.75s; the unchanged starter failed as expected in 3.24s. Maven does not separately report annotation-processing and `javac` compilation time because processing runs inside the compiler invocation, so the requested phase split is incomplete. TASK-343's pre-change Haiku cheat-sheet run recorded medians of 4 Maven builds and 1m19s agent time; no post-change model run was recorded. Details and limitations are in `backlog/docs/benchmark.md`.
<!-- SECTION:NOTES:END -->
