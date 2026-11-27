---
id: TASK-403
title: Apply event migrations in Kafka consumers
status: To Do
assignee: []
created_date: '2026-11-23 18:00'
labels:
  - kafka
  - versioning
dependencies: []
priority: high
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Found while building the in-memory event bus (TASK-175.03).

`KafkaConfiguration#dynamicDeserializer` (`core/src/main/java/be/appify/prefab/core/kafka/KafkaConfiguration.java`) builds `DynamicDeserializer` with the three-argument constructor, which defaults to `EventMigrator.none()`. `EventMigrator` is imported in that file but never injected. Consequently real Kafka consumers never apply `@EventMigration` migrations to messages written with an older major version, while the Pub/Sub and SQS deserializers (`PubSubDeserializer`, `SqsDeserializer`) do receive the migrator.

`DynamicDeserializer` has a four-argument constructor taking the `EventMigrator`, and `PrefabCoreConfiguration#eventMigrator` provides the bean, so the fix is to inject it (optionally, falling back to `EventMigrator.none()`) into the deserializer bean.

The test module's `KafkaTestAutoConfiguration#testConsumerFactory` has the same three-argument construction and should receive the migrator too.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The `DynamicDeserializer` bean used by the Kafka consumer factory receives the `EventMigrator` bean
- [ ] #2 A test proves that a message of an older major version is migrated when read through the configured consumer factory
- [ ] #3 `KafkaTestAutoConfiguration#testConsumerFactory` applies the migrator as well
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
