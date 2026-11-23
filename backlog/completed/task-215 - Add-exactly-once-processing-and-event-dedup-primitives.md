---
id: TASK-215
title: Add exactly-once processing and event dedup primitives
status: Done
assignee: []
created_date: '2026-05-17 09:38'
updated_date: '2026-05-21 06:22'
labels:
  - feature
  - streams
  - kafka
  - reliability
milestone: m-6
dependencies:
  - TASK-202
  - TASK-207
  - TASK-211
references:
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
  - examples/streams
priority: high
ordinal: 167000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add DSL and runtime controls for Kafka exactly-once processing plus reusable event deduplication primitives for idempotent stream handling.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams configuration supports enabling Kafka Streams exactly-once processing mode where supported
- [x] #2 Streams DSL exposes a deduplication primitive based on event identity with configurable retention/window semantics
- [x] #3 Integration tests verify duplicates are suppressed while unique events are emitted
- [x] #4 Developer guide documents guarantees, prerequisites, and limitations for exactly-once and dedup behavior
<!-- AC:END -->

## Implementation Notes

Added `prefab.streams.processing-guarantee` (at_least_once | exactly_once_v2) in StreamsConfiguration; `PrefabStream.deduplicate(idExtractor, retention)` and `deduplicate(retention)` backed by DeduplicationProcessor and a persistent window store. Tests (TopologyTestDriver) in KafkaPrefabStreamsTopologyTest and StreamsConfigurationBootstrapTest. Documented in backlog/docs/feature-guides.md section 7.15.
