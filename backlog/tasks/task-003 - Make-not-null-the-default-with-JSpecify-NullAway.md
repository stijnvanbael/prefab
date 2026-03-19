---
id: TASK-003
title: Make not null the default with JSpecify/NullAway
status: To Do
assignee:
  - '@copilot'
created_date: '2025-10-10 13:32'
updated_date: '2026-03-18 19:03'
labels: []
dependencies: []
ordinal: 36000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Currently, Prefab treats all non-primitive fields as nullable by default unless explicitly annotated with @NotNull (jakarta.validation.constraints.NotNull). This is verbose and error-prone: developers must annotate every required field to prevent unwanted null values from being persisted or serialized. The industry best practice, as promoted by JSpecify (https://jspecify.dev), is to make non-null the default and only mark fields that can legitimately be null with @Nullable. Combined with NullAway (Uber's compile-time null-safety checker), this approach catches null-related bugs at compile time. This task migrates Prefab to use JSpecify annotations and configures NullAway to enforce null-safety across the entire codebase.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Add org.jspecify:jspecify dependency to core/pom.xml and annotation-processor/pom.xml
- [ ] #2 Configure NullAway via the ErrorProne Maven plugin in the root pom.xml so that null-safety violations cause compilation errors
- [ ] #3 Add package-info.java with @NullMarked to all source packages in core and annotation-processor modules so that non-null is the default in all Prefab code
- [ ] #4 Update VariableManifest.nullable() to return true only when the field is explicitly annotated with JSpecify @Nullable (instead of checking absence of @NotNull)
- [ ] #5 Update EventSchemaFactoryWriter to recognise JSpecify @Nullable (org.jspecify.annotations.Nullable) instead of jakarta.annotation.Nullable when deciding whether to wrap an Avro schema in a nullable union
- [ ] #6 Replace all uses of jakarta.annotation.Nullable with org.jspecify.annotations.Nullable in source files processed by the annotation processor (e.g., test resources NullableEvent.java)
- [ ] #7 Remove now-redundant @NotNull (jakarta.validation.constraints.NotNull) field annotations from domain classes in the examples modules, since non-null is the new default
- [ ] #8 All existing annotation-processor tests continue to pass after the changes
- [ ] #9 Prefab-generated database migrations produce NOT NULL columns by default for non-primitive fields; only fields annotated with @Nullable produce nullable columns
- [ ] #10 Prefab-generated Avro schemas produce required (non-null) fields by default; only fields annotated with @Nullable produce union[null, type] fields
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Research latest stable versions of jspecify and NullAway/ErrorProne
2. Add jspecify dependency to core/pom.xml and annotation-processor/pom.xml
3. Add ErrorProne + NullAway Maven plugin config to root pom.xml
4. Add package-info.java with @NullMarked to every package in core and annotation-processor
5. Flip VariableManifest.nullable() logic: remove @NotNull check, add check for JSpecify @Nullable
6. Update EventSchemaFactoryWriter import from jakarta.annotation.Nullable to org.jspecify.annotations.Nullable
7. Update test resource NullableEvent.java to use org.jspecify.annotations.Nullable
8. Remove @NotNull from domain class fields in examples (avro, kafka, pubsub examples)
9. Run annotation-processor tests to verify nothing is broken
10. Run full build to confirm NullAway finds no violations
<!-- SECTION:PLAN:END -->
