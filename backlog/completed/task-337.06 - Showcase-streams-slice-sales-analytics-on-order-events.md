---
id: TASK-337.06
title: 'Showcase streams slice: sales analytics on order events'
status: Done
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

The in-process engine for Pub/Sub and SNS/SQS (decision-008) supports no `breakout` and no `StreamProcessorContext.schedule`. Put the native `breakout` topology in a configuration that only loads with the Kafka backend (e.g. `@ConditionalOnClass(KafkaStreamBackend.class)`), and guard its test with a dedicated capability. The rest of the slice then runs unchanged on those variants (TASK-341, TASK-342).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Streams code and tests live in an optional source folder included by the showcase (Kafka) build
- [x] #2 Every DSL operator used in the old streams example is demonstrated on the shop domain
- [x] #3 Variants without a streams backend report STREAMS as skipped with the gap task ID
<!-- AC:END -->

## Analysis

The showcase already defines `OrderEvent.Placed` / `Cancelled` and `InvoicePaid`, but the order event hierarchy is not
currently a `Keyed` stream value and `InvoicePaid` is keyed by invoice rather than order. Keep the existing event
contracts stable where possible: make order events stream-keyed by their existing order reference, and model the
order-payment join input as a separate stream event keyed by order. Catalog enrichment needs a replicated catalog
entry store plus a processor that reads it.

The variants currently present are Kafka-backed (`showcase` and `showcase-mongodb`). Pub/Sub/SNS-SQS variants do not
exist yet; their planned in-process backend supports the common topology operators but not Kafka's native breakout.
Keep Kafka-dependent sources/tests in separate optional source roots, add a dedicated breakout capability, and make
the shared variant build copy common streams sources only when explicitly enabled. The capability gap mechanism will
then surface `STREAMS` or breakout gaps through each variant's existing capabilities file.

## Implementation Plan

1. Add optional shared streams and Kafka-only streams source/test roots to the showcase and variant build, and add the
   matching streams dependencies to the two existing Kafka variants.
2. Implement order-keyed sales, hourly category revenue, catalog global-store enrichment, order/payment joining, and
   order-type branch/merge topologies using Prefab Streams.
3. Keep the native breakout topology and its test Kafka-only, guarded by a Kafka class condition and dedicated
   capability.
4. Add backend-neutral topology tests for aggregation, grace periods, joins, routing, global-store enrichment and
   tracing; update the showcase coverage matrix and streams documentation.
5. Run the showcase and MongoDB variant checks, record implementation notes, and complete this backlog task.

## Implementation Notes

The showcase and MongoDB + Kafka variant now add `src/streams/java` and `src/streams-test/java` as optional source
roots. Their shared variant parent defaults to excluding both folders; Kafka breakout code and tests live in the
separate `src/streams-kafka/java` and `src/streams-kafka-test/java` roots, also excluded by default. Both current
variants explicitly enable both sets and depend on `prefab-streams` / `prefab-streams-kafka`.

`OrderEvent` implements `Keyed<Reference<Order>>` using the existing partitioning key. The sales topology filters placed
orders, expands quantities with `flatMap`, enriches each line from a catalog global store via `process`, and emits
running and hourly category revenue. Missing catalog entries fail explicitly instead of silently dropping revenue.
Separate topologies demonstrate order/payment window joins and placed/cancelled branch-and-merge routing. The payment
join uses a dedicated order-keyed payment feed; the existing `InvoicePaid` contract remains keyed by invoice and was
not changed. Kafka breakout is conditional on `KafkaStreamBackend` and guarded by the new
`KAFKA_STREAMS_BREAKOUT` capability. The capability gap test already emits skipped entries for any unsupported
capability declared by a variant; the future Pub/Sub/SNS-SQS showcase variants are not present in this checkout and
should declare the applicable specific gaps when added.

The examples README and streams feature/module guides describe the new topology and how variants opt in or declare
gaps.

**Verification:** clean focused builds on both `showcase` and `showcase-mongodb` passed 18 topology, capability,
coverage-matrix and event-platform tests each. The full Testcontainers-dependent suite was not run locally; per the
request, it is delegated to CI because Testcontainers time out on this machine.

CI verification exposed that Kafka Streams needs the catalog global-store source topic to exist before the stream
starts. The Kafka showcase now provisions that topic with a `NewTopic` bean, using the same topic constant as the
`CatalogProduct` event declaration.
