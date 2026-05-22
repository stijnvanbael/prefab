---
id: TASK-228
title: Set defaults from AVSC file in generated builders
status: In Progress
assignee: []
created_date: '2026-05-22 08:04'
updated_date: '2026-05-22 08:08'
labels:
  - avro
  - codegen
  - builder
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When generating Java builder classes from an AVSC (Avro Schema) file, the builder's setter methods should be pre-populated with the default values defined in the schema fields (`"default"` attribute in the AVSC).

Currently, `AvscEventWriter` generates builders via `BuilderWriter.enrichWithBuilder(...)` but passes stripped parameters without any default value information. Avro field defaults (e.g. `"default": null`, `"default": "some-value"`, `"default": 42`) should be read from `Schema.Field.defaultVal()` and used to initialise the corresponding builder fields, so callers only need to override the values they care about.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 When an AVSC field declares a `"default"` value, the generated builder initialises the corresponding field with that default.
- [ ] #2 Supported default types: null, String, int, long, double, float, boolean, and enum symbols.
- [ ] #3 Array fields with a default of `[]` initialise the builder field to an empty list.
- [ ] #4 Fields without a declared default retain the current behaviour (no initialisation).
- [ ] #5 Existing tests continue to pass after the change.
- [ ] #6 New unit/integration tests cover the default-value propagation for each supported type.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Analysis

### Key Classes

| Class | Location | Role |
|---|---|---|
| `AvscEventWriter` | `avro-processor/.../AvscEventWriter.java` | Reads AVSC `Schema` and generates Java record + builder |
| `BuilderWriter` | `annotation-processor/.../BuilderWriter.java` | Emits the nested `Builder` class via JavaPoet |

### Current Flow

1. `AvscEventWriter.buildTopLevelRecord` (and `buildNestedRecord`) call `buildFields(schema, ...)` → returns `List<ParameterSpec>`.
2. Before calling `BuilderWriter.enrichWithBuilder`, they strip annotations from each `ParameterSpec` via `strippedParams(fields)` — returning params with only type + name.
3. `BuilderWriter.buildNestedBuilderClass` iterates the stripped params, adds a private field for each and a `withX(...)` setter — **no initialisation**.

### What needs to change

**Option A – extend `BuilderWriter.enrichWithBuilder` to accept default values**
Pass a `Map<String, String>` (fieldName → initialiser literal) alongside the fields. `BuilderWriter` then emits `private String name = "foo"` instead of `private String name`.

**Option B – keep `BuilderWriter` generic, do the wiring in `AvscEventWriter`**
`AvscEventWriter` creates a new value type `FieldWithDefault` (record) that pairs each `ParameterSpec` with an `Optional<Object>` default. A new `AvscBuilderWriter` (or overloaded `enrichWithBuilder`) accepts that list.

→ **Option A is simpler and cohesive** — `BuilderWriter` already owns the field-declaration responsibility. The signature becomes:

```java
public void enrichWithBuilder(TypeSpec.Builder recordBuilder, ClassName recordType,
                              List<ParameterSpec> fields, Map<String, String> fieldDefaults)
```

The `Map<String, String>` maps field name → JavaPoet literal initialiser string (e.g. `"\"hello\""`, `"42"`, `"null"`, `"List.of()"`).

### Default-value extraction in `AvscEventWriter`

`Schema.Field.defaultVal()` returns:
- `null` → no default declared (skip)
- `JsonProperties.NULL_VALUE` (sentinel object) → default is JSON `null` → emit `null`
- `String` → emit `"\"value\""`
- `Integer` → emit `"value"`
- `Long` → emit `"valueL"`
- `Double` → emit `"value"`
- `Float` → emit `"(float) value"`
- `Boolean` → emit `"true"` / `"false"`
- `List<?>` (empty or not) → if empty emit `"List.of()"` — non-empty out of scope for now

A new private method `defaultInitialiserFor(Schema.Field field): Optional<String>` handles this mapping.

### Backwards Compatibility

The existing `enrichWithBuilder(TypeSpec.Builder, ClassName, List<ParameterSpec>)` signature is used by other paths (annotation-processor driven `@Event` records). Either:
- Add an overload (preferred — zero breaking changes)
- Or delegate: the 3-arg version calls the 4-arg version with an empty map.

### Test Strategy

1. New AVSC file: `event/avsc/defaults/source/DefaultsAvsc.java` + `DefaultsAvscEvent.avsc` with fields covering all supported default types.
2. New test method `defaultsAvscEvent()` in `AvscPluginTest` asserting the generated Builder initialises each field correctly.
3. All existing tests must keep passing (no changes to existing expected output).

### Files to touch

- `annotation-processor/…/BuilderWriter.java` — add overloaded `enrichWithBuilder` with defaults map; emit `= <literal>` on field declarations.
- `avro-processor/…/AvscEventWriter.java` — extract defaults map from `schema.getFields()`, pass to the new overload; add `defaultInitialiserFor(Schema.Field)`.
- `avro-processor/src/test/resources/event/avsc/defaults/` — new AVSC + Java source for the test case.
- `avro-processor/src/test/java/…/AvscPluginTest.java` — new test method.
<!-- SECTION:PLAN:END -->
