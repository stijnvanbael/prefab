---
id: TASK-338
title: Split prefab-streams into a backend-neutral API and a Kafka backend module
status: To Do
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
- [ ] #1 prefab-streams has no Kafka dependency
- [ ] #2 prefab-streams-kafka contains all Kafka-specific code and auto-configuration
- [ ] #3 Existing streams tests and the showcase streams slice pass unchanged apart from the dependency
- [ ] #4 Developer guide and migration notes explain the new artifact
<!-- AC:END -->
