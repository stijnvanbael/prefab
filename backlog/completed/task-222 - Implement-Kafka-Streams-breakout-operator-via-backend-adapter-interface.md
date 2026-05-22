---
id: TASK-222
title: Implement Kafka Streams breakout operator via backend adapter interface
status: Done
assignee: []
created_date: '2026-05-20 00:00'
updated_date: '2026-05-20 13:16'
labels:
  - feature
  - streams
  - kafka
  - api
dependencies:
  - TASK-203
  - TASK-218
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - streams/src/main/java/be/appify/prefab/streams/PrefabStream.java
  - streams/src/main/java/be/appify/prefab/streams/kafka/KafkaPrefabStream.java
  - examples/streams
priority: high
ordinal: 22200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a backend-adapter breakout operator to the streams DSL so users can inject native Kafka Streams topology fragments into a `PrefabStream` while keeping the DSL extensible for future backends.

Initial delivery targets Kafka Streams only. The API shape must isolate backend-native types behind a backend adapter contract so non-Kafka implementations can add equivalent breakout support without changing core DSL signatures.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams DSL exposes a breakout operator on `PrefabStream` that accepts a backend adapter abstraction rather than directly exposing Kafka classes in the core API
- [x] #2 Kafka backend provides an adapter implementation that maps breakout logic to native Kafka Streams operations (`KStream` fragment in, `KStream` fragment out)
- [x] #3 Typed contracts preserve value-type safety across breakout boundaries and fail fast with actionable errors for unsupported backend usage
- [x] #4 `examples/streams` includes a runnable breakout example showing insertion of a native Kafka Streams fragment inside an otherwise Prefab DSL pipeline
- [x] #5 Topology-level tests in `streams` verify breakout behavior for happy path and invalid adapter/backend combinations
- [x] #6 Developer guide docs are updated to describe breakout semantics, backend adapter extension points, and portability trade-offs
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented backend-neutral breakout SPI in core streams API:
- Added `StreamBreakoutAdapter<V, R, NATIVE_IN, NATIVE_OUT>` and `StreamBackend`.
- Extended `PrefabStream` with `breakout(...)` accepting only adapter abstraction in core API.

Implemented Kafka-first adapter path:
- Added `KafkaStreamBreakoutAdapter<V, R>` for `KStream<String, V> -> KStream<String, R>` fragments.
- Wired `KafkaPrefabStream#breakout(...)` with fail-fast backend/type validation and actionable error messages.

Added validation coverage:
- `KafkaPrefabStreamsTopologyTest` now verifies breakout happy path, unsupported backend rejection, and invalid return type rejection.

Added runnable example + docs:
- Updated `examples/streams` topology to inject a native `selectKey` fragment via breakout adapter.
- Updated `examples/streams/README.md` and `backlog/docs/feature-guides.md` with breakout semantics and portability guidance.

Verification:
- `mvn -pl streams,examples/streams -am test` (BUILD SUCCESS)
<!-- SECTION:NOTES:END -->

