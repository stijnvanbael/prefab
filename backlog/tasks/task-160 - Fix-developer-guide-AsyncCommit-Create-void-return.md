---
id: task-160
title: "Fix developer guide: @AsyncCommit @Create should use void return type with publishEvent()"
status: "Sone"
priority: "High"
labels: ["documentation", "async-commit", "reported-by:maestro"]
---

## Problem Statement

The Prefab developer guide section **7.8 Async Commit Pattern** and the `@AsyncCommit`
annotation reference (section 4.1) show an `@AsyncCommit @Create` static factory method
that **returns the event type**:

```java
// From the guide — misleading
@Create
public static OrderPlaced create(@NotNull String customerId) {
    return new OrderPlaced(Reference.create(), customerId);
}
```

This pattern does **not** publish the event to the message broker. The generated service
simply calls `ConversationSession.start(request.title())` and discards the return value:

```java
// Generated service (correct, but the guide example is misleading)
public void start(StartRequest request) {
    ConversationSession.start(request.title());  // return value silently discarded
}
```

The event is never published, so the `@EventHandler` is never triggered and the aggregate
is never persisted. The 202 response arrives but the resource never appears.

## Correct Pattern

The `@Create @AsyncCommit` static method must have a **`void` return type** and publish
the event explicitly using `PublishesEvents.publishEvent()`:

```java
@Create
@AsyncCommit
public static void start(@NotNull String title) {
    publishEvent(new SessionStarted(Reference.create(), title));
}
```

`PublishesEvents.publishEvent(Object)` is a static utility method that routes the event
through `DomainEventPublisher` → Spring `ApplicationEventPublisher` → the generated
`@EventListener` Kafka producer.

## Required Documentation Changes

### Section 4.1 — `@AsyncCommit` annotation reference

Replace:

> On `@Create` static factory method: must return the event type; generates `202 Accepted`

With:

> On `@Create` static factory method: must have `void` return type and call
> `PublishesEvents.publishEvent(event)` internally; the generated endpoint returns
> `202 Accepted`. The event is routed through the Spring application event bus to the
> generated Kafka (or Pub/Sub, SNS) producer.

### Section 7.8 — Async Commit Pattern

Update the code example:

```java
// CORRECT — void return, explicit publishEvent
@Aggregate
@GetById
public record Order(
        @Id Reference<Order> id,
        String customerId,
        String status
) {
    @Create
    @AsyncCommit
    public static void create(@NotNull String customerId) {
        publishEvent(new OrderPlaced(Reference.create(), customerId));
    }

    @EventHandler
    public static Order onOrderPlaced(OrderPlaced event) {
        return new Order(event.id(), event.customerId(), "PLACED");
    }
}

@Event(topic = "orders")
public record OrderPlaced(
        @PartitioningKey Reference<Order> id,
        String customerId
) { }
```

Add a note explaining why `void` is required and what happens if a non-void return type
is used (the return value is silently discarded by the generated service).

### Quick Reference (Appendix)

Update the `@AsyncCommit` row to clarify that `@Create` methods must be `void`.

## Acceptance Criteria

- [x] Section 4.1 `@AsyncCommit` corrected to describe void-return + `publishEvent()` pattern
- [x] Section 7.8 code example updated with `void` return and `publishEvent()` call
- [x] Annotation processor emits a **compile-time warning** (or error) when `@AsyncCommit @Create` has a non-void return type
- [x] Example projects / quick-start guide updated if they use the old pattern

