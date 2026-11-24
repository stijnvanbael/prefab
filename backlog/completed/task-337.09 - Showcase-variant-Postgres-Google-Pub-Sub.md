---
id: TASK-337.09
title: 'Showcase variant: Postgres + Google Pub/Sub'
status: Done
assignee: []
created_date: '2026-11-20 19:47'
updated_date: '2026-11-24 07:45'
labels:
  - examples
  - pubsub
dependencies:
  - TASK-337.05
  - TASK-337.06
  - TASK-337.07
  - TASK-341
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 180000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add `examples/showcase-pubsub`: pom (prefab-pubsub instead of prefab-kafka), application.yml (GCP project id, message ordering), test config and `showcase-capabilities.yml`. Replaces `examples/pubsub`. Include the common streams slice using the Pub/Sub backend from TASK-341 and Postgres state; do not include the Kafka-only breakout implementation. The Avro slice remains out of scope.

Run the full showcase suite, including the backend-neutral streams tests. Declare AVRO as a gap (payloads not self-describing, TASK-275), and declare only specific unsupported streams operators rather than a general STREAMS gap. Every other failing feature is fixed (if small) or declared with a new backlog task.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Module contains no Java sources
- [x] #2 Full showcase suite passes or skips with declared gaps, each linked to a backlog task
- [x] #3 Coverage matrix updated
- [x] #4 Showcase streams slice compiles and its tests pass in showcase-pubsub, or unsupported operators are skipped with a declared gap
- [x] #5 showcase-pubsub does not declare STREAMS as a gap; only specific unsupported operators are declared
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
The showcase-specific criteria formerly in TASK-341 are verified here, after the backend has been merged.
Declare AVRO with TASK-275, and document Kafka-only breakout and any use of unsupported scheduling
(TASK-363) as specific gaps where applicable.

**Implementation (2026-11-24)**

- `examples/showcase-pubsub`: pom (`prefab-postgres`, `prefab-pubsub`, `prefab-streams`, `prefab-streams-pubsub`,
  `prefab-streams-postgres`), `application.yml` (GCP project id, message ordering), `application-test.yml` and
  `showcase-capabilities.yml`. No Java. Only `showcase.streams.skip` is `false`, so the common streams slice is
  compiled and the Kafka-only breakout and Avro slices stay out. Registered in the root pom, with a
  `Showcase tests (Postgres + Pub/Sub)` run configuration.
- Declared gaps: `AVRO` (TASK-275) and `KAFKA_STREAMS_BREAKOUT` (breakout hands over the Kafka Streams API, a limit
  by design in decision-008, documented in TASK-341). `STREAMS` is not a gap. The showcase uses no
  `StreamProcessorContext.schedule`, so TASK-363 needs no gap.
- Coverage matrix and the streams/Avro prose in `examples/README.md` and `backlog/docs/modules.md` updated.
- Found and fixed on the way: `InMemoryAutoConfigurationFilter` threw a `NullPointerException` in in-memory mode,
  because Spring Boot passes `null` for candidates rejected by an earlier filter (regression test added).

**Verification**

This environment has no Docker, so the Pub/Sub emulator + Postgres suite could not be run here:
- Verified locally: the variant compiles with the annotation processor (including the streams slice), the four
  backend-neutral streams topology tests pass against the Pub/Sub harness, and `CapabilityGapsTest`,
  `VariantCapabilitiesTest`, `CoverageMatrixTest` and `EventPlatformTest` pass.
- Not verifiable in the authoring environment (no Docker): the integration tests. In-memory mode (`-Dprefab.test.containers=false`) is no substitute: it has
  no `StreamStateRepository` or Streams on the in-memory bus (TASK-175.05 #6), and it failed 30 of 85 integration
  tests without the streams slice, which is TASK-175.05's job to prove on the showcase.
- Verified by CI on the real stack (Pub/Sub emulator + Postgres): the full showcase suite passed on the merged head
  `f5da25f` (PR #226), after the fixes below. Criteria #2 and #4 are met; the streams slice runs without a `STREAMS`
  gap, and the declared gaps are `KAFKA_STREAMS_BREAKOUT` and `AVRO`.

**CI findings (first run on the real stack)**

The first run failed in `prefab-streams-sns-sqs` (TASK-371, unrelated). The re-run reached `showcase-pubsub` and
11 of 91 tests failed (ordering, inventory, invoicing). Causes found in the log:
- `application-test.yml` used Kafka's `prefab.dlt.retries.initial-interval-ms`; Pub/Sub reads `minimum-backoff-ms` and
  `maximum-backoff-ms` (defaults 1s to 30s), so failing messages outlived the test that published them.
- Nothing in the application publishes `CatalogProduct`, so `CatalogEnrichmentProcessor` threw for every order. Kafka
  hides that; on the Pub/Sub emulator, which has no dead-lettering, the nacked message is redelivered forever and its
  uncaught exception (`PubSubUtil.consume` rethrows after retries) fails whichever Awaitility call is running. Sales
  of products missing from the replicated catalog now count as `uncategorized`.
- Confirmed in the second CI run (after main fixed the SNS/SQS flake, TASK-371): events of one test reach their
  handlers after the next test's `PersistenceTestExecutionListener` wiped the repositories, so `@Multicast` finds no
  aggregates (TASK-365) and the message can never succeed. Concurrent handlers also collide on the optimistic lock
  (`STOCK_LEVEL`, `CUSTOMER_ORDER_HISTORY`), which needs a bigger retry budget (`prefab.dlt.retries.limit: 20`).
  `PubSubUtil.consume` rethrows after the nack, which Awaitility reports in an unrelated test; the showcase `Shop`
  disables Awaitility's uncaught-exception catching and TASK-373 tracks the Prefab side.
<!-- SECTION:NOTES:END -->
