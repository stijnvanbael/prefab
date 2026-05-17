---
id: TASK-220
title: Redesign streams DSL branch and merge typing
status: Done
assignee: []
created_date: '2026-05-17 19:28'
updated_date: '2026-05-17 19:34'
labels:
  - feature
  - streams
  - kafka
milestone: m-1
dependencies:
  - TASK-204
references:
  - streams/src/main/java/be/appify/prefab/streams/PrefabStream.java
  - streams/src/main/java/be/appify/prefab/streams/kafka/KafkaPrefabStream.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Redesign the streams DSL so branch produces one stream per call, add a branch overload that branches by subtype class with filter+cast semantics, and allow merge to return a stream typed to a common supertype.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `PrefabStream` exposes a single-predicate `branch` operator (no varargs/list return)
- [x] #2 `PrefabStream` exposes a `branch(Class<S>)` overload that filters by subtype and returns a cast stream of `S`
- [x] #3 `merge` supports combining sibling streams and returning a stream typed to a declared common supertype
- [x] #4 Kafka backend maps redesigned branch and merge operators to native Kafka Streams operations with safe casts
- [x] #5 Unit/topology tests cover predicate branch, subtype branch, and supertype merge behavior
- [x] #6 Streams DSL docs and example pipeline are updated to use the redesigned API
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Started redesign implementation for branch/merge typing API updates.

Redesigned `PrefabStream` branching API to single-branch operations: `branch(Predicate<V>)` and `branch(Class<S>)` (subtype filter+cast).

Generalized merge typing to `<S> PrefabStream<S> merge(PrefabStream<? extends S> other)` so sibling subtype streams can merge into a declared supertype.

Updated Kafka implementation to map new branch/merge signatures to native Kafka Streams split/filter/cast/merge operations and propagate runtime type metadata for safer compatibility checks.

Reworked topology tests to validate predicate branching, subtype branch filtering/casting, and supertype merge behavior; updated streams example and documentation accordingly.

Validated with `mvn -pl streams test` and `mvn -pl examples/streams -am test`.
<!-- SECTION:NOTES:END -->
