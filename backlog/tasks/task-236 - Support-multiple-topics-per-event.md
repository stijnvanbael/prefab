---
id: TASK-236
title: Support multiple topics per event
status: To Do
assignee: []
created_date: '2026-05-27 14:18'
labels:
  - feature
  - messaging
  - annotation-processor
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add support for publishing a single event to multiple Kafka/Pub-Sub/SNS-SQS topics and consuming from all of them by default.

## Motivation

Some events need to be routed to more than one topic (e.g. a shared integration topic plus a domain-specific one). Currently `@Event` only accepts a single `topic()` value, and every producer/publisher hard-codes that single topic.

## Proposed design

### `@Event` annotation changes
- Rename `topic()` → `topics()` (returns `String[]`) **or** keep `topic()` as primary and add `additionalTopics()` — TBD during implementation.
- Add `PublishTo publishTo() default PublishTo.FIRST` enum attribute:
  - `FIRST` — publish only to the first (primary) topic (backward-compatible default).
  - `ALL` — publish to every topic in the array.

### Producer / Publisher changes
All three generic dispatchers (`GenericKafkaProducer`, `GenericPubSubPublisher`, `GenericSnsPublisher`) and their corresponding `EventRegistry` / `*Util` helpers must:
- Expose an overloaded `dispatch(Object event, String... topicOverrides)` method (or equivalent) so callers can override the target topics at runtime.
- When no override is supplied, apply the `publishTo` strategy from the annotation.

### Consumer changes
- By default, consumers derived from `@EventHandler` methods subscribe to **all** topics listed in the event's `topics()` array.
- `@EventHandlerConfig` gains a new optional attribute (e.g. `consumeFromTopics() default {}`) to restrict which topics a particular handler listens on.

### Annotation processor
The annotation processor that generates Kafka listener / Pub-Sub subscriber / SQS listener beans must be updated to reflect the multi-topic arrays and the new `consumeFromTopics` override.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 @Event accepts an array of topics (e.g. topics() or topic() + additionalTopics())
- [ ] #2 PublishTo enum with FIRST and ALL values is added to @Event
- [ ] #3 @Event.publishTo() defaults to FIRST for backward compatibility
- [ ] #4 GenericKafkaProducer.dispatch() honours the publishTo strategy and publishes to the correct topic(s)
- [ ] #5 GenericPubSubPublisher.dispatch() honours the publishTo strategy and publishes to the correct topic(s)
- [ ] #6 GenericSnsPublisher.dispatch() honours the publishTo strategy and publishes to the correct topic(s)
- [ ] #7 All three dispatchers expose a runtime topic-override mechanism (varargs or equivalent)
- [ ] #8 Generated Kafka/Pub-Sub/SQS consumers subscribe to ALL topics listed in the event by default
- [ ] #9 @EventHandlerConfig gains a consumeFromTopics attribute to restrict which topics a handler listens on
- [ ] #10 Annotation processor updated to generate correct multi-topic listener/subscriber beans
- [ ] #11 Existing single-topic events continue to work without any source changes (backward compatible)
- [ ] #12 Unit and integration tests cover FIRST, ALL, and override scenarios for at least one platform
- [ ] #13 Developer guide updated to document the new multi-topic feature
<!-- AC:END -->
