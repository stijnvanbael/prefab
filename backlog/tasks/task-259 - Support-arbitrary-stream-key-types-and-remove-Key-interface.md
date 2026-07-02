---
id: TASK-259
title: Support arbitrary stream key types and remove Key interface
status: Done
assignee: []
created_date: '2026-07-01 13:44'
updated_date: '2026-07-02 06:52'
labels:
  - streams
  - core
  - serialization
  - breaking-change
dependencies: []
references:
  - core/src/main/java/be/appify/prefab/core/domain/Key.java
  - streams/src/main/java/be/appify/prefab/streams/kafka/StringKeySerde.java
  - >-
    streams/src/main/java/be/appify/prefab/streams/kafka/DeferredStringKeySerde.java
  - streams/src/main/java/be/appify/prefab/streams/kafka/KafkaPrefabStreams.java
  - streams/src/main/java/be/appify/prefab/streams/kafka/KafkaPrefabStream.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Enable Prefab Streams to work with any key type by removing the `be.appify.prefab.core.domain.Key` contract from public APIs and Kafka Streams internals. Key serialization must default to plain String for single-value records and JSON for complex record keys. This task includes API refactoring, serializer/deserializer redesign, regression tests, and migration documentation for existing topologies/state stores.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 All streams/core public APIs currently constrained by `K extends Key<K>` are migrated to unconstrained `K`, and the project compiles for affected modules.
- [x] #2 `be.appify.prefab.core.domain.Key` is removed with no remaining imports/usages in `core`, `streams`, and `examples/streams`.
- [x] #3 Single-value record keys are serialized/deserialized as plain UTF-8 String values by default (no JSON wrapper).
- [x] #4 Complex record keys (multiple components) are serialized/deserialized as JSON by default and preserve equality on round-trip.
- [x] #5 Kafka streams topology behavior (`from`/`to`, joins, and state-store processing) is validated by tests for both single-value and complex key records.
- [x] #6 Developer documentation in `backlog/docs/feature-guides.md` (and related guides if needed) documents the new default key serialization behavior and migration/compatibility implications.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Refactor public stream/core generics to remove `K extends Key<K>` bounds while preserving `Keyed<K>` contracts where appropriate.
2. Replace `StringKeySerde`/`DeferredStringKeySerde` parsing logic with a key codec strategy that supports:
   - single-field records as plain UTF-8 strings
   - multi-field records as JSON
3. Keep key-type inference in `KafkaPrefabStreams.keyTypeOf(...)` working with generic bridges and inheritance.
4. Thread the new key serde/codec through `from`, `to`, `join`, merge/process wrappers, and state-store creation.
5. Update test fixtures and topology tests to cover both key categories and state-store round trips.
6. Update streams feature documentation with serialization defaults and migration guidance for existing changelog/state data.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Initial analysis completed:
- `Key` is currently a hard dependency in streams public API (`PrefabStream`, `PrefabStreams`, `StreamBreakoutAdapter`, `StreamProcessor`) and Kafka implementation (`KafkaPrefabStream`, `KafkaPrefabStreams`, `StringKeySerde`, `DeferredStringKeySerde`).
- Current key deserialization relies on global registry + `Key.parse`, so removing `Key` requires replacing registry-based deserialization with type-aware codecs.
- Existing key examples include both single-field (`BiddingZoneId`, `Reference<T>`) and composite records (`ProductionKey`), which aligns with requested serialization split.
- `keyTypeOf(...)` generic-resolution tests (`KeyTypeResolutionTest`) are a critical guardrail and should be kept/extended through the refactor.
- This change is likely behavior-breaking for persisted Kafka store/changelog key bytes when moving composite keys from `toString()` to JSON; migration notes are required.

Implemented in small, test-backed increments with hard-break semantics (no compatibility mode).

Replaced Key.parse/register-based key handling with strategy-based serdes: single-field records serialize as plain UTF-8 strings; composite/non-record keys serialize as JSON (`StringKeySerde`, `DeferredStringKeySerde`).

