---
id: TASK-325
title: Standardize nullability annotations on JSpecify instead of Jakarta
status: To Do
assignee: []
created_date: '2026-11-16 16:16'
labels: [annotation-processor, dx, refactor]
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Complete the migration of nullability annotations to JSpecify across Prefab source, generated code, examples and test fixtures. Make `org.jspecify.annotations.Nullable` the canonical optional-value annotation and handle its type-use semantics correctly throughout generation.

Related: completed TASK-003 (non-null defaults/JSpecify/NullAway). Despite its Done status, current processor and Avro writers, fixtures and examples still use `jakarta.annotation.Nullable`, while several runtime modules already use JSpecify. This story addresses that remaining inconsistency rather than recreating the original default-nullability work.

JSpecify describes nullness, not runtime validation or bean lifecycle. Retain Jakarta Validation annotations such as `@Valid`, `@NotNull`, `@Size` and other constraints where they enforce runtime behavior, and retain Jakarta lifecycle annotations such as `@PreDestroy`. Do not remove Jakarta dependencies that are still required by those APIs.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Inventory nullability annotations and dependencies in handwritten/generated code, examples, fixtures and documentation; JSpecify becomes the canonical emitted and demonstrated nullability vocabulary.
- [ ] #2 Shared metadata recognizes JSpecify type-use annotations on record components, backing fields, accessor return types, parameters, arrays and generic arguments without confusing a nullable container with nullable elements.
- [ ] #3 Preserve existing Prefab required/optional semantics in PostgreSQL and MongoDB persistence, REST contracts, Avro and AsyncAPI schemas, object mothers, assertions and builders; changing only the annotation vocabulary introduces no unintended schema migration or wire-contract change.
- [ ] #4 Define an explicit compatibility policy for legacy Jakarta nullability annotations. Prefer continued recognition during migration while emitting only JSpecify; document precedence and any planned deprecation rather than silently treating existing nullable fields as required.
- [ ] #5 Use `@NullMarked`, `@NullUnmarked` and `@NonNull` only where their scopes and exceptions are deliberate and verified. Do not enable package-wide checking by mass annotation if existing contracts cannot satisfy it; reuse existing null-safety tooling and track broader enforcement separately if needed.
- [ ] #6 Preserve Jakarta runtime validation and lifecycle behavior and retain necessary dependencies; ensure published modules expose the JSpecify dependency needed to compile their public/generated contracts.
- [ ] #7 Add compilation and schema regressions for JSpecify and legacy inputs, nullable records and nested collections, generated AVSC fields and cross-module usage. Verify native IntelliJ incremental annotation discovery as well as Maven compilation.
- [ ] #8 Update annotation/feature guides and migration instructions with supported imports, type-use placement, default semantics and the difference between static nullness and runtime validation.
<!-- AC:END -->

## Analysis

<!-- SECTION:ANALYSIS:BEGIN -->
`VariableManifest`, `TenantPlugin`, `EventSchemaDocumentationWriter`, `AvscEventWriter` and `EventSchemaFactoryWriter` currently import Jakarta Nullable. Several core/PostgreSQL/MongoDB runtime types already use JSpecify Nullable. `feature-guides.md` still names Jakarta Nullable as an optional marker. Jakarta `PreDestroy` is used for lifecycle management and must remain.

TASK-003's acceptance checklist is not checked despite its Done status, so verify the implementation rather than assuming JSpecify detection or NullAway enforcement is already complete. Reuse the record-component/backing-field metadata handling and avoid duplicating nullability logic in individual writers.

### Implementation findings (2026-11-18)

- The working tree was clean at task start. No `backlog-prefab` MCP tools are available in this session, so task metadata and notes are maintained in this file.
- `VariableManifest.nullable()` currently reads declaration annotations and treats any annotation whose simple name is `Nullable` as a marker. It does not consistently inspect the annotated root type; this is important because JSpecify `@Nullable` has `TYPE_USE` semantics.
- Record fields already merge record-component declaration annotations to accommodate IDE incremental compilation, but do not merge root type-use annotations from the component or accessor return type.
- `EventSchemaDocumentationWriter` and `TenantPlugin` bypass `VariableManifest.nullable()`. Event compatibility validation also has a separate simple-name check. These paths need to use shared nullable metadata.
- Avro writers still import Jakarta `Nullable` for generated source. Existing runtime JSpecify imports depend on unrelated transitive APIs; `prefab-core` has no direct JSpecify dependency, so generated/public contracts need an explicit compile dependency.
- Compatibility policy: recognize JSpecify `@Nullable` and legacy `jakarta.annotation.Nullable` (and preserve the existing simple-name fallback for third-party annotations during this migration); when multiple markers occur, any nullable marker continues to make the value optional. Emit only JSpecify annotations. Jakarta validation and lifecycle annotations remain unchanged.
- Tests will cover root type-use positions, array and generic element distinction, legacy Jakarta detection, and the unchanged Avro nullable schema / generated annotation contract. No package-wide null-marking or NullAway scope expansion is planned.
- The new compiler-model regression test currently fails at the first JSpecify field assertion: `VariableManifest.nullable()` does not see `@Nullable` placed on the type use. This confirms the root metadata gap before implementation.
<!-- SECTION:ANALYSIS:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Audit current annotations, dependency exposure and nullability detection, including type-use metadata across compiler implementations.
2. Specify canonical JSpecify semantics and legacy compatibility; add failing metadata/schema tests before changing imports.
3. Update shared nullability detection, then generation writers, handwritten contracts, examples and fixtures while preserving runtime validation and lifecycle annotations.
4. Verify generated-source compilation, dependency-consumer compilation and before/after schema equivalence; test container-versus-element nullability explicitly.
5. Document the migration and run affected module suites followed by the full reactor, recording environmental limits separately from regressions.
<!-- SECTION:PLAN:END -->
