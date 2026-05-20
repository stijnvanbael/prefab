---
id: TASK-222
title: Implement Kafka Streams breakout operator via backend adapter interface
status: To Do
assignee: []
created_date: '2026-05-20 00:00'
updated_date: '2026-05-20 00:00'
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
- [ ] #1 Streams DSL exposes a breakout operator on `PrefabStream` that accepts a backend adapter abstraction rather than directly exposing Kafka classes in the core API
- [ ] #2 Kafka backend provides an adapter implementation that maps breakout logic to native Kafka Streams operations (`KStream` fragment in, `KStream` fragment out)
- [ ] #3 Typed contracts preserve value-type safety across breakout boundaries and fail fast with actionable errors for unsupported backend usage
- [ ] #4 `examples/streams` includes a runnable breakout example showing insertion of a native Kafka Streams fragment inside an otherwise Prefab DSL pipeline
- [ ] #5 Topology-level tests in `streams` verify breakout behavior for happy path and invalid adapter/backend combinations
- [ ] #6 Developer guide docs are updated to describe breakout semantics, backend adapter extension points, and portability trade-offs
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Scope decision captured on 2026-05-20: ship Kafka Streams breakout first, but anchor the API on a backend adapter interface to avoid Kafka type leakage in `PrefabStream`.

Suggested implementation sequence:
1. Define backend-neutral breakout adapter SPI in `streams` core API.
2. Implement Kafka adapter in `streams/kafka` and wire `KafkaPrefabStream`.
3. Add example + tests.
4. Update `backlog/docs/feature-guides.md` and any relevant reference docs.

Out of scope for this task:
- Non-Kafka breakout implementations (Pub/Sub, SNS/SQS)
- Multi-native-type breakout support beyond `KStream -> KStream`
<!-- SECTION:NOTES:END -->

