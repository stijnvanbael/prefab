---
id: TASK-218
title: Introduce backend-agnostic StreamDefinition wrapper for streams DSL
status: Done
assignee: []
created_date: '2026-05-17 12:02'
updated_date: '2026-05-21 06:21'
labels:
  - feature
  - streams
  - kafka
  - api
milestone: m-1
dependencies:
  - TASK-202
references:
  - streams/src/main/java/be/appify/prefab/streams/PrefabStream.java
  - streams/src/main/java/be/appify/prefab/streams/kafka/KafkaPrefabStream.java
  - >-
    examples/streams/src/main/java/be/appify/prefab/example/streams/StreamTopologyConfiguration.java
priority: high
ordinal: 24200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace direct Kafka Topology return type in streams DSL terminals with a Prefab-owned wrapper so API remains backend-agnostic while still exposing topology metadata/access for runtime wiring and tests.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams DSL terminal methods return a Prefab-owned wrapper type instead of Kafka Topology directly
- [x] #2 Wrapper preserves access to underlying Kafka topology details needed by tests/runtime
- [x] #3 examples/streams topology bean returns the Prefab wrapper type
- [x] #4 Existing streams and examples tests pass after migration
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented a Prefab-owned streams wrapper type (`be.appify.prefab.streams.StreamDefinition`) and migrated terminal DSL APIs from `Topology` to this wrapper. Updated `PrefabStream` terminal methods to return `StreamDefinition`, updated Kafka backend implementation (`KafkaPrefabStream`) to return wrapper instances, and exposed Kafka native runtime/testing access via `StreamDefinition#nativeTopology()`. Migrated streams topology tests to assert against and drive `nativeTopology()`. Updated streams example topology bean method to return `StreamDefinition` instead of Kafka `Topology`. Verified with Maven reactor test run for `streams` and `examples/streams` modules including dependencies.
<!-- SECTION:NOTES:END -->
