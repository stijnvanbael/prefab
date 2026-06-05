---
id: TASK-247
title: Complete key generic propagation in streams module
status: Done
assignee: []
created_date: '2026-06-05 05:14'
updated_date: '2026-06-05 05:38'
labels:
  - streams
  - generics
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Finish the migration that adds explicit key generic parameters to Prefab Streams APIs and all usages so the project compiles cleanly.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 All Prefab Streams interfaces and implementations consistently use explicit key generic parameters.
- [x] #2 All call sites in the repository are updated to match the new generic signatures.
- [x] #3 The project compiles successfully for the affected modules.
- [x] #4 Relevant developer documentation is updated if public API signatures changed.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Propagated key-aware generics through the streams DSL and Kafka implementation so stream operations consistently carry `K extends Key<K>` and `V extends Keyed<K>`.

Key implementation updates:
- Updated test decorators (`AutoRegisterPrefabStreamsTestDecorator`, `AutoRegisterPrefabStreamTestDecorator`) to match new `PrefabStreams` and `PrefabStream` signatures.
- Fixed `StatefulStreamProcessor` store initialization to bridge `Class<?>` store declarations to the new `createStore(Class<VS extends Keyed<KS>>)` signature.
- Reworked Kafka key-type handling:
  - Added robust key type resolution from `Keyed<K>` interface in `KafkaPrefabStreams`.
  - Made `KafkaPrefabStream` carry explicit `Class<K>` key type through pipeline wrappers.
  - Updated state-store creation to use `StringKeySerde<K>` instead of plain `Serdes.String()`.
- Made breakout SPI key-aware:
  - `StreamBreakoutAdapter` now carries input/output key and keyed value types.
  - `PrefabStream.breakout(...)` now ties adapter input to current stream key/value types.
  - `KafkaStreamBreakoutAdapter` updated accordingly.
  - Updated topology tests and breakout adapters to use key-safe native stream fragments.
- Updated docs in `backlog/docs/feature-guides.md` (streams section) to reflect key-aware stream and breakout signatures.

Validation:
- Ran `mvn -pl streams -am test -DskipITs` successfully (core + streams reactor).
<!-- SECTION:NOTES:END -->
