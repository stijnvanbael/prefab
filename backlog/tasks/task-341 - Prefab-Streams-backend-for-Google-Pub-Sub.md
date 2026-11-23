---
id: TASK-341
title: Prefab Streams backend for Google Pub/Sub
status: In Progress
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
- [x] #2 Topology tests run against the Pub/Sub backend through the backend-neutral test API
- [ ] #3 showcase-pubsub no longer declares STREAMS as a gap (or declares only specific unsupported operators)
- [x] #4 Integration test (Pub/Sub emulator + Postgres) covers groupBy+aggregate, windowedAggregate, join and a global store end to end
- [x] #5 Redelivered messages do not change state or output twice
<!-- AC:END -->

## Analysis

Decision-008 assigns broker-neutral processing and transactional state to the existing streams-engine and
streams-postgres modules. This task must add a Pub/Sub adapter without coupling the engine to the Pub/Sub SDK.
The engine currently groups subscriptions by source topic and exposes neither the source type nor a subscription
identity to its source SPI; the adapter needs an explicit subscription contract to decode concrete bindings,
handle tombstones, and scope inbox identities correctly. PubSubUtil currently discards message metadata, so its
consumer API must be extended while preserving existing handler behavior and retry/dead-letter semantics.

The showcase streams slice (TASK-337.06) was merged to main while this work was in progress. The showcase-pubsub
variant (TASK-337.09) is not yet available; its acceptance criteria cannot be verified until that task is integrated.
Container-dependent integration tests are delegated to CI because this environment cannot run Testcontainers.

## Implementation notes

- Added the Pub/Sub source, outbox sink, Spring configuration and backend-neutral in-memory harness factory.
- A source declaration has a stable subscription/inbox namespace separate from other declarations of the same
  topic and type; all branches of the same source share that namespace. Global stores use separate subscriptions.
- Preserved nested polymorphic types in new Postgres JSON state envelopes, while retaining the legacy envelope
  reader. Database state is trusted data; the guide documents the codec's class-resolution trust boundary.
- Added isolated source, sink, configuration, state-codec and cross-thread processor regressions. The emulator +
  Postgres end-to-end test passed in CI, including redelivery with the same Prefab message ID.
- Output topic aliases are registered independently from default event dispatch topics, including concrete generic
  bindings, so an explicit stream sink does not change ordinary publisher routing.
- The first CI run failed during test compilation because Java 21 automatically ran the annotation processor on
  private test fixtures. The module now disables test-scope processing; a clean Java 21 test compilation succeeds.
- A subsequent CI run reached the integration test but its non-web Spring context lacked a `ConversionService`.
  The test application now supplies one explicitly, along with no-credentials emulator configuration.
- CI then exposed that a custom conversion service may not convert `7d` to a duration. The backend now parses
  the retention property using Spring Boot's duration syntax independently of the application's conversion service.
- Full Maven verify passed on Java 21 in CI run 45198222791 for commit `1b648925`. The task remains in progress
  because the showcase-pubsub variant (TASK-337.09) is not yet available for acceptance criteria #1 and #3.
- CI run 45200337894 later timed out waiting for aggregate output. The outbox relay can miss a wake-up when a
  second transaction commits after a topic was scanned while another relay is active. A deterministic in-memory test
  reproduced the missed wake-up; relay requests are now coalesced and trigger another scan, including for
  synchronously delivered downstream messages. CI must verify the container-backed suite after this fix.
