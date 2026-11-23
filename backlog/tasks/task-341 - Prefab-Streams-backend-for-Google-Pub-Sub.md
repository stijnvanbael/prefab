---
id: TASK-341
title: Prefab Streams backend for Google Pub/Sub
status: To Do
assignee: []
updated_date: '2026-11-21 10:00'
created_date: '2026-11-20 19:48'
labels:
  - streams
  - pubsub
  - coverage-gap
dependencies:
  - TASK-339
  - TASK-360
  - TASK-361
priority: medium
type: feature
ordinal: 186000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement a `prefab-streams-pubsub` backend so the backend-neutral Prefab Streams DSL runs on Google Pub/Sub, following the design chosen in TASK-339. Today `StreamBackend` only knows KAFKA, so the showcase-pubsub variant (TASK-337.09) declares STREAMS as a gap pointing at this task.

Scope of a first version is the operator matrix from TASK-339. The backend-neutral topology test API (TestStreams / TopologyTestHarness, TASK-300) must work against it, so the showcase streams slice (TASK-337.06) can be included in showcase-pubsub without code changes.

## Design (decided in TASK-339, see decision-008)

Build the backend on the in-process, database-backed engine `prefab-streams-engine` (TASK-360), with state on Postgres (TASK-361). This module only adds the Pub/Sub parts:
- `PubSubStreamBackend.PUBSUB` (`StreamBackend.named("pubsub")`) and auto-configuration that activates when `PubSubUtil` and a `StreamStateRepository` are present.
- `StreamSource`: one subscription per source node (`{application}.streams.{step-name}`) and one shared subscription per global store, provisioned through `PubSubUtil` with message ordering and the existing dead-letter policy. Extend `SubscriptionRequest` with a metadata-aware consumer (message ID, attributes, publish time), and ack only after the engine's transaction commits. Concrete `EventType` bindings are filtered as in `PubSubUtil.consumeConcrete`.
- `StreamSink` used by the outbox relay: publish with ordering key = record key, attributes `prefab-message-id`, `prefab-event-time` and `prefab-correlation-ids`, the existing serializers, and `resumePublish` for the ordering key after a failed publish.
- Event time comes from `prefab-event-time`, otherwise from the Pub/Sub `publishTime`. Correlation IDs are cut to fit the 1024-byte attribute limit, keeping the newest IDs.

First-version operators (decision-008):
- Supported, provided by the engine:
  - `from`, `to`, `as`, `end`
  - `filter`, `map`, `flatMap`, `branch`, `merge`
  - `process`, also with stores, and `aggregate`
  - `keyBy` / `repartition` (logical, no topics)
  - `groupBy` + `aggregate`, and `windowedAggregate` with grace (stream time per key)
  - Inner `join` with `JoinWindow`, and `deduplicate`
  - Global stores as shared tables, without history from before the first deployment
  - Exactly-once through the inbox and outbox, tracing, and topology tests through the in-memory harness
- Not supported, failing at startup: `breakout` (Kafka-only) and `StreamProcessorContext.schedule` (TASK-363). In the showcase, declare these as specific gaps rather than as STREAMS.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Showcase streams slice compiles and its tests pass in showcase-pubsub, or unsupported operators are skipped with a declared gap
- [ ] #2 Topology tests run against the Pub/Sub backend through the backend-neutral test API
- [ ] #3 showcase-pubsub no longer declares STREAMS as a gap (or declares only specific unsupported operators)
- [ ] #4 Integration test (Pub/Sub emulator + Postgres) covers groupBy+aggregate, windowedAggregate, join and a global store end to end
- [ ] #5 Redelivered messages do not change state or output twice
<!-- AC:END -->
