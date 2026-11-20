---
id: TASK-337.06
title: 'Showcase streams slice: sales analytics on order events'
status: To Do
assignee: []
created_date: '2026-11-20 19:47'
labels:
  - examples
  - streams
dependencies:
  - TASK-337.04
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 177000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Rebuild the Prefab Streams example on the shop domain in the optional source folder `examples/showcase/src/streams/java` (+ `src/streams-test/java`). Replaces `examples/streams` (energy domain).

Topologies on order events from TASK-337.04: revenue per category (`groupBy` + `aggregate`), hourly revenue per category (`windowedAggregate` with grace period), order/payment matching (KStream-KStream `join` with `JoinWindow`), routing by order type (`branch`/`merge`), enrichment from a catalog global store, a native `breakout`, opt-in tracing (`prefab.streams.tracing.enabled`). Tests use the backend-neutral topology test API (TestStreams / TopologyTestHarness).

Only variants with a streams backend include this folder; others report STREAMS as a gap (TASK-337.02).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Streams code and tests live in an optional source folder included by the showcase (Kafka) build
- [ ] #2 Every DSL operator used in the old streams example is demonstrated on the shop domain
- [ ] #3 Variants without a streams backend report STREAMS as skipped with the gap task ID
<!-- AC:END -->
