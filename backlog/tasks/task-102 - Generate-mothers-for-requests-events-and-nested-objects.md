---
id: TASK-102
title: 'Generate mothers for requests, events, and nested objects'
status: To Do
assignee: []
created_date: '2026-04-01 17:11'
updated_date: '2026-04-24 06:57'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 125000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Object Mothers (test data factories) are a well-established testing pattern that provides pre-built, ready-to-use objects for tests. Prefab should auto-generate an Object Mother class for every generated request record (e.g. CreatePersonRequest, PersonUpdateRequest), every @Event class, and every nested value-object type used in their fields. Aggregate classes themselves do NOT get mothers.

A new @Example annotation should be introduced in prefab-core so that developers can annotate any record component or event field with an example value string. @Example is valid on:
- Aggregate fields — the annotation processor propagates the value to the corresponding field in the generated REST response record (e.g. PersonResponse), so the example appears in OpenAPI / Swagger UI for GET responses
- Constructor parameters of aggregates (annotated with @Create, @Update, etc.) — the annotation processor propagates the value to the generated request record component
- Record components of @Event classes

This same annotation value is then propagated to:
- The generated mother (used as the default field value instead of the generic fallback)
- OpenAPI documentation (@Schema(example=...) or @Parameter(example=...) on the generated request and response record field)
- AsyncAPI documentation ("example" property on the JSON Schema field of the event)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add an @Example("value") annotation to prefab-core (be.appify.prefab.core.annotations) that can be placed on any record component or event field to declare a representative example value
- [ ] #2 Introduce a MotherWriter in the annotation processor that generates a {TypeName}Mother class in test sources for every generated request record (Create{Name}Request, {Name}UpdateRequest, etc.) and for every @Event-annotated class
- [ ] #3 When @Example is present on a request record component, the annotation processor emits a @Schema(example=...) (or @Parameter(example=...)) annotation on that field in the generated request record, so the value appears in OpenAPI / Swagger UI
- [ ] #4 When @Example is present on an event field, the EventSchemaDocumentationWriter includes an "example" key in the AsyncAPI JSON Schema for that field
- [ ] #5 MotherWriter is wired into PrefabProcessor and follows the same test-source output directory convention as TestClientWriter
- [ ] #6 Add unit tests for MotherWriter following the pattern used by existing plugin tests (e.g. CreatePluginTest)
- [ ] #7 Also generate mothers for nested value-object types referenced by request or event fields. Mothers for nested types are generated when reachable from any mother target (request record or @Event class), regardless of whether they are also used in request records. Types that are single-value wrappers or standard Java types are excluded.
- [ ] #8 The generated mother class provides a static mother() factory method that directly returns a fully-constructed instance with sensible defaults: String fields default to the field name, numbers to 1, booleans to false, Reference fields to a generated stub id, @Nullable fields default to null, nested value-object fields default to {NestedTypeName}Mother.mother(); if the field carries @Example the annotated value is used as the default instead.
- [ ] #9 The mother class also exposes a static builder() method that returns a fluent builder allowing individual fields to be overridden before construction (e.g. CreatePersonRequestMother.builder().withName("Alice").build()). The static mother() method is a shorthand for builder().build() with all defaults.
- [ ] #10 The annotation processor raises a clear compiler error if an @Example value cannot be parsed as the target field type (e.g. @Example("not-a-number") on an int field), failing the build and pinpointing the offending element.
- [ ] #11 When @Example is present on an aggregate field, the annotation processor propagates a @Schema(example=...) annotation to the corresponding field in the generated REST response record (e.g. PersonResponse), so the example value appears in OpenAPI / Swagger UI for GET responses.
<!-- AC:END -->
