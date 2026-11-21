---
id: TASK-338
title: Split prefab-streams into a backend-neutral API and a Kafka backend module
status: Done
updated_date: '2026-11-21 09:00'
assignee: []
created_date: '2026-11-20 19:48'
labels:
  - streams
  - coverage-gap
dependencies: []
priority: medium
type: task
ordinal: 183000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`prefab-streams` ships the backend-neutral DSL (`be.appify.prefab.streams`: PrefabStreams, PrefabStream, StreamDefinition, Store, AggregationWindow, testing/TopologyTestHarness, ...) together with the Kafka implementation (`be.appify.prefab.streams.kafka`, 29 classes) and depends on spring-boot-starter-kafka, kafka-streams and kafka-streams-avro-serde. The neutral package has no Kafka imports already, so any application using the DSL pulls in Kafka, and a second backend cannot be added cleanly.

Split into `prefab-streams` (API + backend-neutral test API) and `prefab-streams-kafka` (KafkaPrefabStreams, serdes, breakout adapter, Kafka test harness, auto-configuration). `StreamBackend` should become extensible rather than an enum with KAFKA/CUSTOM. Prerequisite for streams backends on other brokers.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 prefab-streams has no Kafka dependency
- [x] #2 prefab-streams-kafka contains all Kafka-specific code and auto-configuration
- [x] #3 Existing streams tests and the showcase streams slice pass unchanged apart from the dependency
- [x] #4 Developer guide and migration notes explain the new artifact
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- New module `streams-kafka` (`prefab-streams-kafka`) holds everything under `be.appify.prefab.streams.kafka` (incl. `kafka.testing`), the `StreamsConfiguration` auto-configuration import and the `TopologyTestHarness.Factory` service registration. Package names are unchanged.
- `prefab-streams` now depends only on `prefab-core` (plus optional JUnit for `TopologyTestExtension`); no Kafka, Spring Kafka or Confluent artifacts. The Avro jar is a transitive dependency of `prefab-core`, not of the streams module.
- `StreamBackend` is a record identified by a case-insensitive name (`StreamBackend.named(..)`) instead of an enum, so other backends can add their own. The Kafka constant is `KafkaStreamBackend.KAFKA`; `KafkaPrefabStream.breakout` compares against it. Only breaking source change: `StreamBackend.KAFKA/CUSTOM` no longer exist.
- All existing streams tests (they need a backend, so they run through the Kafka harness) moved unchanged to `streams-kafka`, apart from `KafkaSpecificTopologyTest` using the new `StreamBackend` API. Added `StreamBackendTest` to `prefab-streams`.
- `examples/streams` depends on `prefab-streams-kafka` additionally; its tests pass unchanged. Root pom (module + dependencyManagement), dependabot, CONTRIBUTING, modules guide and the streams feature guide (section 7.15, incl. migration notes) are updated.
- Verified: `mvn install` for `streams`, `streams-kafka`, `examples/streams` and `test`: 5 + 128 + 8 + 44 tests green.
<!-- SECTION:NOTES:END -->
