---
id: TASK-380
title: Evaluate generated frontend clients and an admin UI
status: To Do
assignee: []
created_date: '2026-11-24 10:00'
labels:
  - frontend
  - productivity
dependencies: []
priority: medium
type: spike
ordinal: 209000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab removes most backend code, so in a full-stack application most of the remaining effort, for humans and agents,
is the frontend: calling the API, paging and filtering lists, forms that mirror request records and their validation,
handling 400 responses, and the internal back-office screens every business app needs. Generic OpenAPI generators
produce clients that know nothing about Prefab's conventions (filters, paging, `Reference<T>`, binary fields,
autocomplete).

Prefab knows all of this at compile time. Two candidates:
1. A typed TypeScript client (and validation schemas) generated from Prefab metadata, with list/filter/paging
   helpers, versioned with the app.
2. A generated admin UI (in the spirit of Django admin) for aggregates: list with filters and paging, detail,
   create/update forms from request records, actions from `@Update` endpoints, respecting `@Security`.

Related: TASK-320 (backend-for-frontend spike, done), TASK-096 (OpenAPI), TASK-127 (exposure strategies), TASK-374
(local run as the backend for frontend development).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Compare a Prefab-aware generated client with a standard OpenAPI generator on the showcase: code written by hand, type safety for filters and paging, effort for an agent to build a list-and-form screen
- [ ] #2 Prototype an admin UI for two showcase aggregates and record what metadata is missing (labels, display fields, ordering) and whether it needs new annotations
- [ ] #3 Recommend build, defer or reject for each candidate in an ADR, with follow-up tasks for what is accepted
<!-- AC:END -->
