---
id: TASK-149
title: Fix generated consumer compilation when event has multiple subtypes but aggregate handles only one
status: In Progress
assignee: [ ]
created_date: '2026-04-30'
updated_date: '2026-04-30'
labels:
  - events
  - kafka
  - code-generation
  - bug
dependencies: [ ]
priority: high
ordinal: 14900
---

## Description

When an `@Avsc` event interface declares multiple schemas (generating multiple concrete record types), but a
component or aggregate only registers an `@EventHandler` for **one** of those subtypes, the generated Kafka
consumer does not compile.

### Root cause

`ConsumerWriterSupport.sameType()` uses `isConcreteAvscRecordOf(handlerParam, rootEventType)` to decide whether
to emit a direct call (`singleTypeHandler`) or a `switch` statement (`multiTypeHandler`). When the `@Avsc`
interface has more than one concrete record, the Kafka listener method receives the **interface type**
(e.g. `OrderEvent`), not the concrete record. `isConcreteAvscRecordOf` still returns `true` because the
handler's parameter (e.g. `OrderCreatedEvent`) does implement the interface. As a result, `singleTypeHandler`
emits `orderProcessor.onOrderCreated(event)` where `event` is of type `OrderEvent`; this fails to compile
because `OrderEvent` is not assignable to `OrderCreatedEvent`.

### Example

```java
// event interface with two @Avsc schemas
@Event(topic = "prefab.order", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc({"OrderCreatedEvent.avsc", "OrderShippedEvent.avsc"})
public interface OrderEvent { }

// component that only handles one of the two subtypes
@Component
public class OrderProcessor {
    @EventHandler
    public void onOrderCreated(OrderCreatedEvent event) { ... }
}
```

Generated (broken) consumer:

```java
public void onOrderEvent(OrderEvent event) {
    orderProcessor.onOrderCreated(event); // compile error: OrderEvent is not OrderCreatedEvent
}
```

Expected generated consumer:

```java
public void onOrderEvent(OrderEvent event) {
    switch (event) {
        case OrderCreatedEvent e -> orderProcessor.onOrderCreated(e);
        default -> { }
    }
}
```

## Acceptance Criteria

- [ ] #1 When an `@Avsc` event has multiple concrete subtypes and a component/aggregate handles only one,
  the generated Kafka consumer compiles successfully and routes via a `switch` statement.
- [ ] #2 The existing behaviour for all other scenarios (single `@Avsc` type, all subtypes handled,
  non-`@Avsc` sealed interfaces) is unchanged.
- [ ] #3 A dedicated unit test (`avscPartialEventConsumer`) is added to `KafkaConsumerWriterTest` that
  verifies the correct generated output.
- [ ] #4 All existing `KafkaConsumerWriterTest`, `PubSubSubscriberWriterTest`, and `SqsSubscriberWriterTest`
  tests continue to pass.

## Implementation Notes

Fix `ConsumerWriterSupport.writeEventHandler`: replace the `eventType` parameter with `listenerParamType`
(the actual type of the `event` parameter on the listener method). Update `sameType` to call
`isConcreteAvscRecordOf(listenerParamType, handlerParamType)` — i.e. "is the listener's concrete record
assignable to the handler's expected type?" — instead of the reversed order that caused the bug.

Update `KafkaConsumerWriter.addEventHandlers` to pass the already-computed `listenerParamType` variable
(which is `concreteTypes.getFirst()` when there is exactly one concrete type, or `eventType` otherwise).
`PubSubSubscriberWriter` and `SqsSubscriberWriter` already pass `eventType` as the listener parameter type
and require no structural changes.
