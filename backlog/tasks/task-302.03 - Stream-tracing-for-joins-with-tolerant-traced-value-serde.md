---
id: TASK-302.03
title: Stream tracing for joins with tolerant traced value serde
status: Done
assignee: []
created_date: '2026-10-30 13:40'
labels:
  - streams
  - tracing
dependencies:
  - TASK-302.01
parent_task_id: TASK-302
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Slice 3 of TASK-302. Internal `TracedValue` and `TracedValueSerde` with the magic-prefixed envelope and a tolerant reader (always installed, envelope written only when tracing is enabled). `join` wraps both sides and merges origin sets. Includes migration tests (legacy bytes, JSON and Avro) and the topology-name stability test.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A joined message correlates to both contributing messages.
- [x] #2 Legacy bytes read as "no origins"; envelope bytes stay readable when tracing is disabled.
- [x] #3 Store and internal topic names are identical with tracing off and on.
- [x] #4 With tracing disabled the written payload bytes are unchanged.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
TracedValue(value, origins) and TracedValueSerde (envelope 0xFF 'P' 'T' v1, int length, origins, payload). writing() serde is used for join window stores when tracing is enabled; tolerant() is now always installed on internal topic serdes (joinSerde) so envelope bytes stay readable after disabling. Join wraps both sides (trace-wrap), merges origins with MessageTracing.union (capped, oldest evicted) and unwraps into the correlation header (trace-unwrap). Wrap/unwrap are explicitly named and the join keeps its name, so stores and application processors are identical with tracing on/off (MessageTracingJoinTopologyTest). Avro payload migration is covered by design (Confluent magic byte 0x00 never collides with 0xFF) but has no dedicated Avro test; JSON/String legacy bytes are tested in TracedValueSerdeTest.
<!-- SECTION:NOTES:END -->
