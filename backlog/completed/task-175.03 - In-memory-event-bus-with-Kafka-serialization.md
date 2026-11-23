---
id: TASK-175.03
title: In-memory event bus with Kafka serialization
status: Done
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
- [x] #1 `InMemoryEventBus` is the only `DomainEventDispatcher` for registered event types and honours topic overrides
- [ ] #2 Events are encoded at publish time with `DynamicSerializer` (headers included) and decoded per subscriber with `DynamicDeserializer`, so versions and migrations apply; Avro works with the mock schema registry when no registry URL is configured
- [ ] #3 Generated `@KafkaListener` methods are invoked per resolved topic (placeholders and SpEL), once per listener method; generic bindings receive the event type header
- [ ] #4 Delivery happens after commit of the publishing transaction, or immediately without one, in FIFO order per thread; events published by handlers are queued, not nested
- [ ] #5 A handler exception is recorded, not propagated to the publisher, and fails the test that triggered it; unknown event types are ignored as by real consumers
- [x] #6 `@TestEventConsumer` fields receive decoded events in in-memory mode and are reset per test; `EventConsumerAssert` works unchanged
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- New package `be.appify.prefab.test.inmemory.messaging`, wired by `InMemoryMessagingAutoConfiguration` (in-memory mode only, after `PrefabRegistryConfiguration`, conditional on an `EventRegistry` bean). `InMemoryTestAutoConfiguration.InMemoryModeCondition` was made public so it can be reused.
- `InMemoryEventBus` is the `DomainEventDispatcher`. It resolves topics with `EventRegistry.topicsForDispatch` (or the overrides), encodes each message at publish time through a `MessageCodec`, and delivers in `afterCommit` of the active transaction. The outermost delivery runs inside a `PROPAGATION_NOT_SUPPORTED` `TransactionTemplate`, which suspends the committed transaction (it is still bound during `afterCommit`), so a `@Transactional` handler starts its own transaction and its published events are delivered after that commit, queued in the running trampoline. Publishing outside an actual transaction delivers immediately. Without a transaction it delivers immediately.
- Trampoline: a `ThreadLocal` delivery queue owned by the outermost delivery. `ScopedValue` is unavailable because the project compiles for `maven.compiler.release` 21, so `ThreadLocal` with `remove()` in `finally` is used.
- Per-platform plug-in point: `MessageCodec` (`supports(Class)`, `supports(EventType)`, `encode`, `decode`). `EncodedMessage` carries topic, string headers, bytes and its codec, so each subscriber decodes its own copy. `KafkaMessageCodec` wraps `DynamicSerializer`/`DynamicDeserializer`; TASK-175.04 adds codecs as beans.
- `KafkaListenerSubscriber` (`BeanPostProcessor` + `SmartInitializingSingleton`) collects `@KafkaListener` methods and subscribes them after all singletons exist, so topic placeholders and SpEL resolve against the final bean factory and the invoked bean is the (possibly proxied) singleton. Listener parameters: one payload parameter plus `@Header` `String` parameters.
- Serializers are built from `KafkaProperties`, the `ConversionService`, the `EventRegistry` and the `EventMigrator` bean; the mock schema registry is enabled when `schema.registry.url` is absent.
- Test listeners (registered in `spring.factories`): `InMemoryBusTestExecutionListener` resets before and fails after a test with an `AssertionError` (first handler failure as cause, others suppressed); `InMemoryEventConsumerExecutionListener` injects `@TestEventConsumer` fields in `beforeTestMethod` and closes the subscription in `afterTestMethod` (works for per-method and per-class test instances).
- `prefab-test` now has `kafka-streams-avro-serde` as a `provided` dependency (and the Confluent repository, like the other modules): Spring introspects `DynamicSerializer`, which references `GenericAvroSerializer`, so the class must be on the classpath of the module's own context tests.
- Differences from real Kafka: no partitions, offsets, consumer groups, concurrency or rebalancing; the group id is ignored and every listener method receives every message; no retries or dead-letter topic (a failing handler is recorded and fails the test); a message whose decoded event is not an instance of the listener's payload type is skipped instead of raising a conversion error; `@KafkaListener` features other than `topics` (patterns, partitions, error handlers) are ignored.
- Observation: `KafkaConfiguration` builds `DynamicDeserializer` without the `EventMigrator` (the three-argument constructor), while the in-memory bus passes the migrator bean, as the acceptance criteria require.
<!-- SECTION:NOTES:END -->
