---
id: TASK-115
title: Move ClassManifest and TypeManifest caches into PrefabContext
status: To Do
assignee: []
created_date: '2026-04-10 05:00'
labels:
  - "\U0001F527refactor"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`ClassManifest` and `TypeManifest` both maintain static `ConcurrentHashMap` caches to avoid creating duplicate manifest objects for the same underlying type:

```java
// ClassManifest
private static final Map<TypeElement, ClassManifest> manifestCache = new ConcurrentHashMap<>();

// TypeManifest
private static final Map<Class<?>, TypeManifest> manifestByClassCache = new ConcurrentHashMap<>();
private static final Map<TypeMirror, TypeManifest> manifestByTypeMirrorCache = new ConcurrentHashMap<>();
```

Static caches have the following problems in annotation-processor environments:

1. **Cross-round contamination**: Java annotation processors can run across multiple rounds. Static state accumulated in round N may affect round N+1 in unexpected ways.
2. **Test isolation failures**: Unit tests that create multiple `ProcessingEnvironment` instances (e.g., via compile-testing frameworks) share the same caches, causing false positives or hard-to-debug state leakage between test cases.
3. **Memory leaks**: Type mirrors and type elements from one processing run are held alive indefinitely because the static map prevents them from being garbage-collected.

The fix is to move the caches into `PrefabContext` so that each processing round (and each test case) starts with a fresh cache. The factory methods `ClassManifest.of(...)` and `TypeManifest.of(...)` should accept the context (or a dedicated registry object) and use its caches rather than static maps.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 ClassManifest and TypeManifest no longer contain static cache maps
- [ ] #2 A ManifestRegistry (or equivalent) is added to PrefabContext that holds per-round ClassManifest and TypeManifest caches
- [ ] #3 The ClassManifest.of() and TypeManifest.of() factory methods use the registry from the supplied context instead of static state
- [ ] #4 Each new PrefabContext (i.e. each processing round) starts with an empty cache; stale state from a previous round cannot leak into subsequent rounds
- [ ] #5 All existing annotation-processor tests continue to pass after the refactoring
<!-- AC:END -->
