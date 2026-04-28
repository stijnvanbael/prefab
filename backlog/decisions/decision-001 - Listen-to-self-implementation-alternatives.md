---
id: decision-001
title: Listen-to-self implementation alternatives
date: '2026-04-27 16:39'
status: proposed
---

## Context

TASK-140 requires aggregate roots to support **listen-to-self** semantics: the aggregate raises a domain event,
the framework publishes it to an async broker (Kafka / Pub/Sub / SNS-SQS), and the same service consumes it through
an idempotent event handler that performs the actual state mutation.

### Current state

- `PublishesEvents.publish()` forwards to `DomainEventPublisher`, which today dispatches on the **Spring
  application event bus** (synchronous, in-process).
- `@Event(topic = "…")` on an event class generates an async producer that publishes to the configured broker.
- `@EventHandler` on a **static** method of an aggregate generates a consumer that creates or updates an aggregate
  instance from the incoming event.
- `@EventHandler` on an **instance** method (with `@ByReference` or `@Multicast`) loads an existing aggregate
  instance and mutates it.
- The aggregate itself never triggers an async consumer — those are two independent code paths today.

### The two-phase commit problem

When a command mutates an aggregate **and** publishes an event to an external broker, two separate resources are
involved: the database and the message broker. Without a distributed transaction (XA / two-phase commit), these two
writes cannot be made atomic:

- If the database commits but the broker publish fails → the event is lost; consumers never see it.
- If the broker publish succeeds but the database commit fails → the event is published for a state change that
  never happened.

Neither outcome is acceptable. Distributed transactions are expensive, fragile, and unsupported by most modern brokers.

### Backward compatibility constraint

The listen-to-self pattern is **opt-in**. Existing `@Create` and `@Update` methods that return a persisted aggregate
must continue to work exactly as today:

```java
// Existing pattern — must remain unchanged and fully supported:
@Create
public Order(@NotNull String customerId) {
    this(Reference.create(), 0L, OrderStatus.PENDING);
    // DB write happens immediately; returns 201 Created with the new resource.
}
```

Listen-to-self is an additional, explicitly chosen pattern on top of the existing behaviour — not a replacement for it.

### Goal

Allow a **single aggregate** to both *raise* an event and *react* to it asynchronously, so the **state change
happens only inside the async consumer** — never in the original command. The command's sole responsibility is
publishing the event. This eliminates the two-phase commit problem because:

1. The command publishes to the broker only (no DB write for the state change).
2. The consumer reads from the broker and writes to the DB only (no broker publish).

Each side touches exactly one resource.

### Key design constraints on all alternatives

Before evaluating alternatives, two constraints eliminate entire classes of designs:

**Constraint 1 — No phantom aggregate construction.**
The publishing side of a listen-to-self command must not construct an aggregate instance that is never persisted.
`new Order(..., PENDING).publish(event)` is misleading: a PENDING order appears to exist in the domain model but
is never saved. The command side must publish the event without constructing the aggregate at all.

**Constraint 2 — The annotation processor cannot trace events through method bodies.**
Java APT operates on declarations only — it cannot perform data-flow or call-graph analysis. It is impossible for
the processor to determine which events a `@Create` or `@Update` method publishes. Therefore the command side and
the consumer side must be independently processable from their annotations alone. The runtime connection between
them lives in the broker — not in the processor.

---

## Alternatives

### Alternative A — Per-method annotations: `@ListenToSelf` on the handler, `@FireAndForget` on the command

`@ListenToSelf` on an `@EventHandler` method signals that the processor should generate a broker consumer instead
of a Spring `@EventListener`. `@FireAndForget` on a `@Create` / `@Update` method signals that the endpoint should
publish and return `202 Accepted` without saving.

#### Client code

