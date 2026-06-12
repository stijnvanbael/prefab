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
Extended `StreamStepNames` with one `AtomicInteger` counter per operator type
(`filter`, `map`, `flat-map`, `branch-subtype`, `merge`, `join`, `process`).
Each counter starts at 0 and increments on every `next*Name()` call within the same
topology context, guaranteeing both uniqueness within a topology and stability across
independent builds of the same DSL structure.

Applied `Named.as(stepNames.next*Name())` to every operator in `KafkaPrefabStream`:
- `filter` / `map` / `flatMap` — directly on the underlying KStream method.
- `branch(Class<S>)` — the internal filter step uses `branch-subtype-N` and the cast step
  `branch-subtype-N-cast`, keeping the pair visually grouped in the topology description.
- `merge` — via `Named.as(...)` on `KStream.merge`.
- `join` — via `StreamJoined.with(...).withName(joinName)` (the `NamedOperation` API).
- `process` — via the `Named`-accepting overload of `KStream.process`.

`breakout` was intentionally left unnamed because it delegates entirely to user-supplied
native Kafka code; imposing a name there would conflict with the caller's own naming.

Added 14 new tests (stability + uniqueness) covering every operator; all 34 tests pass.
Updated `backlog/docs/feature-guides.md` with a reference table of all name patterns.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
All DSL stream operators now produce stable, representative Kafka Streams processor names
(`filter-1`, `map-1`, `flat-map-1`, `branch-subtype-1`, `merge-1`, `join-1`, `process-1`).
34 tests pass; developer guide updated with a full naming-pattern reference table.
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

