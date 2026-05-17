---
id: TASK-202
title: Implement streams DSL source and sink for Kafka backend
status: To Do
assignee: []
created_date: '2026-05-17 09:14'
updated_date: '2026-05-17 09:18'
labels:
  - feature
  - streams
  - kafka
milestone: m-1
dependencies:
  - TASK-201
references:
  - core/src/main/java/be/appify/prefab/core/kafka/KafkaConfiguration.java
  - core/src/main/java/be/appify/prefab/core/kafka/DynamicSerializer.java
  - core/src/main/java/be/appify/prefab/core/kafka/DynamicDeserializer.java
  - backlog/tasks/task-099 - Prefab-Streams-DSL.md
priority: high
ordinal: 20200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement initial Kafka-backed DSL flow for `from(...)` and `to(...)`, including topic resolution and serialization integration.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Kafka backend supports `from(Class<?>)` and `to(Class<?>)` in the streams DSL
- [ ] #2 Source and sink wiring reuse Prefab serialization infrastructure (`DynamicSerializer` and `DynamicDeserializer`)
- [ ] #3 A sample topology in `examples/streams` reads from one topic and writes to another using only `from` and `to`
- [ ] #4 Automated test coverage validates source ingestion and sink emission for the sample topology
<!-- AC:END -->
