---
id: TASK-230
title: 'Rework event publishers: simplify and decouple from Spring domain events'
status: Done
assignee: []
created_date: '2026-05-22 12:16'
updated_date: '2026-05-22 16:53'
labels:
  - architecture
  - events
  - kafka
  - pubsub
  - sns-sqs
dependencies: []
priority: medium
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The current event publishing pipeline routes domain events through Spring's `ApplicationEventPublisher` (`SpringDomainEventPublisher`), which then re-publishes them as Spring application events. `GenericKafkaProducer`, `GenericPubSubPublisher`, and `GenericSnsPublisher` listen via `@EventListener(Object)` and filter by registry presence.

**Refined scope — two categories of events:**

1. **`@Event`-annotated records** (registered in `EventRegistry` / `SqsUtil` / `PubSubUtil`) → dispatch **directly** to infrastructure adapters, bypassing Spring's `ApplicationEventPublisher`.
2. **Non-`@Event`-annotated objects** (e.g. internal Spring events, framework hooks) → continue flowing through `ApplicationEventPublisher` unchanged.

**Important runtime constraint:** `@Event` carries `RetentionPolicy.CLASS`, so it is not available via reflection at runtime. The runtime proxy for "is this an `@Event`-type?" is: *is the type registered in the infrastructure registry?* The generic publishers already perform this check (`eventRegistry.topicForType(...)`, `sqsUtil.tryTopicForType(...)`, `pubSubUtil.tryTopicForType(...)`).

**Proposed design:**

Introduce a `DomainEventDispatcher` interface in `core`:
```java
public interface DomainEventDispatcher {
    boolean canDispatch(Class<?> eventType);
    void dispatch(Object event);
}
```

`GenericKafkaProducer`, `GenericPubSubPublisher`, and `GenericSnsPublisher` implement `DomainEventDispatcher`:
- `canDispatch` → delegates to the relevant registry presence check.
- `dispatch` → existing publish logic (without `@EventListener`).

`SpringDomainEventPublisher` receives a `List<DomainEventDispatcher>` and reroutes:
```
for each event:
  dispatchers that canDispatch(event.getClass()) → dispatch(event)
  if none matched → applicationEventPublisher.publishEvent(event)
```

**Files in scope:**
- `core/…/domain/DomainEventPublisher.java` — unchanged (abstract base stays)
- `core/…/spring/SpringDomainEventPublisher.java` — inject dispatchers; route by registry presence
- `core/…/kafka/GenericKafkaProducer.java` — implement `DomainEventDispatcher`; remove `@EventListener`
- `core/…/pubsub/GenericPubSubPublisher.java` — implement `DomainEventDispatcher`; remove `@EventListener`
- `core/…/sns/GenericSnsPublisher.java` — implement `DomainEventDispatcher`; remove `@EventListener`
- New interface `core/…/domain/DomainEventDispatcher.java`
- Expected test-resource fixtures for generated producers that contain `@EventListener` may need updating if they are tested; currently none appear to reference them in active tests.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 DomainEventDispatcher interface exists in core with canDispatch(Class<?>) and dispatch(Object) methods
- [x] #2 GenericKafkaProducer implements DomainEventDispatcher; @EventListener is removed; canDispatch delegates to EventRegistry
- [x] #3 GenericPubSubPublisher implements DomainEventDispatcher; @EventListener is removed; canDispatch delegates to PubSubUtil
- [x] #4 GenericSnsPublisher implements DomainEventDispatcher; @EventListener is removed; canDispatch delegates to SqsUtil
- [x] #5 SpringDomainEventPublisher injects List<DomainEventDispatcher>; routes @Event-registered types directly to matched dispatchers; falls back to ApplicationEventPublisher for unregistered types
- [x] #6 CapturingDomainEventPublisher and PublishedEventsExtension continue to work correctly for unit tests
- [x] #7 Non-@Event objects (e.g. Spring framework events fired via PublishesEvents) still reach ApplicationEventPublisher
- [x] #8 All existing tests pass; new unit tests cover direct-dispatch path and fallback path
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Step 1 — Introduce `DomainEventDispatcher` interface
Create `core/src/main/java/be/appify/prefab/core/domain/DomainEventDispatcher.java`.
Two methods: `boolean canDispatch(Class<?> eventType)` and `void dispatch(Object event)`.

