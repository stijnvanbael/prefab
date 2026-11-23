---
id: TASK-207
title: Implement groupBy and aggregate operators in streams DSL
status: Done
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-21 06:39'
labels:
  - feature
  - streams
  - kafka
  - aggregate
milestone: m-3
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 7.2479248046875
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add non-windowed grouping and aggregation operators for Kafka-backed streams DSL pipelines, with tests and example usage.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams DSL supports `groupBy` followed by non-windowed `aggregate`
- [x] #2 Kafka backend maps grouped aggregate semantics to native Kafka Streams grouped stateful operations
- [x] #3 `examples/streams` includes a runnable non-windowed aggregate pipeline
- [x] #4 Tests verify aggregate state evolution for multiple events on the same key
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Execution sequencing note: complete TASK-207 before TASK-214 and TASK-216 to establish stable grouped-state semantics.

This task defines baseline grouping and aggregate contracts consumed by repartition naming and custom process/store wiring.

Implemented independently of TASK-206 (no table abstraction needed; the aggregate `KTable` is converted with `toStream()`).
- `PrefabStream.groupBy(Class<KO>, Function<V,KO>)` returns the new `PrefabGroupedStream`, whose `aggregate(Class<VR>, Supplier<VR>, BiFunction)` emits the updated aggregate per record.
- Kafka: `KGroupedStream.aggregate` with named `Grouped`/`Materialized` (store `aggregate-<in>-<out>`, repartition `group-by-<in>-repartition`); serdes via `joinSerde` so internal topics are registered with the `EventRegistry`.
- Tests: `KafkaPrefabStreamsTopologyTest` (state evolution, topology shape); example: `examples/streams` `sales` package + README, `SalesTotalsTopologyTest`.
- The existing list-based `PrefabStream.aggregate` is untouched.

### Analysis (independent of TASK-206)

**Finding: TASK-207 has no technical dependency on TASK-206.** TASK-206 (KStream-KTable join) would introduce a table abstraction (`PrefabTable`), but `groupBy().aggregate()` can be delivered without one by materialising the `KTable` and immediately calling `toStream()`, returning a `PrefabStream` (changelog semantics: one emission per update). The `TASK-206` dependency can be dropped; if 206 lands later, a `toTable()`/`aggregateToTable` variant can be added additively.

**Current state**
- `PrefabStream.aggregate(groupBy, Function<List<V>,VO>, egressCondition)` already exists as a *default* method built on `AggregationProcessor` (Processor API + custom store; keeps all values in a list, re-aggregates each time). It is not native Kafka Streams grouped stateful op and has no `groupBy` step -> doesn't satisfy AC #1/#2.
- `KafkaPrefabStream` wraps `KStream<K,V>`; serdes are `SerdeAdapter` over `DynamicSerializer/Deserializer` (topic-based; internal topics registered via `registerInternalTopic`, as done for `join`). Step names come from `StreamStepNames`.

**Proposed API** (new, additive; keep existing `aggregate` untouched to avoid breaking the example)
```java
<KO> PrefabGroupedStream<KO, V> groupBy(Function<V, KO> keyExtractor);   // + overload with Class<KO>

interface PrefabGroupedStream<KG, V extends Keyed<?>> {
    <VR extends Keyed<KG>> PrefabStream<KG, VR> aggregate(
        Supplier<VR> initializer, BiFunction<V, VR, VR> aggregator);       // non-windowed
}
```
Constraint: `PrefabStream<K, V extends Keyed<K>>` requires `V` keyed by `K`; the result is keyed by the group key `KG`, so `VR extends Keyed<KG>` (consistent with existing `process`/`breakout`).

**Kafka mapping** (`KafkaPrefabStreamGrouped`, package `streams.kafka`)
1. `stream.groupBy((k,v) -> keyExtractor.apply(v), Grouped.with(keySerdeOut, valueSerde(valueType)).withName(stepNames.nextGroupByName(type)))` -> repartition topic only when key changes (native).
2. `.aggregate(initializer, (k,v,agg) -> aggregator.apply(v,agg), Named, Materialized.with(keySerdeOut, joinSerde(outType)).as(storeName))` -> native `KGroupedStream.aggregate`.
3. `.toStream()` -> `wrap(...)` into `KafkaPrefabStream` with unknown value type.
- Serde: reuse `joinSerde(...)` pattern so repartition and changelog topics are registered with `EventRegistry`; the output type is unknown at the call site (lambda erasure) -> either require `Class<VR>` in `aggregate` (recommended, also gives readable store names, resolving the limitation noted in `AggregationProcessor.deriveStoreType`) or register lazily from first value.
- Key serde for new key type: `DeferredStringKeySerde`/`DeferredJsonKeySerde` exist; pick by key class (prefer explicit `Class<KO>` in `groupBy`, like task-099 sketch `groupBy(OrderId.class, ...)`).
- Add `StreamStepNames.nextGroupByName/nextAggregateName/storeName` for deterministic names (needed by TASK-214 repartition naming).

**Tests** (extend `KafkaPrefabStreamsTopologyTest`, TopologyTestDriver): same key multiple events -> state evolves (e.g. 1, 3, 6); different keys isolated; re-keying via groupBy emits to new key; topology contains `KSTREAM-AGGREGATE`/named state store (verifies native mapping, AC #2).

**Example**: add a non-windowed running-total pipeline in `examples/streams` (e.g. total production per `EnergySource`), with a test in the example module.

**Risks/open questions**
- Default `aggregate` name collision with the new grouped `aggregate` is avoided because the latter lives on `PrefabGroupedStream`.
- Decorators (`AutoRegisterPrefabStreamTestDecorator`) must delegate `groupBy`.
- Tombstones/null values: define behaviour (ignore).
- Decide whether to keep or deprecate the list-based `aggregate` (out of scope; separate task).
<!-- SECTION:NOTES:END -->
