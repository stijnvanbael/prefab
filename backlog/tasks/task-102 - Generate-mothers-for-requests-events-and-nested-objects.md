---
id: TASK-102
title: 'Generate mothers for requests, events, and nested objects'
status: To Do
assignee: []
created_date: '2026-04-01 17:11'
updated_date: '2026-04-02 06:40'
labels:
  - "\U0001F4E6feature"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Object Mothers (test data factories) are a well-established testing pattern that provides pre-built, ready-to-use objects for tests. Prefab should auto-generate an Object Mother class for every generated request record (e.g. CreatePersonRequest, PersonUpdateRequest), every @Event class, and every nested value-object type used in their fields. Aggregate classes themselves do NOT get mothers.\n\nA new @Example annotation should be introduced in prefab-core so that developers can annotate any record component or event field with an example value string. This same annotation value is then propagated to:\n- The generated mother (used as the default field value instead of the generic fallback)\n- OpenAPI documentation (@Schema(example=...) or @Parameter(example=...) on the generated request record field)\n- AsyncAPI documentation ("example" property on the JSON Schema field of the event)
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add an @Example("value") annotation to prefab-core (be.appify.prefab.core.annotations) that can be placed on any record component or event field to declare a representative example value
- [ ] #2 Introduce a MotherWriter in the annotation processor that generates a {TypeName}Mother class in test sources for every generated request record (Create{Name}Request, {Name}UpdateRequest, etc.) and for every @Event-annotated class
- [ ] #3 Also generate mothers for nested value-object types referenced by request or event fields (i.e. types that are not single-value wrappers and not standard Java types)
- [ ] #4 The generated mother class provides a static mother() factory method returning a fully-constructed instance with sensible defaults: String fields default to the field name, numbers to 1, booleans to false, Reference fields to a generated stub id; if the field carries @Example the annotated value is used as the default instead
- [ ] #5 The mother class exposes a fluent builder-style API (e.g. PersonMother.mother().withName("Alice").build()) so individual fields can be overridden
- [ ] #6 When @Example is present on a request record component, the annotation processor emits a @Schema(example=...) (or @Parameter(example=...)) annotation on that field in the generated request record, so the value appears in OpenAPI / Swagger UI
- [ ] #7 When @Example is present on an event field, the EventSchemaDocumentationWriter includes an "example" key in the AsyncAPI JSON Schema for that field
- [ ] #8 MotherWriter is wired into PrefabProcessor and follows the same test-source output directory convention as TestClientWriter
- [ ] #9 Add unit tests for MotherWriter following the pattern used by existing plugin tests (e.g. CreatePluginTest)
<!-- AC:END -->
