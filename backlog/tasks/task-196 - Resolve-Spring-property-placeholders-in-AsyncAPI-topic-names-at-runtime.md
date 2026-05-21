---
id: TASK-196
title: Resolve Spring property placeholders in AsyncAPI topic names at runtime
status: Done
assignee: []
created_date: '2026-05-11 17:40'
updated_date: '2026-05-21 06:21'
labels:
  - async-api
  - bug
dependencies: []
priority: medium
ordinal: 30200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Topics configured as Spring property placeholders (e.g. `${topics.channel.name}`) in `@Event` annotations appear verbatim in the generated `asyncapi.json`. This makes the AsyncAPI document invalid because the raw placeholder is not a real topic name.

The `asyncapi.json` is generated at compile time by the annotation processor, so Spring properties are not available then. The fix must resolve placeholders at runtime, inside `AsyncApiController`, using `Environment.resolvePlaceholders()` before returning the document content.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 AsyncAPI JSON served by GET /async-api/asyncapi.json has all Spring property placeholders (${…}) in channel names replaced with their actual configured values
- [x] #2 If a placeholder cannot be resolved, a meaningful error is logged and the endpoint returns a 500 response
- [x] #3 Existing AsyncAPI generation tests continue to pass
- [x] #4 A new test covers placeholder resolution in the controller
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fixed by injecting `Environment` into `AsyncApiController` via constructor and calling `environment.resolveRequiredPlaceholders(raw)` on the JSON string before returning it.

Key changes:
- `AsyncApiController`: added `Environment` field; no-arg constructor replaced with `Environment`-accepting constructor; `asyncApiJson()` now pipes the raw content through `resolvePlaceholders()` helper; unresolvable placeholders are logged at ERROR and propagated as `IllegalArgumentException`.
- `async-api/pom.xml`: added `spring-boot-starter-test` test-scope dependency for `MockEnvironment` and AssertJ.
- `src/test/resources/META-INF/async-api/asyncapi.json`: synthetic test resource with a `${topics.orders}` placeholder to drive two unit tests.
- `AsyncApiControllerTest`: two tests — one verifies resolved output, one verifies the exception path when a placeholder is undefined.

Commit: `fix: resolve Spring property placeholders in AsyncAPI topic names at runtime`
<!-- SECTION:NOTES:END -->
