---
id: TASK-386
title: Fix @DbRename and @DbDefaultValue examples in the annotation reference
status: To Do
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

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The annotation reference shows `@DbRename` with its `value` attribute and a compiling example
- [ ] #2 No other agent-facing document uses `oldName` for `@DbRename`
- [ ] #3 The `@DbDefaultValue` reference says that string values are quoted by Prefab, with an example that generates `DEFAULT 'PENDING'`
<!-- AC:END -->
