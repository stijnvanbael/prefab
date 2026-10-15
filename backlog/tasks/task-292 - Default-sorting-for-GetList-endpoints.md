---
id: TASK-292
title: Default sorting for @GetList endpoints
status: Done
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
- [x] #1 `@GetList` can declare a default sort (one or more fields, each ascending or descending)
- [x] #2 The default sort is applied by the generated controller/service when the request has no `sort` parameter
- [x] #3 An explicit `sort` request parameter overrides the default completely
- [x] #4 The default sort works together with filters (including range filters), paging, tenant and parent
- [x] #5 Unknown sort fields are a compile error
- [x] #6 Works on PostgreSQL and MongoDB; generated test client and OpenAPI reflect the behaviour
- [x] #7 Annotation reference and Developer Guide updated
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- New `@SortOrder` annotation + `GetList.defaultSort()`.
- Generated controller annotates the `Pageable` parameter with Spring's `@SortDefault.SortDefaults`; an explicit `sort` param overrides it completely, and it is independent of filters/paging/tenant/parent and of the store (PostgreSQL/MongoDB). The test client only sends `sort` when given, so the default applies.
- Unknown fields raise a compile error in `GetListPlugin` (`ClassManifest.element()` added for error location).
- Tests: `DefaultSortTest`. Docs: annotation-reference, feature-guides.
<!-- SECTION:NOTES:END -->
