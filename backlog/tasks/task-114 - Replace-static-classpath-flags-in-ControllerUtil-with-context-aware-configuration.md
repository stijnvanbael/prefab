---
id: TASK-114
title: Replace static classpath flags in ControllerUtil with context-aware configuration
status: To Do
assignee: []
created_date: '2026-04-10 05:00'
labels:
  - "\U0001F527refactor"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`ControllerUtil` contains two static boolean flags that are evaluated once at class-load time:

```java
public static final boolean SECURITY_INCLUDED = isSecurityIncluded();
public static final boolean OPENAPI_INCLUDED = isOpenApiIncluded();
```

These flags are then consulted in many generated-code helpers (`securedAnnotation()`, `withMockUser()`, `tagAnnotation()`, `operationAnnotation()`, `pathParameterAnnotation()`).

This design has several downsides:

1. **Untestable**: Tests cannot simulate the presence or absence of Spring Security or OpenAPI without actually adding/removing them from the compile classpath.
2. **Implicit global state**: The flags live in a utility class with no clear ownership; any code can read them without going through a context object.
3. **Inconsistent with the rest of the framework**: `PrefabContext` is already the central object for passing processing configuration through the system. Classpath capabilities are a natural fit there.

The refactoring moves these capability flags to `PrefabContext` (or a dedicated `PrefabCapabilities` record within it) and converts the static helper methods in `ControllerUtil` to instance methods or static methods that accept the context as a parameter.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 SECURITY_INCLUDED and OPENAPI_INCLUDED are no longer static fields on ControllerUtil
- [ ] #2 The capability flags are computed once during PrefabContext construction and exposed via accessor methods (e.g., context.isSecurityIncluded(), context.isOpenApiIncluded())
- [ ] #3 All ControllerUtil helper methods that previously read the static flags are updated to receive the context or the capability values as parameters
- [ ] #4 All existing annotation-processor tests continue to pass; tests that need to simulate absent dependencies can do so by constructing a PrefabContext with the appropriate flags set to false
<!-- AC:END -->
