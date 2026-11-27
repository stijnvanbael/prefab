---
id: TASK-375
title: Inspect registered Avro schemas in memory
status: Done
assignee: []
created_date: '2026-11-24 09:30'
labels:
  - test
  - avro
dependencies:
  - TASK-175
priority: low
type: task
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
In memory, Avro events use Confluent's mock schema registry (`mock://...`), which has no HTTP endpoint. The showcase
tests `PaymentIntegrationTest.createPayment_registersTheEventSchemaInTheSchemaRegistry` and
`ShipmentIntegrationTest.dispatch_registersTheEventSchemaInTheSchemaRegistry` read `GET {registry}/subjects` and are
therefore skipped in `examples/showcase-inmemory` through the `SCHEMA_REGISTRY_API` capability.

Make the check backend-agnostic, for example through a small test helper that lists the registered subjects of the
active registry (a `SchemaRegistryClient` for the mock scope in memory, HTTP with containers), then drop the
capability from the in-memory variant.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A test helper lists the registered Avro subjects in both modes
- [x] #2 The two showcase tests use it, and `SCHEMA_REGISTRY_API` is removed from `Capability` and the variants
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)

## Implementation notes

- Added the shared `SchemaRegistrySubjects` test helper, using Confluent's mock client for the in-memory
  `schema-url` scope and `CachedSchemaRegistryClient` for container-backed HTTP registries.
- Updated the payment and shipment schema-registration tests to run in both modes.
- Removed `SCHEMA_REGISTRY_API` and its in-memory capability gaps; updated the coverage matrix documentation.
- Verified with `mvn -pl examples/showcase,examples/showcase-inmemory -am -DskipTests test-compile`.
<!-- DOD:END -->