```java

@Aggregate
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        OrderStatus status,
        String trackingCode
) implements PublishesEvents {

    @Create
    public Order(@NotNull String customerId) {          // unchanged synchronous path
        this(Reference.create(), 0L, OrderStatus.CONFIRMED, null);
    }

    @Update(path = "/ship", method = "POST")
    @FireAndForget                                       // publish only, 202 Accepted
    public void ship(@NotNull String trackingCode) {
        publish(new OrderShipped(id, trackingCode));
    }

    @EventHandler
    @ListenToSelf                                        // broker consumer, not @EventListener
    @ByReference("order")
    public Order onOrderShipped(OrderShipped event) {
        return new Order(id, version, OrderStatus.SHIPPED, event.trackingCode());
    }
}
```

#### Trade-offs

| ✅ Pros                                                               | ❌ Cons                                                                               |
|----------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| Surgical opt-in per method — most of the aggregate stays synchronous | Two new annotations required, both needed together every time                        |
| `@Create` constructor pattern completely unchanged                   | Easy to forget one annotation; the combination is a protocol that must be documented |
| Processor processes each side independently                          | Scattered: the async contract is expressed across two unrelated methods              |
| Mixing sync and async methods on the same aggregate is explicit      | A mixed aggregate is harder to reason about — is `ship()` sync or async?             |

---

### Alternative B — `@AsyncCommit` on the aggregate class

A single class-level annotation signals that **all** state changes on this aggregate go through the broker.
The processor changes its code-generation strategy for the entire aggregate:

- `@Create` must be a **static method returning the event** (not a constructor returning the aggregate). This
  avoids phantom construction and gives the processor a visible return type without data-flow analysis.
- `@Update` `void` methods call `publish()` as today — the processor no longer saves the aggregate after the call.
  Both endpoints return `202 Accepted`.
- `@EventHandler` methods are automatically wired as broker consumers. `@ListenToSelf` is not needed.
- `@FireAndForget` is not needed anywhere — the class annotation makes the contract uniform.

#### Client code

```java

@Aggregate
@AsyncCommit                                            // all state changes go via the broker
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        OrderStatus status,
        String trackingCode
) implements PublishesEvents {

    // Calls publish() instead of constructing an aggregate. Processor generates a REST endpoint that calls this method,
    // publishes the event, and returns 202 Accepted.
    @Create
    public static void placeOrder(@NotNull String customerId) {
        publish(new OrderPlaced(Reference.create(), customerId));
    }

    // Calls publish() as today. Processor does NOT save afterwards. Returns 202 Accepted.
    @Update(path = "/ship", method = "POST")
    public void ship(@NotNull String trackingCode) {
        publish(new OrderShipped(id, trackingCode));
    }

    // Processor generates a broker consumer for both handlers automatically.
    @EventHandler
    public static Order onOrderPlaced(OrderPlaced event) {
        return new Order(event.reference(), 0L, OrderStatus.CONFIRMED, null);
    }

    @EventHandler
    @ByReference("order")
    public Order onOrderShipped(OrderShipped event) {
        return new Order(id, version, OrderStatus.SHIPPED, event.trackingCode());
    }
}
```

Non-`@AsyncCommit` aggregates like `ChannelSummary` are completely unaffected — they continue to generate
`@EventListener` exactly as before.

#### What the processor sees — independently on each side

| Declaration                                                                     | Processor generates                                                                                           |
|---------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `@Create static EventType method(…)`                                            | REST endpoint; calls method; publishes returned event; returns `202 Accepted`                                 |
| `@Update void method(…)` on `@AsyncCommit`                                     | REST endpoint; loads aggregate; calls method (which calls `publish()`); returns `202 Accepted`; does NOT save |
| `@EventHandler static AggregateType method(EventType)` on `@AsyncCommit`       | Broker consumer; calls method; saves result; deduplication guard                                              |
| `@EventHandler @ByReference AggregateType method(EventType)` on `@AsyncCommit` | Broker consumer; loads aggregate; calls method; saves result; deduplication guard                             |

