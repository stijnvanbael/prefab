---
id: TASK-175.03
title: In-memory event bus with Kafka serialization
status: In Progress
assignee: []
created_date: '2026-11-23 11:00'
labels:
  - test
dependencies:
  - TASK-175.01
parent_task_id: TASK-175
priority: high
type: task
ordinal: 156300
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace the Kafka transport with a synchronous in-process event bus in in-memory mode, round-tripping every event through the Kafka serializers (TASK-175 design §4, decision-011).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 `InMemoryEventBus` is the only `DomainEventDispatcher` for registered event types and honours topic overrides
- [ ] #2 Events are encoded at publish time with `DynamicSerializer` (headers included) and decoded per subscriber with `DynamicDeserializer`, so versions and migrations apply; Avro works with the mock schema registry when no registry URL is configured
- [ ] #3 Generated `@KafkaListener` methods are invoked per resolved topic (placeholders and SpEL), once per listener method; generic bindings receive the event type header
- [ ] #4 Delivery happens after commit of the publishing transaction, or immediately without one, in FIFO order per thread; events published by handlers are queued, not nested
- [ ] #5 A handler exception is recorded, not propagated to the publisher, and fails the test that triggered it; unknown event types are ignored as by real consumers
- [ ] #6 `@TestEventConsumer` fields receive decoded events in in-memory mode and are reset per test; `EventConsumerAssert` works unchanged
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->
