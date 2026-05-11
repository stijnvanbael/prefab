---
id: TASK-191
title: >-
  Fix generated test client: enum fields passed to queryParam() instead of
  String
status: Done
assignee: []
created_date: '2026-05-11 09:15'
updated_date: '2026-05-11 09:20'
labels:
  - bug
  - annotation-processor
  - test-client
  - 'reported-by:maestro'
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem Statement

The Prefab annotation processor generates a `*Client.java` test helper for every `@Aggregate` that has `@GetList`. When the aggregate contains an `enum` field, the generated `findXxx()` method includes it as a search parameter and passes it directly to `MockMvcRequestBuilders.queryParam(String, String...)`:

```java
// Generated — does NOT compile
if (status != null) {
    request.queryParam("status", status);  // ← SessionStatus, not String
}
```

`queryParam` is declared as `queryParam(String name, String... values)`. Passing an `enum` value where a `String` is expected causes a compile error.

### Affected scenario

Any `@Aggregate` with `@GetList` that contains a field of an `enum` type.

### Current workaround (Maestro)

Added `<testExclude>**/*Client.java</testExclude>` to the `maven-compiler-plugin` configuration in `pom.xml` to suppress compilation of all generated test clients. Tests are written using `MockMvc` directly.

## Proposed Fix

In the `GetListPlugin` (or wherever the test client `findXxx()` method is generated), call `.name()` or `.toString()` on enum values before passing to `queryParam()`:

```java
// Fixed
if (status != null) {
    request.queryParam("status", status.name());
}
```

More robustly: use `String.valueOf(value)` or detect the parameter type and emit the appropriate conversion for all non-`String` types (enums, numbers, etc.).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Generated `*Client.java` calls `.name()` (or `String.valueOf()`) for enum fields
- [ ] #2 Generated `*Client.java` compiles cleanly with enum fields present in `@GetList` aggregates
- [ ] #3 Existing integration test clients that use numeric or boolean filter fields also compile
- [ ] #4 Unit test in `prefab-annotation-processor` verifies the generated `findXxx()` for an enum-valued aggregate
- [ ] #5 `<testExclude>` workaround in consuming projects can be removed
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fixed in Prefab 0.8.0 (no tracked task existed at the time). Confirmed resolved: the testExclude workaround and manual ConversationSessionClient override are both gone from Maestro pom.xml / src/test/java.
<!-- SECTION:NOTES:END -->