### Step 2 — Implement `DomainEventDispatcher` on generic publishers
- **`GenericKafkaProducer`**: `canDispatch` → `eventRegistry.topicForType(type) != null` (use the null-safe path); `dispatch` → existing publish logic; remove `@EventListener`.
- **`GenericPubSubPublisher`**: `canDispatch` → `pubSubUtil.tryTopicForType(type).isPresent()`; `dispatch` → existing publish logic; remove `@EventListener`.
- **`GenericSnsPublisher`**: `canDispatch` → `sqsUtil.tryTopicForType(type).isPresent()`; `dispatch` → existing publish logic; remove `@EventListener`.

### Step 3 — Rework `SpringDomainEventPublisher`
Inject `List<DomainEventDispatcher> dispatchers`. In `publish(Object event)`:
- Find all dispatchers where `canDispatch(event.getClass())` is true.
- If at least one matches → call `dispatch(event)` on each matching dispatcher.
- If none match → delegate to `applicationEventPublisher.publishEvent(event)`.

### Step 4 — Update expected test fixtures (if referenced)
Check whether the `expected/kafka/*KafkaProducer.java`, `expected/pubsub/*PubSubPublisher.java`, `expected/sns/*SnsPublisher.java` files are asserted in active tests. If so, remove `@EventListener` from those fixtures and update the corresponding writer/test. Mark as out-of-scope if no test references them currently.

### Step 5 — Tests
- Unit-test `SpringDomainEventPublisher`: given a dispatcher that `canDispatch` → verify `dispatch` called and `applicationEventPublisher` not called.
- Unit-test fallback: given no dispatcher matches → verify `applicationEventPublisher.publishEvent` called.
- Verify `CapturingDomainEventPublisher` still captures correctly in unit tests using `PublishedEventsExtension`.
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Implementation summary

### New files
- `core/…/domain/DomainEventDispatcher.java` — new interface with `canDispatch(Class<?>)` and `dispatch(Object)`
- `core/…/spring/SpringDomainEventPublisherTest.java` — six unit tests covering direct-dispatch, fallback, multi-dispatcher, and lifecycle

### Modified files

| File | Change |
|---|---|
| `EventRegistry` | Added `hasTopicForType(Class<?>)` to enable clean `canDispatch` checks without exception-driven control flow |
| `GenericKafkaProducer` | Implements `DomainEventDispatcher`; `@EventListener` removed; `canDispatch` → `eventRegistry.hasTopicForType`; `publish` renamed to `dispatch` |
| `GenericPubSubPublisher` | Implements `DomainEventDispatcher`; `@EventListener` removed; `canDispatch` → `pubSubUtil.tryTopicForType(...).isPresent()` |
| `GenericSnsPublisher` | Implements `DomainEventDispatcher`; `@EventListener` removed; `canDispatch` → `sqsUtil.tryTopicForType(...).isPresent()` |
| `SpringDomainEventPublisher` | Injects `List<DomainEventDispatcher>`; routes `@Event`-registered types directly to all matching dispatchers; falls back to `ApplicationEventPublisher` when none match |

### Routing logic (SpringDomainEventPublisher.publish)
- Collect dispatchers where `canDispatch(event.getClass()) == true`
- If any matched → call `dispatch(event)` on each (Kafka, Pub/Sub, SNS/SQS receive event directly)
- If none matched → `applicationEventPublisher.publishEvent(event)` (non-`@Event` objects still use Spring bus)

### Key design note
`@Event` is `RetentionPolicy.CLASS` so not available at runtime. Registry presence (already checked by the generic publishers) is the correct runtime proxy for "this is an `@Event` type".

Expected `*KafkaProducer.java` / `*PubSubPublisher.java` / `*SnsPublisher.java` test-resource fixtures that contained `@EventListener` are legacy files not referenced by any active test — left unchanged as they are out of scope.
<!-- SECTION:FINAL_SUMMARY:END -->
