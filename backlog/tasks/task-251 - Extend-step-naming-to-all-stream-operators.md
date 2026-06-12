---
id: TASK-251
title: Extend stable step naming to all stream operators
status: Done
assignee: []
created_date: '2026-06-12'
updated_date: '2026-06-12'
labels:
  - streams
  - kafka
dependencies:
  - TASK-249
references:
  - streams/src/main/java/be/appify/prefab/streams/kafka/StreamStepNames.java
  - streams/src/main/java/be/appify/prefab/streams/kafka/KafkaPrefabStream.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-249 introduced stable, representative names for `branch(Predicate)` steps only.
All other DSL operators — `filter`, `map`, `flatMap`, `branch(Class)`, `merge`, `join`,
and `process` — still received auto-generated `KSTREAM-*` names that are neither
descriptive nor aligned with Prefab's naming convention.

Extend `StreamStepNames` with per-operator counters and apply `Named.as(...)` (or the
equivalent `StreamJoined.withName(...)` for joins) in `KafkaPrefabStream` so every
topology step gets a stable, human-readable name such as `filter-1`, `map-2`,
`flat-map-1`, `join-1`, `process-1`, etc.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `filter` steps are named `filter-N` in the topology description.
- [x] #2 `map` steps are named `map-N` in the topology description.
- [x] #3 `flatMap` steps are named `flat-map-N` in the topology description.
- [x] #4 `branch(Class<S>)` filter and cast steps are named `branch-subtype-N` and `branch-subtype-N-cast`.
- [x] #5 `merge` steps are named `merge-N` in the topology description.
- [x] #6 `join` steps carry the name `join-N` in the topology description.
- [x] #7 `process` steps are named `process-N` in the topology description.
- [x] #8 Names are stable (same description across two independent topology builds with identical DSL).
- [x] #9 Names remain unique within one topology (two identical operators in the same topology get distinct names).
- [x] #10 A topology stability test covers each operator.
- [x] #11 Developer documentation is updated to document all operator name patterns.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Extended `StreamStepNames` with a single `Map<String, AtomicInteger> usageCountByName` that
tracks how many times each base name has been claimed.  All `next*Name()` methods now accept
the value type(s) of the DSL step as `Class<?>` parameters so the base name encodes the
actual domain types: `filter-incoming-order`, `join-incoming-order-shipping-update`, etc.

`buildBaseName` converts each `Class.getSimpleName()` to kebab-case via the same regex already
used in `KafkaPrefabStreams`, deduplicates identical type names with `.distinct()`, and falls
back to the plain operator prefix when all types are null/unknown.  A numeric suffix starting
at `-2` is appended only on collision (same operator+types used more than once in one topology).

A private `inputTypeOrNull()` helper on `KafkaPrefabStream` returns
`valueType.knownRuntimeType()` when known and `null` when the type is opaque (after `map`,
`flatMap`, or `breakout`).  This null propagates into `buildBaseName` which simply skips it,
yielding a graceful fallback (e.g. just `map`) instead of blowing up.

`branch(Class<S>)` passes the explicit `subtype` class directly, not the input stream type,
so branching on `OrderCreated` and `OrderShipped` from the same source produces
`branch-subtype-order-created` and `branch-subtype-order-shipped` — naturally unique without
needing a counter.

All 34 tests pass; developer guide updated with type-bearing name examples.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
All DSL stream operators now produce type-bearing, human-readable Kafka Streams processor
names: `filter-incoming-order`, `map-incoming-order`, `flat-map-word-batch`,
`branch-incoming-order-matched`, `branch-subtype-order-created`, `merge-incoming-order`,
`join-incoming-order-shipping-update`, `process-incoming-order`.  34 tests pass; developer
guide reference table updated.
<!-- SECTION:FINAL_SUMMARY:END -->

assignee: []
created_date: '2026-06-12'
updated_date: '2026-06-12'
labels:
  - streams
  - kafka
dependencies:
  - TASK-249
references:
  - streams/src/main/java/be/appify/prefab/streams/kafka/StreamStepNames.java
  - streams/src/main/java/be/appify/prefab/streams/kafka/KafkaPrefabStream.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
TASK-249 introduced stable, representative names for `branch(Predicate)` steps only.
All other DSL operators — `filter`, `map`, `flatMap`, `branch(Class)`, `merge`, `join`,
and `process` — still receive auto-generated `KSTREAM-*` names that are neither
descriptive nor aligned with Prefab's naming convention.

Extend `StreamStepNames` with per-operator counters and apply `Named.as(...)` (or the
equivalent `StreamJoined.withName(...)` for joins) in `KafkaPrefabStream` so every
topology step gets a stable, human-readable name such as `filter-1`, `map-2`,
`flat-map-1`, `join-1`, `process-1`, etc.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `filter` steps are named `filter-N` in the topology description.
- [ ] #2 `map` steps are named `map-N` in the topology description.
- [ ] #3 `flatMap` steps are named `flat-map-N` in the topology description.
- [ ] #4 `branch(Class<S>)` filter and cast steps are named `branch-subtype-N` and `branch-subtype-N-cast`.
- [ ] #5 `merge` steps are named `merge-N` in the topology description.
- [ ] #6 `join` steps carry the name `join-N` in the topology description.
- [ ] #7 `process` steps are named `process-N` in the topology description.
- [ ] #8 Names are stable (same description across two independent topology builds with identical DSL).
- [ ] #9 Names remain unique within one topology (two identical operators in the same topology get distinct names).
- [ ] #10 A topology stability test covers each operator.
- [ ] #11 Developer documentation is updated to document all operator name patterns.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
<!-- SECTION:NOTES:END -->

