---
id: TASK-219
title: Auto-bootstrap Kafka Streams topologies from Prefab StreamDefinition beans
status: Done
assignee: []
created_date: '2026-05-17 13:04'
updated_date: '2026-05-21 06:21'
labels:
  - feature
  - streams
  - kafka
  - autoconfiguration
milestone: m-1
dependencies:
  - TASK-202
  - TASK-218
references:
  - >-
    streams/src/main/java/be/appify/prefab/streams/kafka/StreamsConfiguration.java
  - streams/src/main/java/be/appify/prefab/streams/StreamDefinition.java
  - >-
    examples/streams/src/main/java/be/appify/prefab/example/streams/StreamTopologyConfiguration.java
priority: high
ordinal: 23200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab Streams should automatically discover StreamDefinition beans and bootstrap Kafka Streams topology/lifecycle without requiring manual startup wiring.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Kafka Streams topology is auto-bootstrapped when StreamDefinition beans are present
- [x] #2 Bootstrap uses Prefab Streams defaults and integrates with Spring Boot Kafka Streams lifecycle
- [x] #3 No manual topology startup code is required in examples/streams
- [x] #4 Automated tests verify topology startup registration behavior
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- Added automatic StreamDefinition bootstrap in streams autoconfiguration via a `SmartInitializingSingleton` bean (`streamTopologyBootstrap`) that discovers all `StreamDefinition` beans and eagerly builds them before lifecycle startup.
- This guarantees Prefab Streams DSL definitions are registered into Kafka Streams topology without extra manual startup wiring.
- Added tests:
  - `StreamsConfigurationBootstrapTest` verifies all discovered StreamDefinition beans are initialized by the bootstrapper.
  - `KafkaPrefabStreamsTopologyTest#multipleDefinitions_shouldShareCombinedTopology` verifies multiple DSL definitions contribute to one combined topology.
  - `StreamsExampleApplicationTest` confirms the Spring Boot example starts and forwards records without manual topology bootstrap code.
- Updated docs in `backlog/docs/feature-guides.md` to document auto-discovery/bootstrap behavior and the `StreamDefinition` return type example.
<!-- SECTION:NOTES:END -->
