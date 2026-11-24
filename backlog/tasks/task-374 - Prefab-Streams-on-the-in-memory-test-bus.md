---
id: TASK-372
title: Prefab Streams on the in-memory test bus
status: To Do
assignee: []
created_date: '2026-11-24 09:00'
labels:
  - streams
  - test
dependencies:
  - TASK-175
priority: medium
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`@IntegrationTest` runs in memory by default (TASK-175), but Prefab Streams topologies are not wired to the in-memory
event bus: the streams backends (`prefab-streams-kafka`, `-pubsub`, `-sns-sqs`, `-postgres`, `-mongodb`) need their
broker, so the showcase variant `examples/showcase-inmemory` declares `STREAMS` and `KAFKA_STREAMS_BREAKOUT` as
unsupported, and Streams tests need `@IntegrationTest(containers = true)`.

Run Prefab Streams topologies on the in-memory bus, so applications with streams get container-free integration tests.

Starting points:
- `streams-engine` already contains `InMemoryTopologyTestHarness`, an in-process topology runner used by topology unit
  tests, and the in-process engine with pluggable state repositories (TASK-362).
- The in-memory bus (`be.appify.prefab.test.inmemory.messaging`) delivers synchronously after commit, which suits a
  synchronous engine; the open question is how a topology's source and sink topics map to bus topics, and how
  repartition and changelog topics are handled.
- `KAFKA_STREAMS_BREAKOUT` operators (Kafka Streams specific) need a decision: unsupported in memory, or an emulation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Analysis and design recorded in this task: how topology sources and sinks bind to the in-memory bus, state stores, windowing and time
- [ ] #2 With `prefab.test.containers=false`, the showcase Streams slice (`STREAMS`) passes in `examples/showcase-inmemory`, and the capability is removed from its capabilities file
- [ ] #3 A decision is made and documented for `KAFKA_STREAMS_BREAKOUT`
- [ ] #4 Documentation in `backlog/docs/` (testing section of `feature-guides.md`, `modules.md`) describes Streams in memory
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
