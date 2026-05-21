---
id: TASK-226
title: Make generated assertion classes extensible with SELF type parameter
status: To Do
assignee: []
created_date: '2026-05-21 09:51'
labels:
  - annotation-processor
  - test
  - assertions
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Generated `*Assert` classes (response, event, and nested) currently use a `private` constructor and have no self-referential type parameter, which prevents developers from subclassing them to add domain-specific custom assertions — the standard AssertJ extensibility pattern.

The fix is two-fold:
1. Introduce a `SELF` type parameter on each generated assert class so that fluent methods return the correct subtype when the class is extended.
2. Change the constructor visibility from `private` to `protected` so subclasses can call `super(actual, FooAssert.class)`.

Affected method in `AssertionWriter`: `privateConstructorFor()` → rename/change to emit a `protected` constructor; `writeAssertClass()` → add `SELF` type variable and update `superclass`, constructor, and all fluent method return types accordingly.

File: `annotation-processor/src/main/java/be/appify/prefab/processor/assertion/AssertionWriter.java`
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Each generated *Assert class declares a SELF type parameter bounded by the assert class itself (e.g. `public class FooAssert<SELF extends FooAssert<SELF>>` extends `AbstractAssert<SELF, Foo>`)
- [ ] #2 The constructor of each generated *Assert class is `protected` instead of `private`
- [ ] #3 All fluent assertion methods (hasX, hasXSatisfying) return `SELF` instead of the concrete assert type
- [ ] #4 The static `assertThat()` factory method is updated to instantiate correctly and return the concrete type (raw or wildcarded as needed)
- [ ] #5 A developer can subclass a generated assert class, add a custom assertion method, and have it return the subtype without casting
- [ ] #6 All existing generated-assertion tests continue to pass
- [ ] #7 Developer guide updated to show the subclassing pattern
<!-- AC:END -->
