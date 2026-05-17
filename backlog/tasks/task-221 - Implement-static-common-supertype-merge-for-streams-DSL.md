---
id: TASK-221
title: Implement static common-supertype merge for streams DSL
status: Done
assignee: []
created_date: '2026-05-17 20:08'
updated_date: '2026-05-17 20:14'
labels:
  - feature
  - streams
  - kafka
milestone: m-1
dependencies:
  - TASK-220
references:
  - streams/src/main/java/be/appify/prefab/streams/PrefabStream.java
  - streams/src/main/java/be/appify/prefab/streams/PrefabStreams.java
  - streams/src/main/java/be/appify/prefab/streams/kafka/KafkaPrefabStream.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Refine the streams DSL merge API so common-supertype merging is expressed safely. Keep or simplify instance merge as needed, and add a static/factory merge API that can merge two streams into a declared common supertype at compile time.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams DSL exposes a compile-time safe common-supertype merge API
- [x] #2 Instance merge API no longer implies unsupported supertype guarantees
- [x] #3 Kafka backend supports the revised merge API without regressing existing branch behavior
- [x] #4 Topology tests cover static/factory merge of sibling subtype streams into a shared supertype
- [x] #5 Example and streams documentation are updated to use the revised merge API where appropriate
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Started implementation to replace instance common-supertype merge with a compile-time safe static/factory merge API.

Narrowed instance `merge` to `PrefabStream<V> merge(PrefabStream<? extends V> other)` so instance chaining no longer implies unsupported widening to arbitrary supertypes.

Added compile-time safe factory merge on `PrefabStreams`: `<M> PrefabStream<M> merge(PrefabStream<? extends M> left, PrefabStream<? extends M> right)`, implemented by the Kafka backend through shared internal merge handling.

Updated topology tests to cover same-type instance merge and sibling subtype merge widened through `streams.merge(...)`, and refreshed the streams example plus documentation to demonstrate the new API split.

Verified with `mvn -pl streams test` and `mvn -pl examples/streams -am test`.
<!-- SECTION:NOTES:END -->
