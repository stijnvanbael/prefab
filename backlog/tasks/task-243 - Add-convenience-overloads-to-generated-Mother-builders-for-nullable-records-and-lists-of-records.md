---
id: TASK-243
title: >-
  Add convenience overloads to generated Mother builders for nullable records
  and lists of records
status: Done
assignee: []
created_date: '2026-05-29'
labels: []
dependencies: []
priority: medium
---

## Description

`MotherWriter` generates `Consumer<MotherBuilder>` overloads for nested-record fields so callers can
customise child objects inline. Two gaps exist that cause ambiguity or missing ergonomics:

1. **Nullable record fields** — when a field is a nullable record, calling the raw setter with `null`
   is ambiguous: the compiler cannot distinguish `null` (the record value) from `null` (a `Consumer`).
   A dedicated `withoutX()` overload that sets the field to `null` eliminates the ambiguity.

2. **List-of-record fields** — there is currently no ergonomic way to build a list of customised
   records inline. Three overloads are needed:
   - `xList(Consumer<NestedMotherBuilder>... customisers)` — varargs that creates one instance per
     consumer and collects them into a list.
   - `emptyX()` — sets the field to an empty list (avoids ambiguity with the varargs overload when
     the caller wants an empty list).
   - `withoutX()` — sets the field to `null` when the list field is nullable, for the same reason as
     the nullable-record case.

## Acceptance Criteria

- [ ] #1 A nullable record field `Foo foo` in an event or request record generates a `withoutFoo()` method on `MotherBuilder` that sets `foo` to `null` and returns `this`.
- [ ] #2 A `List<Foo>` field where `Foo` is a record generates a varargs overload `foos(Consumer<FooMother.MotherBuilder>... customisers)` that materialises one `Foo` per consumer and sets the list.
- [ ] #3 A `List<Foo>` field generates an `emptyFoos()` overload that sets the field to an empty list and returns `this`.
- [ ] #4 A nullable `List<Foo>` field generates a `withoutFoos()` overload that sets the field to `null` and returns `this`.
- [ ] #5 Non-nullable record fields and non-nullable list fields do not generate `withoutX()` methods.
- [ ] #6 All new overloads are generated for both event mothers and request-record mothers.
- [ ] #7 Existing generated code and tests are unaffected (no regressions).
- [ ] #8 Unit / integration tests cover each new overload variant.

## Plan

1. Extend `TypeManifest` / `VariableManifest` to expose nullability information if not already present.
2. In `MotherWriter.buildEventMotherBuilderInnerClass` and `buildRequestMotherBuilderInnerClass`:
   - For nullable record fields → emit `withoutX()`.
   - For `List<Record>` fields → emit varargs overload + `emptyX()`.
   - For nullable `List<Record>` fields → also emit `withoutX()`.
3. Update `MotherWriterTest` (and/or integration tests) to assert all new overload signatures.
4. Update `backlog/docs/generated-artefacts.md` to document the new overloads.

## Implementation Notes

- `MotherWriter.buildEventMotherBuilderInnerClass` and `buildRequestMotherBuilderInnerClass` updated
  to iterate list-of-record fields separately from plain nested-record fields.
- Added helpers: `isListOfNestedObjectType`, `withoutNullableFieldMethod`, `listVarargsMethod`,
  `listVarargsOverloadForEventMotherBuilder`, `listVarargsOverloadForRequestMotherBuilder`,
  `emptyListMethod`.
- Varargs parameter declared with `ArrayTypeName.of(consumerType)` as required by palantir javapoet's
  `.varargs(true)` contract.
- `withoutX()` casts `null` to the field's concrete type to resolve the compiler ambiguity with the
  `Consumer` overload.
- Test fixture: `mother/nullablerecord/source/{ShipmentEvent,Order}.java`; 7 new tests in
  `MotherPluginTest` covering all AC items.
- Committed: `feat: add withoutX, varargs, and emptyX overloads to generated Mother builders`

