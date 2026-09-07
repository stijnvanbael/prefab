---
id: TASK-282
title: Fail fast when Avro is used on Kafka without a schema registry URL
status: To Do
assignee: []
created_date: '2026-09-07 06:38'
labels:
  - bug
  - events
  - avro
  - kafka
  - streams
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When `schema.registry.url` is not configured, Prefab silently substitutes the Confluent mock registry instead of failing. `DynamicSerializer` (constructor) and `DynamicDeserializer` (constructor) both do `if (!properties.containsKey("schema.registry.url")) properties.put("schema.registry.url", "mock://schema-url")`, and `JsonKeySerde.withSchemaRegistryUrl` applies the same `putIfAbsent` default for Avro key serdes in the streams module.

A mock registry keeps schema ids in process memory, so a misconfigured deployment starts cleanly and then publishes messages whose schema ids resolve against nothing. Consumers in other processes cannot recover the writer schema, and the failure surfaces far from its cause. It also silently undermines any registry-based compatibility or schema-evolution strategy (TASK-274), because there is no shared registry to hold the schema versions.

The mock default exists so that tests and the annotation-processor fixtures do not need a registry. Keep that convenience where it is legitimately needed, but make a production application using `@Event(serialization = AVRO)` on Kafka fail at startup with an actionable message when no real registry is configured, rather than degrading to an in-memory mock. The test harness already wires a real registry testcontainer in `KafkaTestcontainerAutoConfiguration`, so tests have a supported path.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 An application that publishes or consumes AVRO events over Kafka without a configured schema.registry.url fails at startup with a message naming the missing property and the affected topics or event types.
- [ ] #2 The mock registry fallback is no longer applied implicitly; it is only used when explicitly opted in, for example through a Prefab test property or the existing test autoconfiguration.
- [ ] #3 Applications that use only JSON serialization are unaffected and still start without a schema registry.
- [ ] #4 The same behaviour applies to the streams module key serdes, which today apply the mock default independently in JsonKeySerde.
- [ ] #5 Existing tests and annotation-processor fixtures that rely on the mock registry continue to pass through the explicit opt-in, and a regression test covers the fail-fast path.
- [ ] #6 backlog/docs/configuration.md documents the schema registry requirement for AVRO on Kafka and the test-only opt-in.
<!-- AC:END -->
