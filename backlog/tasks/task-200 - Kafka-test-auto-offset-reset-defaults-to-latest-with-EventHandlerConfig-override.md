---
id: TASK-200
title: >-
  Kafka test auto-offset-reset defaults to latest with EventHandlerConfig
  override
status: In Progress
assignee:
  - '@copilot'
created_date: '2026-05-14 09:14'
updated_date: '2026-05-14 09:14'
labels:
  - kafka
  - annotations
  - testing
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Change Kafka behavior so production keeps default auto-offset-reset at earliest, test consumer factory defaults to latest, and generated Kafka listeners can override auto.offset.reset per handler via @EventHandlerConfig using either literal values or Spring property placeholders. The EventHandlerConfig value should take precedence over spring.kafka.consumer.auto-offset-reset when set.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Kafka production consumer factory continues defaulting auto.offset.reset to earliest when no Spring property is provided.
- [ ] #2 Kafka test consumer factory defaults auto.offset.reset to latest when no explicit consumer property is provided.
- [ ] #3 @EventHandlerConfig exposes a Kafka-only autoOffsetReset attribute with empty default; supports literal values and Spring placeholders.
- [ ] #4 Generated Kafka listener uses @EventHandlerConfig(autoOffsetReset=...) to set auto.offset.reset and this override takes precedence over spring.kafka.consumer.auto-offset-reset.
- [ ] #5 Processor and core/test module tests cover default test behavior and EventHandlerConfig override generation.
- [ ] #6 Developer guide docs are updated to describe Kafka-only EventHandlerConfig autoOffsetReset behavior and test-vs-production defaults.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extend EventHandlerConfig with Kafka-specific autoOffsetReset attribute (empty default) plus utility detection for custom offset override.
2. Update Kafka consumer generation to emit @KafkaListener(properties = "auto.offset.reset=...") when annotation override is set.
3. Keep KafkaConfiguration production default at earliest; change KafkaTestAutoConfiguration test consumer default to latest.
4. Add/adjust unit tests and expected generated-source fixtures for override generation and test default behavior.
5. Update developer guide docs in annotation-reference and configuration sections.
6. Run focused Maven tests for changed modules and commit with conventional commit message.
<!-- SECTION:PLAN:END -->
