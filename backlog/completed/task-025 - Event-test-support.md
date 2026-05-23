---
id: task-025
title: Event test support
status: Done
assignee: []
created_date: '2025-10-10 13:38'
updated_date: '2026-05-21 10:30'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 312.5
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Fixtures for producing and consuming events
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams topology tests can bootstrap Prefab Kafka serde and type registration with a single test helper
- [x] #2 Topology tests can read/write test records without creating `TopologyTestDriver` and Kafka serdes manually
- [x] #3 Existing `KafkaPrefabStreamsTopologyTest` uses the helper to keep tests minimal
<!-- AC:END -->

## Implementation Notes
<!-- SECTION:NOTES:BEGIN -->
Added `KafkaTopologyTestBootstrap` in `streams/src/test/java/be/appify/prefab/streams/kafka/`.

The helper now bootstraps:
- `SerializationRegistry`
- `EventRegistry`
- `DynamicSerializer` / `DynamicDeserializer`
- `TopologyTestDriver`

It exposes minimal test ergonomics:
- `registerJson(topic, type)` for combined topic+serialization registration
- `streams(...)` to create `KafkaPrefabStreams`
- `run(...)` to get a typed test session with `input(...)`, `output(...)`, and `rawOutput(...)`

Refactored `KafkaPrefabStreamsTopologyTest` to use the helper across all scenarios, removing local fixture and
streams config boilerplate from each test.
<!-- SECTION:NOTES:END -->

