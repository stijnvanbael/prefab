---
id: TASK-116
title: Split TypeManifest into focused, single-responsibility classes
status: To Do
assignee: []
created_date: '2026-04-10 05:00'
labels:
  - "\U0001F527refactor"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`TypeManifest` is a 438-line class that handles many distinct concerns:

- **Type identity**: package name, simple name, fully qualified name, equality.
- **Java type metadata**: kind (class/record/interface/enum), primitiveness, boxing, type parameters.
- **Annotation introspection**: `annotationsOfType()`, `inheritedAnnotationsOfType()`, `supertypes()`, `supertypeWithAnnotation()`.
- **Member introspection**: `fields()`, `methodsWith()`, `enumValues()`.
- **Polymorphism**: `isSealed()`, `permittedSubtypes()`.
- **Prefab-specific concepts**: `isSingleValueType()`, `singleValueAccessor()`, `isCustomType()`, `isStandardType()`.
- **Code generation helpers**: `asTypeName()`, `asBoxed()`, `asClass()`, `asElement()`, `asClassManifest()`.

This wide surface makes the class difficult to understand and modify. Adding new functionality risks unintended interactions between concerns.

The refactoring should extract cohesive subsets into separate, focused classes or delegation objects. Possible splits:

- **`TypeIdentity`** – package, simple name, fully-qualified name, equality, `asTypeName()`.
- **`TypeAnnotations`** – annotation introspection methods.
- **`TypeMembers`** – fields, methods, enum values.

`TypeManifest` can remain as the public facade that delegates to these helpers, keeping the existing API stable while reducing internal complexity.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 TypeManifest is refactored so that no single class exceeds a reasonable size (suggested guideline: 250 lines)
- [ ] #2 The public API of TypeManifest (all existing public methods) remains unchanged so that no call sites outside TypeManifest need to be updated
- [ ] #3 The extracted helper classes are package-private (not part of the public API) and focused on a single concern
- [ ] #4 All existing annotation-processor tests continue to pass after the refactoring
<!-- AC:END -->
