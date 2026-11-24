---
id: TASK-375
title: Inspect registered Avro schemas in memory
status: To Do
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
- [ ] #1 A test helper lists the registered Avro subjects in both modes
- [ ] #2 The two showcase tests use it, and `SCHEMA_REGISTRY_API` is removed from `Capability` and the variants
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
