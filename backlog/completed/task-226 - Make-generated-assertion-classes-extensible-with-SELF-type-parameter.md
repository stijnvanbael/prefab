---
id: TASK-226
title: Make generated assertion classes extensible with SELF type parameter
status: Done
assignee: []
created_date: '2026-05-21 09:51'
updated_date: '2026-05-21 11:04'
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
- [x] #1 Each generated *Assert class declares a SELF type parameter bounded by the assert class itself (e.g. `public class FooAssert<SELF extends FooAssert<SELF>>` extends `AbstractAssert<SELF, Foo>`)
- [x] #2 The constructor of each generated *Assert class is `protected` instead of `private`
- [x] #3 All fluent assertion methods (hasX, hasXSatisfying) return `SELF` instead of the concrete assert type
- [x] #4 The static `assertThat()` factory method is updated to instantiate correctly and return the concrete type (raw or wildcarded as needed)
- [x] #5 A developer can subclass a generated assert class, add a custom assertion method, and have it return the subtype without casting
- [x] #6 All existing generated-assertion tests continue to pass
- [x] #7 Developer guide updated to show the subclassing pattern
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented SELF type parameter extensibility for all generated assert classes.

**Changes:**
- `AssertionWriter.java`: Added `TypeVariableName` import. In `writeAssertClass`, created a `SELF extends FooAssert<SELF>` type variable, added it to the class, updated the superclass to `AbstractAssert<SELF, SubjectType>`. Renamed `privateConstructorFor` → `protectedConstructorFor` with `Modifier.PROTECTED`. Updated all fluent method signatures to return `TypeVariableName selfType` (SELF) and changed `return this` → `return myself` throughout all field assert method builders.
- `AssertionPluginTest.java`: Updated `responseAssertClassExtendsAbstractAssert` test to check for `<SELF extends ProductResponseAssert<SELF>>` and `extends AbstractAssert<SELF, ProductResponse>`.
- `backlog/docs/generated-artefacts.md`: Updated docs to show new SELF-parameterised class shape for both ResponseAssert and EventAssert, and added a subclassing guide showing how to extend a generated assert class with custom domain assertions.

All 11 assertion plugin tests pass.
<!-- SECTION:FINAL_SUMMARY:END -->
