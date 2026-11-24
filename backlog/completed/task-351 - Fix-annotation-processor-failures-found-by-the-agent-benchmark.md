---
id: TASK-351
title: Fix annotation processor failures found by the agent benchmark
status: Done
assignee: []
created_date: '2026-11-21 09:45'
labels:
  - annotation-processor
  - bug
  - agents
dependencies: []
priority: high
type: bug
ordinal: 196000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
While building the TASK-343 benchmark, three ordinary models made the annotation processor generate code that does not compile, or crash. An agent cannot work around these without reading Prefab's sources, so each one shows up as an unexplained benchmark failure.

1. **Duplicate `@Valid`.** `@Create`/`@Update` parameters of a record type annotated with `@Valid` (natural for nested value objects) produce a request record with `@Valid` twice: `jakarta.validation.Valid is not a repeatable annotation interface`. Benchmark task `rest-value-object`.
2. **`@Filter` on a primitive `boolean`** crashes the processor with `IllegalArgumentException: not a valid name: boolean` (seen on MongoDB). A boxed `Boolean` works. Benchmark task `mongodb-crud`.
3. **AVSC namespace different from the Java package.** With `@Avsc` on a contract in `com.example.shipping` and a schema in namespace `com.example.logistics`, the record is generated in `com.example.shipping`, but `ShipmentDispatchedEventTypeRegistrar` is generated in `com.example.logistics.infrastructure.event` and imports `com.example.logistics.ShipmentDispatched`, which does not exist. The docs explicitly promise that a differing namespace works. Benchmark task `avro-avsc-contract`.
4. **A wrong import crashes the processor instead of producing a compile error.** With
   `import be.appify.prefab.core.annotations.Security;` (the class lives in `...annotations.rest`),
   `@GetById(security = @Security(enabled = false))` makes the processor fail with
   `IncompleteAnnotationException: be.appify.prefab.core.annotations.rest.Security missing element authority`, which
   hides javac's "cannot find symbol". Haiku concluded that `@Security` is broken and overrode the generated controller
   (benchmark task `security-public-endpoints`, Haiku with full docs, 2026-11-22). Annotation values with error types
   must be skipped so javac reports the real error.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Annotating a record-typed @Create/@Update parameter with @Valid compiles and validates the nested record
- [x] #2 @Filter on primitive boolean (and other primitives) compiles on PostgreSQL and MongoDB, or gives a clear compile error
- [x] #3 An @Avsc schema whose namespace differs from the contract's Java package compiles and publishes/consumes correctly
- [x] #4 Each case is covered by an annotation-processor test
- [x] #6 An unresolvable annotation value (e.g. a wrong import) leads to javac's own compile error, not a processor crash
- [x] #5 The benchmark tasks rest-value-object, mongodb-crud and avro-avsc-contract are updated to use the natural form again (see their task.yml notes) and still validate
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Root causes and fixes:

1. **Duplicate `@Valid`**: `RequestParameterBuilder.defaultParameters` copied the parameter's own annotations and then
   added `@Valid` for non-standard types. It now skips the added `@Valid` when the parameter already has one.
2. **Primitive `@Filter`**: the filter parameter type was built with `ClassName.get(package, simpleName)`, which has no
   valid name for `boolean`. `GetListUtil.declaredTypeName` boxes the type first; `GetListServiceWriter.filterParamType`
   (used for `Optional<T>` in the generated query) now boxes too. Postgres and MongoDB share this code, so one fix
   covers both. Verified for `boolean` and `int`.
3. **AVSC namespace**: `KafkaPlugin.writeAvscRegistrarsForElement` used the schema namespace as the package of the
   registrar and of the event class. The record is generated next to the `@Avsc` contract, so both now use the contract's
   package. The Avro schema keeps its own namespace.
4. **Wrong import**: javac hands out annotation proxies for unresolved annotations that throw
   `IncompleteAnnotationException` when read. New `UnresolvedAnnotations` detects an unresolved annotation type or value
   (nested annotations, arrays, types). `ClassManifest.hasUnresolvedFields` now includes it, so such aggregates are
   deferred and never generated, and `PrefabProcessor.validateSecurityAnnotation` skips them. javac then reports its
   own "cannot find symbol".

Tests: `NestedValidParameterTest`, `PrimitiveFilterTest`, `UnresolvedAnnotationTest` (annotation-processor) and
`avscEventRegistrarUsesTheContractPackageWhenTheSchemaNamespaceDiffers` (kafka). Each fails without its fix.

Benchmark: `rest-value-object` and `mongodb-crud` references use the natural form again (`@Valid` on the parameters,
primitive `boolean`); `avro-avsc-contract` starter schema now has namespace `com.example.logistics` while the record
stays in `com.example.shipping`. The workaround notes were removed from their `task.yml`. The benchmark itself was not
re-run (needs an agent and Docker).

Docs: `backlog/docs/annotation-reference.md` (`@Filter`, `@Avsc`, nested `@Valid`).
<!-- SECTION:NOTES:END -->
