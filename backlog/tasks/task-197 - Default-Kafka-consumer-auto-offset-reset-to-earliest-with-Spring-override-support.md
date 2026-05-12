---
id: TASK-197
title: >-
  Default Kafka consumer auto-offset-reset to earliest with Spring override
  support
status: Done
assignee: []
created_date: '2026-05-12 16:43'
updated_date: '2026-05-12 16:47'
labels: []
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Set Prefab Kafka consumer configuration to default `auto-offset-reset=earliest` while allowing users to override through the standard `spring.kafka.consumer.auto-offset-reset` property.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Kafka consumer configuration defaults `auto-offset-reset` to `earliest` when no explicit Spring consumer value is provided.
- [x] #2 If `spring.kafka.consumer.auto-offset-reset` is explicitly configured by the user, that value is preserved and used.
- [x] #3 Tests cover both default and override behavior.
- [x] #4 Developer guide documentation is updated where Kafka configuration behavior is described.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Defaulted Kafka consumer `auto.offset.reset` to `earliest` in `kafkaConsumerFactory` using `putIfAbsent`, so explicit `spring.kafka.consumer.auto-offset-reset` values continue to win. Added unit tests covering default and override scenarios, and updated configuration docs to reflect behavior.

Implemented Kafka consumer default offset reset in `KafkaConfiguration#kafkaConsumerFactory` by setting `ConsumerConfig.AUTO_OFFSET_RESET_CONFIG` to `earliest` via `putIfAbsent`, preserving explicit Spring consumer configuration.

Added `KafkaConfigurationTest` with two tests to verify default behavior (`earliest`) and override behavior via `spring.kafka.consumer` property mapping (`latest`).

Updated developer guide documentation in `backlog/docs/configuration.md` to document the new default and the standard Spring override property.
<!-- SECTION:NOTES:END -->
