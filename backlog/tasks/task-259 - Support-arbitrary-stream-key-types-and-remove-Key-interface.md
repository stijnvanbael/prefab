---
id: TASK-259
title: Support arbitrary stream key types and remove Key interface
status: To Do
assignee: []
created_date: '2026-07-01 13:44'
updated_date: '2026-07-01 13:45'
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
- [ ] #1 All streams/core public APIs currently constrained by `K extends Key<K>` are migrated to unconstrained `K`, and the project compiles for affected modules.
- [ ] #2 `be.appify.prefab.core.domain.Key` is removed with no remaining imports/usages in `core`, `streams`, and `examples/streams`.
- [ ] #3 Single-value record keys are serialized/deserialized as plain UTF-8 String values by default (no JSON wrapper).
- [ ] #4 Complex record keys (multiple components) are serialized/deserialized as JSON by default and preserve equality on round-trip.
- [ ] #5 Kafka streams topology behavior (`from`/`to`, joins, and state-store processing) is validated by tests for both single-value and complex key records.
- [ ] #6 Developer documentation in `backlog/docs/feature-guides.md` (and related guides if needed) documents the new default key serialization behavior and migration/compatibility implications.
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
<!-- SECTION:NOTES:END -->
