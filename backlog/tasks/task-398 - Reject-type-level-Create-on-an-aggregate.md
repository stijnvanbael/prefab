---
id: TASK-398
title: Reject type-level @Create on an aggregate
status: To Do
assignee: []
created_date: '2026-11-26 13:00'
labels:
  - annotation-processor
  - agents
dependencies: []
references:
  - core/src/main/java/be/appify/prefab/core/annotations/rest/Create.java
  - annotation-processor/src/main/java/be/appify/prefab/processor/rest/AggregateEndpoints.java
priority: high
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found in TASK-392 (run `20261126-115004-qwen-local-cheat-sheet`, `rest-create-and-get`). Qwen put `@Create` on the
`@Aggregate` record instead of its constructor. `@Create` allows `ElementType.TYPE` (needed for `@ViewOf` create
forms), so this compiles without a diagnostic, and no create endpoint is generated: the controller only has
`GET /books/{id}`. Worse, `AggregateEndpoints` does list a type-level `@Create`, so the generated
`target/prefab/agent-context.md` advertises `POST /books (create)`. The agent read that file, trusted it, and
reported success; the hidden tests got 404.

The `@Create` Javadoc already says that on a synchronous aggregate the target must be a constructor (on an
`@AsyncCommit` aggregate a public static factory method).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `@Create` on an `@Aggregate` type is a compile error that says to annotate the constructor (or, for `@AsyncCommit`, a public static factory method)
- [ ] #2 `@Create` on a `@ViewOf` type keeps working
- [ ] #3 `agent-context.md` only lists endpoints that are actually generated; a test covers the type-level case
- [ ] #4 The annotation reference and troubleshooting docs mention the new diagnostic
<!-- AC:END -->
