---
id: TASK-287
title: Add fluent as(RequestPostProcessor...) security override to generated test clients
status: Done
assignee: []
created_date: '2026-09-21 13:20'
labels:
  - testing
  - security
dependencies:
  - TASK-286
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Generated test clients mock a user per endpoint at code-generation time, derived from
the `@Security` annotation on that endpoint (role/authority baked in). There is no way
for a test method to override the mocked role/authority for a single call, short of
duplicating the generated client's request-building logic.

Add a fluent `client.as(RequestPostProcessor...)` method to generated test clients that
returns a view of the client applying the given request post processors (e.g.
`SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")`) instead of the
default mocked user, for that call chain only.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every generated test client (for aggregates whose endpoints use `@Security`) exposes a public `as(RequestPostProcessor... requestPostProcessors)` method returning a client instance that applies the given post processors instead of the default mocked user for subsequent calls
- [x] #2 Calling `as(...)` does not mutate the original client instance; the original client continues using its default mocked user
- [x] #3 When no `as(...)` override is supplied, generated client behaviour is unchanged from today (same default mocked user per endpoint)
- [x] #4 Endpoints with `@Security(enabled = false)` are unaffected by `as(...)` overrides, consistent with today's behaviour of not applying a mock user to permitAll endpoints (documented as a known limitation)
- [x] #5 `ControllerUtil` and `TestClientWriter` are the only annotation-processor classes that need to change; per-endpoint writer classes (Create/Update/Delete/GetById/GetList/Autocomplete/Binary/CreateOrUpdate) are unaffected
- [x] #6 Annotation-processor tests cover the generated `as(...)` method and the override behaviour; expected-output fixtures are updated accordingly
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- `TestClientWriter.buildClientType` now (when `ControllerUtil.SECURITY_INCLUDED`) adds a `private final List<RequestPostProcessor> securityOverrides` field, a private all-args constructor `(MockMvc, JsonMapper, List<RequestPostProcessor>)`, a public `as(RequestPostProcessor... requestPostProcessors)` method returning a fresh client instance (original instance is untouched — `as(...)` never mutates `this`), and a private `applySecurityOverride(RequestPostProcessor defaultPostProcessor)` helper that returns the override chain when `securityOverrides` is non-empty, otherwise the endpoint's default mocked user.
- `ControllerUtil.withMockUser(Security security)` now wraps its emitted post processor in `applySecurityOverride(...)` instead of applying `SecurityMockMvcRequestPostProcessors.user("test")...` directly. Because every generated `*TestClientWriter` class already calls `ControllerUtil.withMockUser(...)`, none of them needed to change.
- Endpoints with `@Security(enabled = false)` still emit no `.with(...)` at all (unchanged), so `as(...)` has no effect there — this is the documented limitation from AC #4.
- Added `TestClasses.REQUEST_POST_PROCESSOR` for `org.springframework.test.web.servlet.request.RequestPostProcessor`.
- Extended `SecurityGenerationTest` to assert the generated `as(RequestPostProcessor...)` method, the `applySecurityOverride` helper, and that a secured endpoint's default post processor is routed through it.
- Depends on TASK-286 (constructor simplification) landing first since both touch `TestClientWriter.buildConstructor()`.
- Verified with `mvn -pl annotation-processor -am test` (392 tests, all green).
<!-- SECTION:NOTES:END -->
