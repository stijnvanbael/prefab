---
id: decision-013
title: Owner-indexed aggregated outputs for incremental builds
date: '2026-11-26'
status: accepted
related_tasks:
  - TASK-310
---

## Context

IntelliJ IDEA's native build (JPS) gives javac only the changed sources of a module and first deletes every output
those sources own, including shared resources such as `db/migration/V1__generated.sql`. Prefab built several outputs
from all inputs it saw in one compilation: the Flyway migration and aggregate manifest, the AsyncAPI document, the
`Assertions` factories and repository mixin bindings. A partial compilation therefore produced a migration with 6 of
12 aggregates, an AsyncAPI document without unrelated channels, and a repository without its mixin. Generated test
helpers are written outside the Filer, so JPS never removed helpers of deleted sources.

Alternatives considered: requiring Maven delegation or a full rebuild (rejected by the task: no compiler-setting
workarounds), reading the previous output back from the class path (JPS may already have deleted it), and an
IntelliJ plugin (extra distribution and maintenance for one IDE).

## Decision

1. **Record inputs per owning source, not rendered output.** Each aggregating writer adds its inputs to a
   `SourceContributions<T>` keyed by the top-level type of the contributing source. Inputs are small JSON records
   (desired tables, event identities, assertion entries, mixin bindings), stored under
   `target/prefab/incremental-v1/{main,test}/`.
2. **Merge on every compilation.** Entries of sources javac received are replaced, entries whose source file no
   longer exists are dropped, all others are kept. The writer renders its output from the merged inputs in
   `PrefabPlugin.finishProcessing()`, after the last round.
3. **Own generated test sources.** `target/prefab/test-sources-{main,test}.json` maps each file in
   `target/prefab-test-sources/` to its owning sources. A file whose owners are all gone is deleted; unchanged
   content is not rewritten, so its timestamp stays stable.
4. **Persist only on success.** Indexes are written after a final round without errors and without failed plugins,
   so a broken build never records partial inputs.
5. **Only for standard layouts.** State is kept only when sources live under `src/<set>/java/`. In-memory and other
   layouts behave as before: generate from the current compilation.

## Consequences

- Partial recompiles produce the same aggregated output as a clean Maven build; `IncrementalCompilationTest` guards
  this with consecutive javac invocations.
- A schema change of a stored input record needs a new `incremental-vN` directory so stale metadata is ignored.
- JPS still decides which sources javac sees: changes to the set of generated file names need a recompile of the
  module's production sources, and an `.avsc`-only edit needs a recompile of its `@Avsc` contract. This is documented
  in the troubleshooting guide rather than worked around.
