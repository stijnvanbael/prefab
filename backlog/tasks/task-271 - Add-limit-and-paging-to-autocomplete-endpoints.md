---
id: TASK-271
title: Add limit and paging to autocomplete endpoints
status: Done
assignee: []
created_date: '2026-08-24 10:45'
updated_date: '2026-08-24 11:08'
labels:
  - ✨feature
  - rest
  - autocomplete
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Extend generated `@Autocomplete` endpoints so callers can control how many results are returned and request subsequent pages. The annotation should expose a configurable default limit while the generated REST API accepts paging inputs without switching the public contract to Spring's `size` parameter.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `@Autocomplete` exposes a `limit()` attribute with default value `20`
- [x] #2 Generated controller methods accept `query`, `page`, and `limit` request parameters, defaulting to page `0` and the annotation-configured limit
- [x] #3 Generated service methods propagate page and limit to the repository via `PageRequest`
- [x] #4 Generated autocomplete test clients can request non-default pages and limits
- [x] #5 Autocomplete generation tests and docs are updated to describe the new contract
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Analysis

- Existing autocomplete generation already uses `Pageable` at the repository layer, but the generated service hard-codes `PageRequest.of(0, 10)` and the controller exposes only `query`.
- The earlier autocomplete task explicitly called out a future `limit()` attribute; this change closes that gap and keeps the REST contract aligned with the user's `limit` terminology instead of exposing Spring Data's `size`.
- The most direct design is:
  - add `limit()` to `@Autocomplete`
  - generate controller params `query`, `page=0`, `limit=<annotation value>`
  - generate service methods `autocompleteByX(query, page, limit)`
  - keep repository signatures on `Pageable`
  - let generated test clients translate paging requests into `page` and `limit` query params
- Implementation started in `core/.../Autocomplete.java` and the autocomplete controller, service, and test-client writers; docs and plugin tests are being updated alongside the generator so the new public contract is captured in one change.
- `@Autocomplete` now exposes `limit()` with default `20`; generated controllers map `page` and `limit` as request parameters and pass them into new paged service overloads while preserving a convenience query-only service method that uses the configured default limit.
- Generated autocomplete test clients now offer both the existing simple helper and a `Pageable` overload that serialises `page` and `limit`, keeping paging testable without leaking Spring's `size` parameter into the HTTP contract.
- Updated `annotation-reference.md` and `feature-guides.md` to document the new `page`/`limit` query parameters and the annotation-level default limit.
- Validation completed with:
  - targeted autocomplete generation tests
  - full `core` + `annotation-processor` Maven tests via `mvn -q -pl core,annotation-processor -am test`
- A repository-wide `mvn -q test` run was started as well, but it was aborted after example integration tests spent an extended time repeatedly pulling `apache/kafka:4.0.2` in the shared environment.
<!-- SECTION:NOTES:END -->
