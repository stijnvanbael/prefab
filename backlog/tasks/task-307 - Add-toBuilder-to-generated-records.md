---
id: TASK-307
title: Add toBuilder to all generated records
status: In Progress
assignee: []
created_date: '2026-11-16 13:36'
labels:
  - feature
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Generate a public instance `toBuilder()` method for every Prefab-generated Java record so a caller can copy an existing instance, override selected components with the fluent builder, and build a new instance. Use the method in generated or handwritten code where it replaces meaningful copy-and-modify boilerplate without changing behavior or public APIs.

This applies to generated event, request, response, nested and other record variants, not just records that already have a builder. Prefab cannot add methods to handwritten records, including code-first `@Event` records; do not modify those types or claim they acquire `toBuilder()`. Sealed interfaces and other non-record types are outside the method's scope.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Every Prefab-generated record exposes a public instance `toBuilder()` returning its usable nested builder, including ordinary and polymorphic request/response records, AVSC-generated event and nested records, union branch wrappers, and historic snapshot records. Records without an existing builder gain one; sealed interfaces do not.
- [ ] #2 `record.toBuilder().build()` preserves every actual record component, including nulls, primitive values, nested values, lists, and values that differ from builder defaults. Setting any subset of components produces a new record with only those components changed; zero-component records also work.
- [ ] #3 Each invocation returns an independent builder. List adders on a copied builder, including when a source list is a mutable `ArrayList`, do not mutate the source record or a builder obtained from another invocation. Other reference components retain their existing shallow-copy semantics; canonical constructor validation and normalization still run on `build()`.
- [ ] #4 Generated APIs compile and remain fluent with the default, empty, and custom `prefab.builder.setterPrefix` values, including nested types and AVSC defaults. Existing `builder()`, setters, list adders, constructors, serialization, and generated convenience methods keep their behavior.
- [ ] #5 Copying is based on the record's actual components and types, not a potentially different test-client parameter representation; requests with path, binary, converted or omitted parameters are covered. Report a clear compiler diagnostic for an input that would produce an incompatible `toBuilder()` signature rather than silently generating uncompilable code.
- [ ] #6 Identify and replace applicable copy-and-modify boilerplate with `toBuilder()` where the source and target are the same generated record and the result is behaviorally equivalent. Preserve existing APIs and document inspected candidates or why none are suitable.
- [ ] #7 Add executable generator and runtime coverage for all generation families, including polymorphic and nested records, snapshots, union branches, defaulted and nullable fields, zero components, multiple builder prefixes, list isolation, and generated-source compilation. Run the full Maven suite and address regressions.
- [ ] #8 Update the generated-artefacts/developer documentation with the API, shallow-copy and list-adder behavior, examples, and the distinction between generated records and handwritten events.
<!-- AC:END -->

## Analysis

<!-- SECTION:ANALYSIS:BEGIN -->
- `BuilderWriter.enrichWithBuilder` currently emits `builder()` and a self-typed nested builder; its setters use the configured prefix. Its list adder copies non-`ArrayList` lists but mutates an `ArrayList` in place, so simply assigning a record's list to a builder is insufficient for isolation.
- `ControllerUtil.writeRecord` creates request components from body/method parameters but creates builder fields using `buildTestClientParameter`; these representations can differ. Its `withX` helpers construct a new record and are candidates for careful simplification only if their path-parameter and conversion semantics are preserved.
- `HttpWriter` writes ordinary and polymorphic subtype response records directly without `BuilderWriter`. `CreateRequestRecordWriter` and `UpdateRequestRecordWriter` likewise write polymorphic nested request records directly.
- `AvscEventWriter` already enriches top-level and named nested records, including schema defaults, but creates union branch wrapper records separately without builders. `AvscSnapshotPlugin` generates root and recursively discovered snapshot records without builders. Enumerate all record-generation sites again during implementation to catch additional paths.
- `MotherWriter` uses generated nested builders for generated records and standalone builders for handwritten event records. Preserve those existing extension patterns and avoid introducing a requirement to alter user-owned records.
- Implementation started: cover the shared writer first, then wire every direct record writer and verify the full reactor. Maven selects JDK 25 even though the default `java` executable is JDK 21.
- Shared writer now seeds a fresh builder directly from record components, copying lists to protect mutable source lists; the prefix-independent runtime regression test passes.
- Wired REST requests/responses and AVSC branch/snapshot writers to the shared generator. Request builders now use actual record components, and matching request `withX` helpers delegate to `toBuilder`; retained the legacy path for unmatched plugin-provided method parameters.
- Targeted REST and AVSC generation tests pass. Added coverage for custom-prefix copy chains across polymorphic requests/responses, snapshots, AVSC events, nested types and union wrappers, plus runtime checks for nulls, defaults and list isolation. Full reactor validation is in progress.
- First full reactor run reached the annotation-processor suite and found one source-text expectation affected by JavaPoet's equivalent nested-type spelling; fixed it and reran the expanded targeted suite successfully. A full rerun is in progress.
<!-- SECTION:ANALYSIS:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Inventory all generated record emission paths and the exact component lists, including ordinary/polymorphic REST records, AVSC records and branches, and snapshots. Identify any generated or handwritten copy-and-modify sites and record their suitability.
2. Extend the shared builder generation to emit `toBuilder()` seeded from every actual record component, overriding defaults even for null values. Reuse configurable fluent setter conventions or an internal initialization mechanism without assuming test-client parameter types. Ensure fresh builders and copy-on-write or defensive list handling protect the original even for mutable `ArrayList` inputs.
3. Wire the shared generator into every remaining record writer using the correct fully qualified nested record type and actual constructor components. Preserve AVSC constraints, default initialization, polymorphic interfaces, and snapshot structure; handle name/signature conflicts with actionable diagnostics.
4. Simplify verified copy-and-modify sites without changing behavior, including existing request `withX` helpers only where equivalent; document any sites intentionally left unchanged.
5. Add focused `BuilderWriterTest`, REST generator, AVSC event/union and snapshot generator tests that compile generated sources and exercise runtime round trips, overrides, validation, defaults, prefix configuration, list isolation and edge cases. Run relevant module tests followed by the full Maven suite.
6. Document the generated API and examples in `backlog/docs/generated-artefacts.md` and the relevant developer guide, including shallow references, list behavior, and the limitation for handwritten records.
<!-- SECTION:PLAN:END -->
