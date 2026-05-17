---
id: TASK-204
title: Implement branch and merge operators in streams DSL
status: Done
assignee: []
created_date: '2026-05-17 09:15'
updated_date: '2026-05-17 18:28'
labels:
  - feature
  - streams
  - kafka
milestone: m-1
dependencies:
  - TASK-203
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 20400
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add DSL support for stream branching and fan-in merging on the Kafka backend, including example pipelines and tests.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams DSL exposes `branch` and `merge` operators for Kafka pipelines
- [x] #2 Kafka backend maps branching and merging behavior to native Kafka Streams topology operations
- [x] #3 `examples/streams` includes a runnable branch-and-merge scenario with multiple output paths
- [x] #4 Tests validate records are routed to expected branches and merged output preserves expected content
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Started implementation for branch and merge operators in streams DSL.

Implemented `branch(Predicate<V>...)` and `merge(PrefabStream<V>)` in streams DSL and Kafka backend (`KafkaPrefabStream`) using native Kafka Streams split/branch and merge operations.

Added topology tests in `KafkaPrefabStreamsTopologyTest` for branch routing and fan-in merge behavior, plus updated example integration test coverage for short/long branch topics and merged output topic.

Extended `examples/streams` with `ShortWordEvent` and `LongWordEvent`, updated topology and topic config, and documented branch-and-merge usage in `examples/streams/README.md` and `backlog/docs/feature-guides.md`.

Verified with `mvn -pl streams test` and `mvn -pl examples/streams -am test` (both passing).
<!-- SECTION:NOTES:END -->
