---
id: TASK-327
title: Expand Reference fields in frontend views without N+1
status: To Do
assignee: []
created_date: '2026-11-17 10:00'
labels: [feature, persistence, bff]
dependencies: [TASK-326]
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Allow a `@ViewOf` field to resolve a `Reference<T>` into a view of the referenced aggregate with one batched lookup per page. Requires a batch `findAllById` capability on generated repositories, explicit semantics for missing references, and explicit limits per persistence backend. Overlaps TASK-319: reuse its projection contract rather than defining a second one.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Resolving a page of N items issues a constant number of queries
- [ ] #2 Missing referenced records have documented, tested behaviour
- [ ] #3 Tenant and row restrictions of the referenced aggregate apply; no data leaks
- [ ] #4 Unsupported backends fail at compile time with a clear message
<!-- AC:END -->
