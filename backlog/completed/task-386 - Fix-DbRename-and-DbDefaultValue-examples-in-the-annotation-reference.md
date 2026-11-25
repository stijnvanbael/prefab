---
id: TASK-386
title: Fix @DbRename and @DbDefaultValue examples in the annotation reference
status: Done
assignee: []
created_date: '2026-11-25 06:00'
labels:
  - documentation
  - agents
dependencies: []
references:
  - backlog/docs/annotation-reference.md
  - core/src/main/java/be/appify/prefab/core/annotations/DbRename.java
priority: high
type: bug
ordinal: 211000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`backlog/docs/annotation-reference.md` documents `@DbRename` with an `oldName` attribute
(`@DbRename(oldName = "full_name")`), but the annotation only has `value()`: the correct use is
`@DbRename("full_name")`. An agent following the reference gets a compile error ("cannot find symbol: method
oldName()"). Found while writing the TASK-377 brownfield task `brownfield-rename-contract`, whose reference uses
`@DbRename`.

The same section documents `@DbDefaultValue` as an SQL expression with string literals in quotes
(`@DbDefaultValue("'PENDING'")`), but the generator quotes string values itself: `@DbDefaultValue("'BRONZE'")` on an
enum field generates `DEFAULT '''BRONZE'''`, so existing rows get the value `'BRONZE'` including the quotes and no
longer map to the enum. `database-migrations.md` correctly uses `@DbDefaultValue("pending")`. Found with the TASK-377
task `brownfield-evolve-schema`.

Also check the cheat sheet, recipes and feature guides for the same mistakes.
<!-- SECTION:DESCRIPTION:END -->

## Analysis

The annotation reference contains both incorrect examples. `DbRename` declares only `value()`, and the column
generator quotes values for textual types, so the reference should demonstrate `@DbRename("full_name")` and an
unquoted string default with generated SQL `DEFAULT 'PENDING'`. The benchmark overview repeats these known issues
as current defects and should be updated after the reference is corrected. The migration guide and cheat sheet
already use valid examples; no recipe or feature-guide changes are needed.

## Implementation Notes

Updated the annotation reference to use `@DbRename("full_name")` with the `value` attribute and a complete
compilable Java field example. Documented that Prefab quotes string and enum defaults, with `@DbDefaultValue("PENDING")`
producing `DEFAULT 'PENDING'`. Updated the benchmark overview so it records the defect as corrected rather than
misleading agents about the current reference. Searched the docs, cheat sheet, recipes, and feature guides for the
incorrect annotation forms; no other affected examples were found.

`git diff --check` and the search for the incorrect forms passed. Two full `mvn -q verify` attempts were blocked by
unrelated Windows test failures: a RocksDB temp-file access error in `prefab-streams-kafka`, then an invalid
`/C:/...` path in `AgentContextPluginTest`.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The annotation reference shows `@DbRename` with its `value` attribute and a compiling example
- [x] #2 No other agent-facing document uses `oldName` for `@DbRename`
- [x] #3 The `@DbDefaultValue` reference says that string values are quoted by Prefab, with an example that generates `DEFAULT 'PENDING'`
<!-- AC:END -->
