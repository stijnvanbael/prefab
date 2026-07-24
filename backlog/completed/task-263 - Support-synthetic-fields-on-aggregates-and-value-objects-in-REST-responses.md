---
id: TASK-263
title: Support synthetic fields on aggregates and value objects in REST responses
status: Done
assignee: []
created_date: '2026-07-10 11:31'
labels:
  - feature-request
dependencies: []
priority: medium
---

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A new annotation (e.g. @Computed) can be placed on a no-arg method of an aggregate root or value object
- [x] #2 The annotated method return value is included as a read-only field in the generated REST response DTO
- [x] #3 The synthetic field is not present in request DTOs (create / update)
- [x] #4 The synthetic field is not mapped to a database column
- [x] #5 The field name in the response matches the method name
<!-- AC:END -->

## Analysis

Two distinct serialization paths need to be covered:

1. **Aggregates (and polymorphic subtypes)**: REST responses are *generated records*
   (`HttpWriter.writeResponseRecord` / `buildSubtypeResponseRecord`). The aggregate itself is never
   serialized by Jackson, so the processor must add a record component per `@Computed` method and
   populate it in the generated `from(...)` factory method.
2. **Value objects**: they are embedded *as-is* (domain type) in generated response records and
   serialized directly by Jackson. The processor cannot intercept this, so `@Computed` is
   meta-annotated with `@JacksonAnnotationsInside` + `@JsonProperty(access = READ_ONLY)`:
   the method result is serialized as a read-only property wherever the value object appears,
   and ignored during deserialization (request DTOs — AC #3).

Persistence (AC #4): Spring Data JDBC/Mongo map fields only, and DB migrations are derived from
fields, so no column is ever created for a method. One leak remains: `@DbDocument` value objects are
serialized to JSONB with Jackson (`PrefabMappingJdbcConverter`), which would store the computed
value redundantly. Fixed by rebuilding the converter's `JsonMapper` with an `AnnotationIntrospector`
that ignores `@Computed` members.

## Implementation plan

- New `be.appify.prefab.core.annotations.Computed` (METHOD target, RUNTIME retention, Jackson meta-annotations).
- `HttpWriter`: append computed components to response records (regular + polymorphic subtype) and
  to the `from(...)` calls.
- `PrefabProcessor`: eager validation — `@Computed` methods must be public, take no arguments and
  not return `void`; name must not clash with a field.
- `PrefabMappingJdbcConverter`: ignore `@Computed` members for JSONB storage.
- Tests: processor test (response contains field, requests don't), core Jackson round-trip test for
  value objects, postgres JSONB exclusion test, and end-to-end assertions in the kafka example.

## Implementation Notes

- `be.appify.prefab.core.annotations.Computed`: METHOD target, RUNTIME retention, meta-annotated
  with `@JacksonAnnotationsInside @JsonProperty(access = READ_ONLY)` so value objects embedded in
  responses expose the field automatically and request deserialization ignores it.
- `HttpWriter`: response records (regular and polymorphic subtype) gain one component per
  `@Computed` method (`computedParameters`), populated via the shared `responseAccessors` helper in
  the generated `from(...)` methods.
- `PrefabProcessor.validateComputedMethods`: compile error when a `@Computed` method is not public,
  has parameters, returns `void`, or clashes with a field name.
- `PrefabMappingJdbcConverter` (postgres): the injected `JsonMapper` is rebuilt with an
  `AnnotationIntrospector` that ignores `@Computed` members, so JSONB documents never store the
  derived value; legacy documents containing it still read fine (READ_ONLY).
- `AssertionPlugin` now includes `@Computed` methods when building response and nested assert
  classes, so generated assertion helpers expose synthetic field checks alongside regular record
  components.
- Documented in `backlog/docs/annotation-reference.md` (REST annotations + quick reference table).
- Tests: `ComputedFieldTest` (processor, 7 tests incl. polymorphic and validation failures),
  `ComputedTest` (core Jackson behaviour), `PrefabMappingJdbcConverterTest` (JSONB exclusion),
  and `ProductIntegrationTest` in the kafka example (end-to-end: aggregate `tagCount`, value object
  `summary` asserted on raw response JSON). Full suites of core, annotation-processor, postgres and
  examples/kafka pass.
