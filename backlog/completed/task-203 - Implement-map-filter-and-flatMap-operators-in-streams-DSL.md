---
id: TASK-203
title: Implement map filter and flatMap operators in streams DSL
status: Done
assignee: []
created_date: '2026-05-17 09:14'
updated_date: '2026-05-17 15:04'
labels:
  - feature
  - streams
  - kafka
milestone: m-1
dependencies:
  - TASK-202
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 20300
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add Kafka-backed stateless transformation operators `map`, `filter`, and `flatMap` to the streams DSL and demonstrate them in the standalone streams example.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams DSL exposes `map`, `filter`, and `flatMap` operators for Kafka pipelines
- [x] #2 Kafka backend maps these operators to native KStream operations with correct type transitions
- [x] #3 `examples/streams` contains at least one runnable pipeline using all three operators
- [x] #4 Topology tests verify operator behavior for pass-through, filtering, and one-to-many mapping scenarios
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Implementation Summary

### DSL changes (`streams` module)
- `PrefabStream` made generic (`PrefabStream<V>`) with three new methods:
  - `filter(Predicate<V>)` → returns `PrefabStream<V>`
  - `<R> map(Function<V, R>)` → returns `PrefabStream<R>`
  - `<R> flatMap(Function<V, Iterable<R>>)` → returns `PrefabStream<R>`
- `PrefabStreams.from(Class<V>)` now returns `PrefabStream<V>` instead of raw `PrefabStream`

### Kafka backend (`KafkaPrefabStream<V>`, `KafkaPrefabStreams`)
- `KafkaPrefabStream<V>` is now generic; delegates to native KStream ops:
  - `filter` → `KStream.filter((k, v) -> predicate.test(v))`
  - `map` → `KStream.mapValues(mapper::apply)`
  - `flatMap` → `KStream.flatMapValues(mapper::apply)`
- Private `wrap(KStream<String, R>)` helper keeps all operators DRY
- `to(String)` uses `@SuppressWarnings("unchecked")` cast `Serde<V>` because `DynamicSerializer`/`DynamicDeserializer` operate on `Object` at runtime (safe: serde selects format by topic name, not generic type)
- `KafkaPrefabStreams.from()` casts `KStream<String, Object>` → `KStream<String, V>` (same rationale)

### Tests added (`KafkaPrefabStreamsTopologyTest`)
- `filter_shouldDropRecordsNotMatchingPredicate` — sends 3 records, 2 match, asserts 2 outputs
- `map_shouldTransformValues` — maps `IncomingOrder` to `ProcessedOrder` with uppercased customer name
- `flatMap_shouldExpandOneRecordToMany` — splits CSV payload into 3 individual word records

### Example updated (`examples/streams`)
- New `WordEvent` record annotated with `@Event(topic = "${topics.streams.words}")`
- `StreamTopologyConfiguration` replaced with a single `wordExtractionTopology` bean that chains all three operators: filter blank → map to uppercase → flatMap split on comma → to WordEvent topic
- `application.yml` extended with `topics.streams.words: streams.words`
- Integration test updated to assert uppercased HELLO/WORLD/FOO word records appear on the words topic
<!-- SECTION:NOTES:END -->