#### Trade-offs

| ✅ Pros                                                                         | ❌ Cons                                                                         |
|--------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| One annotation expresses the whole contract                                    | All-or-nothing: every `@Update` on the aggregate becomes async                 |
| No `@ListenToSelf` or `@FireAndForget` needed anywhere                         | Cannot mix sync and async `@Update` methods on the same aggregate              |
| `@Update` void ambiguity disappears entirely — class context resolves it       | `@Create` must change to a static factory pattern for event-sourced aggregates |
| Uniform: every developer reading the class immediately understands the pattern | Static `@Create` is a new contract; documentation and tooling support needed   |
| Processor strategy is decided once at class level, not method by method        |                                                                                |

---

## Recommendation

**Alternative B — `@AsyncCommit` on the aggregate class.**

### Rationale

Alternative A solves the problem but scatters the contract across pairs of annotations on unrelated methods. Every
developer must know to apply both `@FireAndForget` and `@ListenToSelf` together, and that an `@Update void` is
asynchronous only if `@FireAndForget` is present. This is an invisible protocol that is easy to break silently.

`@AsyncCommit` makes the contract visible and uniform at the class level. The processor can derive all code
generation from a single declaration. The `@Update void` ambiguity disappears entirely — on an `@AsyncCommit`
aggregate, every `@Update` is fire-and-forget by definition.

The only cost is that `@Create` must change form: from a constructor to a static factory returning the event. This
is a small and honest change — the constructor form was always misleading in the event-sourced context because it
implied immediate persistence.

### Mixing sync and async when needed

If an aggregate genuinely needs some synchronous and some asynchronous operations, the right answer is to
**split the aggregate** by responsibility rather than mixing annotations on a single class. This is consistent with
the Single Responsibility Principle and keeps each aggregate's persistence contract uniform and easy to reason about.

### Generated code sketch

```java
// Generated: OrderKafkaConsumer.java
@Component
public class OrderKafkaConsumer {
    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "${topics.order.name}", groupId = "…")
    @Transactional
    public void onOrderPlaced(OrderPlaced event) {
        if (processedEventRepository.existsById(event.eventId())) return;
        processedEventRepository.save(event.eventId());
        orderRepository.save(Order.onOrderPlaced(event));
    }

    @KafkaListener(topics = "${topics.order.name}", groupId = "…")
    @Transactional
    public void onOrderShipped(OrderShipped event) {
        if (processedEventRepository.existsById(event.eventId())) return;
        processedEventRepository.save(event.eventId());
        var order = orderRepository.findById(event.order()).orElseThrow();
        orderRepository.save(order.onOrderShipped(event));
    }
}
```

## Consequences

- **Fully backward compatible**: only aggregates explicitly annotated with `@AsyncCommit` change behaviour.
  All existing aggregates are unaffected.
- One new source-retained annotation is needed: `@AsyncCommit` on the aggregate class.
- `@Create` on an `@AsyncCommit` aggregate must be a **static method returning the event type**. The constructor
  form is a compile error on `@AsyncCommit` aggregates (enforced by the processor).
- `@Update` `void` methods on `@AsyncCommit` aggregates are treated as publish-only; the processor omits the
  repository save and generates `202 Accepted`.
- `@EventHandler` methods on `@AsyncCommit` aggregates automatically generate broker consumers with a
  deduplication guard; `@ListenToSelf` and `@FireAndForget` are not introduced.
- A `ProcessedEvent` infrastructure record must be generated or provided by the framework; its primary key is a
  `UUID` populated from the `@EventId` field.
- A new `@EventId` annotation is required on event fields. The processor enforces its presence on every event
  type consumed by an `@AsyncCommit` aggregate and uses it as the sole deduplication key.
- Documentation and integration tests must cover: `@AsyncCommit` create, update, replay with deduplication, and
  interaction with external consumers on the same topic.
