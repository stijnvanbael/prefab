---
id: TASK-337.07
title: 'Showcase Avro slice: Avro-serialized events'
status: Done
assignee: []
created_date: '2026-11-20 19:47'
updated_date: '2026-11-23 16:00'
labels:
  - examples
  - avro
dependencies:
  - TASK-337.04
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 178000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace `examples/avro` with an optional source folder `examples/showcase/src/avro/java` (+ tests). Serialization is chosen per event in source (`@Event(serialization = AVRO)`), so Avro cannot be a config-only variant.

Cover: Avro-serialized events (e.g. sale/payment events), `@Avsc`/`@AvscFile` usage, schema registry in tests (`prefab.test.schema-registry.enabled`), prefab-avro-processor. Include the folder in the Kafka-based builds (showcase, showcase-mongodb). Pub/Sub and SNS/SQS write bare Avro binary without a writer schema, which cannot be decoded (TASK-275), so those variants declare AVRO as a gap linked to TASK-275 and include the folder once it is fixed.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Avro events are produced and consumed in an integration test on Kafka
- [x] #2 Avro slice is included in every variant whose broker supports Avro; limitations reference their gap task
<!-- AC:END -->

## Analysis

Avro is chosen per event in source, so it lives in an optional source folder like streams. Besides Java sources and
tests, the AVSC-first example needs a *resource*: the annotation processor reads `.avsc` files from the classpath, so
the folder gets a resource root (`src/avro/resources`) that the showcase and the variant build add before compiling.
The old `examples/avro` domain (cash register, sale, customer) is replaced by two aggregates on the shop domain that
cover both styles: `Payment` (code-first events) and `Shipment` (AVSC-first event).

## Implementation Plan

1. Add `src/avro/java`, `src/avro/resources` and `src/avro-test/java` to the showcase pom (`prefab-avro`,
   `prefab-avro-processor`) and an opt-in copy (`showcase.avro.skip`) to the variant parent.
2. Code-first: `Payment` aggregate with `PaymentEvent` (`Received`, `Refunded`, `@Decimal` amount) and a `Receipt`
   aggregate that is created and voided by event handlers, so the test proves the event is encoded, written, read back
   and decoded.
3. AVSC-first: `Shipment` aggregate created from `OrderEvent.Placed`, publishing `ShipmentDispatched` generated from
   `shipment-dispatched.avsc` through `@Avsc(files = @AvscFile(..., keyProperty))`.
4. Enable the Testcontainers schema registry for the showcase and `showcase-mongodb`; document the slice.

## Implementation Notes

The showcase and `showcase-mongodb` (both Kafka) include the slice; `showcase-mongodb` adds `prefab-avro` and
`prefab-avro-processor` and sets `showcase.avro.skip=false`. Both enable `prefab.test.schema-registry` in
`application-test.yml`, and the local configuration points at a schema registry on `localhost:8081` (Avro topics make
the registry mandatory at startup). The `AVRO` capability already existed; no variant lists it as unsupported yet.
The Pub/Sub and SNS/SQS variants (TASK-337.09, TASK-337.10) declare `AVRO` as a gap linked to TASK-275, because they
write bare Avro binary without a writer schema, and include the slice once that is fixed. The README documents this.

`ShipmentClient`, the generated test client of an aggregate that publishes an AVSC-generated event, is written to
`target/classes` instead of `target/prefab-test-sources`, because the aggregate is processed in a later round in which
the project root cannot be derived. `ShipmentIntegrationTest` therefore calls the endpoints through MockMvc; the
processor fix is TASK-368. Also found while building it: the `@Avsc` contract interface must be sealed and permit the
generated record, and the Avro namespace must equal the package (both known, TASK-351 and TASK-354).

CI exposed two more things. `@ByReference(property)` names the event property that holds the reference of the
handling aggregate itself, so `Receipt` (a different aggregate from `Payment`) must use `@Multicast` with a repository
query, like `Invoice.onOrderCancelled`. And the AsyncAPI plugin lost every event that is not generated from an AVSC
file as soon as the module contained one: the document is written in a later processing round that only sees the
generated sources. `EventSchemaDocumentationPlugin` now keeps the events and consumed events of the deferring round
(covered by `AvscEventSchemaDocumentationTest`). A second mixing problem, the single AVSC file being applied to
code-first events (wrong Avro namespace), is TASK-369.

**Verification:** the showcase and `showcase-mongodb` compile without warnings (main and tests), the capability,
coverage-matrix, event-platform and topology tests pass, and a throwaway test confirmed that the generated converters
round-trip `PaymentEvent.Received` (including the decimal) and that the AVSC schema loads and validates. The
Testcontainers integration tests need Docker, which this environment does not have, so they run in CI.
