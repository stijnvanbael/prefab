---
id: TASK-351
title: Fix annotation processor failures found by the agent benchmark
status: To Do
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
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Annotating a record-typed @Create/@Update parameter with @Valid compiles and validates the nested record
- [ ] #2 @Filter on primitive boolean (and other primitives) compiles on PostgreSQL and MongoDB, or gives a clear compile error
- [ ] #3 An @Avsc schema whose namespace differs from the contract's Java package compiles and publishes/consumes correctly
- [ ] #4 Each case is covered by an annotation-processor test
- [ ] #5 The benchmark tasks rest-value-object, mongodb-crud and avro-avsc-contract are updated to use the natural form again (see their task.yml notes) and still validate
<!-- AC:END -->