Removed the `Key` contract from public stream/core APIs by relaxing `Keyed<K>` and all stream DSL/adapter/processor/store generics to unconstrained `K`.

Deleted `core/src/main/java/be/appify/prefab/core/domain/Key.java` and removed remaining usages/imports in `core`, `streams`, and `examples/streams` (including `Reference`, `BiddingZoneId`, `ProductionKey`).

Updated Kafka streams internals to carry key serdes through pipeline wrappers (`KafkaPrefabStream`) and to use strategy-based key serde in source/sink/join/store paths.

Extended tests to validate topology behavior for both key categories, including `from`/`to` byte-level key assertions (plain string vs JSON), composite-key joins, and composite-key state-store processing (`KafkaPrefabStreamsTopologyTest`, `StringKeySerdeTest`).

Updated developer documentation (`backlog/docs/feature-guides.md`, Streams DSL section) with new key-serialization defaults and explicit migration warning for persisted state/changelog bytes.

Verification: `mvn -pl streams -am test -DskipITs -Dtest=KafkaPrefabStreamsTopologyTest,StringKeySerdeTest,KeyTypeResolutionTest -Dsurefire.failIfNoSpecifiedTests=false`; `mvn -pl streams,examples/streams -am test -DskipITs -Dtest=KafkaPrefabStreamsTopologyTest,StringKeySerdeTest,KeyTypeResolutionTest -Dsurefire.failIfNoSpecifiedTests=false`.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented TASK-259 as a hard-break migration to arbitrary stream key types.

What changed:
- Removed `Key` contract dependency from stream/core public APIs:
  - `Keyed<K>` is now unconstrained.
  - Stream DSL interfaces and implementations (`PrefabStream`, `PrefabStreams`, `StreamProcessor`, `StreamBreakoutAdapter`, `StatefulStreamProcessor`, `Aggregation`, `AggregationProcessor`) now use unconstrained key generics.
- Removed `be.appify.prefab.core.domain.Key` entirely:
  - Deleted `core/src/main/java/be/appify/prefab/core/domain/Key.java`.
  - Removed `Key.parse`/`Key.register` usage from `Reference`.
  - Removed `implements Key<...>` from example stream key records.
- Reworked Kafka key serialization defaults:
  - `StringKeySerde` now selects strategy by key shape.
  - Single-field record keys: plain UTF-8 string bytes.
  - Composite record keys: JSON bytes.
  - Non-record keys: JSON bytes.
  - `DeferredStringKeySerde` now resolves runtime type and delegates to the same strategy.
- Propagated key serde through Kafka stream internals:
  - `KafkaPrefabStream` now carries `Serde<K>` instead of `Class<K>`.
  - Source/sink/join/store paths use strategy-based key serdes consistently.
- Extended regression coverage:
  - Added `StringKeySerdeTest` for plain-string vs JSON behavior and deferred serde behavior.
  - Updated `KeyTypeResolutionTest` to work without `Key` bound.
  - Extended `KafkaPrefabStreamsTopologyTest` with:
    - raw key-byte assertions for single-field and composite keys,
    - composite-key join behavior,
    - composite-key stateful processor/store behavior.

Docs:
- Updated `backlog/docs/feature-guides.md` (Streams DSL section) to document serialization defaults and migration implications for persisted state/changelog keys.

Validation:
- `mvn -pl streams -am test -DskipITs -Dtest=KafkaPrefabStreamsTopologyTest,StringKeySerdeTest,KeyTypeResolutionTest -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -pl streams,examples/streams -am test -DskipITs -Dtest=KafkaPrefabStreamsTopologyTest,StringKeySerdeTest,KeyTypeResolutionTest -Dsurefire.failIfNoSpecifiedTests=false`
Both commands completed successfully.
<!-- SECTION:FINAL_SUMMARY:END -->
