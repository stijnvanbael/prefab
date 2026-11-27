---
id: TASK-368
title: Write generated test clients for aggregates that publish AVSC-generated events to prefab-test-sources
status: Done
assignee: []
created_date: '2026-11-23 15:30'
labels:
  - annotation-processor
  - avro
  - testing
dependencies: []
priority: medium
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while building the showcase Avro slice (TASK-337.07).

An aggregate that publishes an event generated from an `.avsc` file (`@Avsc`) references a type that does not exist in the first processing round, so the aggregate is processed in a later round. `TestJavaFileWriter.getRootPath()` then cannot derive the project root: the preferred element is found, but its source path is resolved through the filer in a round where only generated sources are root elements, and the candidates yield nothing. The writer falls back to `writeToFiler(...)` and writes the generated test client (e.g. `ShipmentClient.java`) into `target/classes/<package>/` instead of `target/prefab-test-sources/<package>/`. That folder is not a test source root, so the client cannot be injected in tests (`cannot find symbol: class ShipmentClient`) and a stray `.java` file ends up on the classpath.

Reproduce: `examples/showcase/src/avro/java/.../shipping/Shipment.java` (`@Update dispatch` publishes the generated `ShipmentDispatched`); the other aggregates of the same module get their clients in `target/prefab-test-sources`. The benchmark task `avro-avsc-contract` avoids test clients for the same reason.

Fix the root path lookup so it is stable across rounds (e.g. remember the root once resolved, or resolve it from any root element of the first round), and add a processor test with an AVSC-first aggregate that asserts where the test client is written. Then switch `ShipmentIntegrationTest` in the showcase back to the generated `ShipmentClient`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The test client of an aggregate that publishes an AVSC-generated event is written to `target/prefab-test-sources`
- [x] #2 A processor test covers the case
- [x] #3 `ShipmentIntegrationTest` uses the generated `ShipmentClient`
<!-- AC:END -->

## Implementation Notes

- Added an AVSC processor regression test covering an aggregate that references an AVSC-generated event and asserting that its client is written to `target/prefab-test-sources`, not `target/classes`.
- Updated `ShipmentIntegrationTest` to use the generated `ShipmentClient` for shipment lookup and dispatch.
- The existing incremental root caching in `IncrementalBuildState` preserves the project root across deferred processing rounds, which makes the regression pass.
- Validation: `AvscIncrementalCompilationTest` and showcase `test-compile` pass.
