---
id: TASK-292
title: Default sorting for @GetList endpoints
status: To Do
assignee: []
created_date: '2026-10-15 10:55'
labels:
  - rest
  - getlist
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Allow an aggregate annotated with `@GetList` to declare a default sort order that is applied when the request does not specify a `sort` parameter. Without it, list results come back in unspecified database order, which makes paging unstable. An explicit `sort` request parameter must always override the default.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `@GetList` can declare a default sort (one or more fields, each ascending or descending)
- [ ] #2 The default sort is applied by the generated controller/service when the request has no `sort` parameter
- [ ] #3 An explicit `sort` request parameter overrides the default completely
- [ ] #4 The default sort works together with filters (including range filters), paging, tenant and parent
- [ ] #5 Unknown sort fields are a compile error
- [ ] #6 Works on PostgreSQL and MongoDB; generated test client and OpenAPI reflect the behaviour
- [ ] #7 Annotation reference and Developer Guide updated
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
