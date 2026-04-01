---
id: TASK-102
title: Generate mothers
status: To Do
assignee: []
created_date: '2026-04-01 17:11'
labels:
  - "\U0001F4E6feature"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Object Mothers (also known as test data factories or test fixtures) are a well-established testing pattern that provides pre-built, ready-to-use domain objects for tests. Currently, developers writing tests against Prefab-generated aggregates must manually instantiate their domain objects with all required fields. Prefab should auto-generate an Object Mother class for each aggregate so that tests can get a sensible default instance (or a customised variant) with minimal boilerplate.\n\nThe guiding principle is Prefab's philosophy of start high, dive deep when you need to: a generated mother should compile and work out of the box, while still allowing developers to override individual fields when a specific state is required.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Introduce a MotherWriter (or MotherPlugin) in the annotation processor that generates a {AggregateName}Mother class in the test sources for every aggregate
- [ ] #2 The generated mother class provides a static mother() factory method that returns a fully-constructed aggregate instance with sensible default values for all fields
- [ ] #3 Scalar field defaults follow a predictable convention (e.g. String fields default to the field name as a string, numeric fields default to 1, booleans to false, Reference fields to a generated stub reference)
- [ ] #4 The mother class exposes a fluent builder-style API (e.g. mother().withName("Alice")) so individual fields can be overridden without re-specifying all other fields
- [ ] #5 Nested single-value types and embedded value objects are also populated with defaults using the same convention
- [ ] #6 MotherWriter is registered as a PrefabPlugin in META-INF/services/be.appify.prefab.processor.PrefabPlugin and wired into PrefabProcessor
- [ ] #7 Generated mothers are written to the test source output directory (consistent with TestClientWriter and other test support writers)
- [ ] #8 Add unit tests for MotherWriter following the pattern used by existing plugin tests (e.g. CreatePluginTest)
<!-- AC:END -->
