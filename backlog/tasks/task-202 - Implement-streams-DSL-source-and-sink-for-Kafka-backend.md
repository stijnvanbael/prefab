---
id: TASK-202
title: Implement streams DSL source and sink for Kafka backend
status: Done
assignee: []
created_date: '2026-05-17 09:14'
updated_date: '2026-05-17 11:46'
labels:
  - feature
  - streams
  - kafka
milestone: m-1
dependencies:
  - TASK-201
references:
  - core/src/main/java/be/appify/prefab/core/kafka/KafkaConfiguration.java
  - core/src/main/java/be/appify/prefab/core/kafka/DynamicSerializer.java
  - core/src/main/java/be/appify/prefab/core/kafka/DynamicDeserializer.java
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
priority: high
ordinal: 20200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement initial Kafka-backed streams DSL flow for `from(...)` and `to(...)`, including topic resolution and serialization integration, in a dedicated Streams module (not as an extension inside `core/kafka`).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams Kafka DSL support is implemented in a separate Streams module and not by extending `core/kafka` directly
- [x] #2 Kafka backend supports `from(Class<?>)` and `to(Class<?>)` in the streams DSL
- [x] #3 Source and sink wiring reuse Prefab serialization infrastructure (`DynamicSerializer` and `DynamicDeserializer`)
- [x] #4 A sample topology in `examples/streams` reads from one topic and writes to another using only `from` and `to`
- [x] #5 Automated verification is primarily done with `TopologyTestDriver`, plus exactly one `@IntegrationTest` for end-to-end verification
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Implement Kafka-backed `from(...)` and `to(...)` wiring in the dedicated Streams module.
2. Reuse `DynamicDeserializer` for source ingestion and `DynamicSerializer` for sink emission.
3. Add/update `examples/streams` topology to demonstrate source->sink flow via topic resolution.
4. Add TopologyTestDriver-based tests as the primary automated coverage.
5. Add exactly one `@IntegrationTest` that verifies end-to-end behavior.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented a dedicated top-level `streams` Maven module (`prefab-streams`) and wired it into root reactor + dependencyManagement.

Added Kafka-backed baseline DSL APIs: `PrefabStreams#from(Class<?>)` and `PrefabStream#to(Class<?>)` / `PrefabStream#to(String)` with concrete Kafka implementations.

Reused `DynamicDeserializer` and `DynamicSerializer` via a lightweight `SerdeAdapter` for both source and sink wiring.

Extended `KafkaJsonTypeResolver` with `topicForType(Class<?>)` fail-fast semantics: throws clear exceptions for no topic and multiple topic registrations.

Added TopologyTestDriver-first verification in `streams` module (`KafkaPrefabStreamsTopologyTest`) covering from->to(Class), from->to(String), and fail-fast resolution behavior.

Updated `examples/streams` with a runnable topology (`from(StreamEvent.class).to(outputTopic)`) and added exactly one `@IntegrationTest` that verifies end-to-end input-topic to output-topic flow.

Updated docs in `backlog/docs/modules.md` and `backlog/docs/feature-guides.md` to document the new streams module and source/sink DSL baseline.
<!-- SECTION:NOTES:END -->
