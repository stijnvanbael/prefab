---
id: decision-002
title: Fluent domain assertion DSL for tests
date: '2026-04-28'
status: proposed
related_tasks:
  - TASK-142
---

## Context

Integration tests currently assert on raw return values: `create*` methods return a plain `String` (the
created ID), `get*ById` returns a bare `*Response` record, and event consumers are asserted with a
`Consumer<ListAssert<V>>` lambda. This leads to repetitive, low-readability assertion code scattered across
every integration test, with no shared vocabulary per domain concept.

The target style is a fluent, nested assertion DSL that reads like a specification, independent of the
mechanism that produced the value (REST call, event consumer, or unit test):

```java
assertThat(meteringConfig)
    .hasNumberOfSnapshots(1)
    .hasSnapshotSatisfying(snapshot -> snapshot
        .hasMeteringConfigSatisfying(mc -> mc
            .hasStatus(S2))
        .hasSnapshotType(ASSET_ORIGINAL_UPDATED)
        .hasSnapshotValidAsFromTimestamp(initialDate)
        .anyAfgaandvertrekSatisfying(av -> av
            .hasAfgaandvertrekId(Afgaandvertrek.idFromInstallatieId(installatieIdA))
            .anyToegangspuntSatisfying(tp -> tp.hasEanGsrn(ean)))
        .anyAfgaandvertrekSatisfying(av -> av
            .hasAfgaandvertrekId(Afgaandvertrek.idFromInstallatieId(installatieIdB))));
```

### Key requirement

The assertion classes must be **domain-first and transport-agnostic**: the same `OrderResponseAssert`
must work whether the `OrderResponse` was obtained from a REST test client, deserialized from a Kafka
event, or constructed directly in a unit test. No coupling to `ResultActions`, `MockMvc`, or any specific
messaging library is permitted inside the assertion classes themselves.

---

## Alternatives

### Alternative A — `AbstractDomainAssert` base class + hand-written assertion classes *(recommended)*

Prefab ships one base class in `prefab-test`. Users write their own `*Assert` class per domain concept
by extending it. No code generation changes are required.

```java
package be.appify.prefab.test.asserts;

import org.assertj.core.api.AbstractAssert;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractDomainAssert<SELF extends AbstractDomainAssert<SELF, ACTUAL>, ACTUAL>
        extends AbstractAssert<SELF, ACTUAL> {

    protected AbstractDomainAssert(ACTUAL actual, Class<SELF> selfType) {
        super(actual, selfType);
    }

    /**
     * Navigates into a single nested object and runs assertions on it via a typed assert instance.
     * Returns {@code this} for chaining on the parent assert.
     *
     * <pre>
     * assertThat(order)
     *     .hasShippingAddressSatisfying(addr -> addr
     *         .hasCity("Brussels")
     *         .hasZip("1000"));
     * </pre>
     */
    protected <N, NA extends AbstractDomainAssert<NA, N>> SELF nestedSatisfying(
            String description,
            N nested,
            NA nestedAssert,
            Consumer<NA> assertion
    ) {
        isNotNull();
        org.assertj.core.api.Assertions.assertThat(nested).as(description).isNotNull();
        assertion.accept(nestedAssert);
        return myself;
    }

    /**
     * Asserts that at least one element of a list satisfies the given assertion.
     * Returns {@code this} for chaining on the parent assert.
     *
     * <pre>
     * assertThat(order)
     *     .anyLineSatisfying(line -> line
     *         .hasProduct("Laptop")
     *         .hasQuantity(2));
     * </pre>
     */
    protected <N, NA extends AbstractDomainAssert<NA, N>> SELF anyInListSatisfying(
            String description,
            List<N> list,
            Function<N, NA> assertFactory,
            Consumer<NA> assertion
    ) {
        isNotNull();
        org.assertj.core.api.Assertions.assertThat(list)
                .as(description)
                .anySatisfy(item -> assertion.accept(assertFactory.apply(item)));
        return myself;
    }
}
```

Users write one assertion class per domain concept:

```java
public class OrderResponseAssert
        extends AbstractDomainAssert<OrderResponseAssert, OrderResponse> {

    public OrderResponseAssert(OrderResponse actual) {
        super(actual, OrderResponseAssert.class);
    }

    public static OrderResponseAssert assertThat(OrderResponse actual) {
        return new OrderResponseAssert(actual);
    }

    public OrderResponseAssert hasProduct(String product) {
        org.assertj.core.api.Assertions.assertThat(actual.product()).isEqualTo(product);
        return this;
    }

    public OrderResponseAssert hasQuantity(int quantity) {
        org.assertj.core.api.Assertions.assertThat(actual.quantity()).isEqualTo(quantity);
        return this;
    }

    public OrderResponseAssert hasShippingAddressSatisfying(Consumer<AddressAssert> assertion) {
        return nestedSatisfying(
                "shippingAddress",
                actual.shippingAddress(),
                new AddressAssert(actual.shippingAddress()),
                assertion);
    }

    public OrderResponseAssert anyLineSatisfying(Consumer<OrderLineAssert> assertion) {
        return anyInListSatisfying("lines", actual.lines(), OrderLineAssert::new, assertion);
    }
}
```

#### Usage — REST response

```java
var order = orderClient.getOrderById(orderId);

OrderResponseAssert.assertThat(order)
    .hasProduct("Laptop")
    .hasQuantity(2)
    .hasShippingAddressSatisfying(addr -> addr
        .hasCity("Brussels")
        .hasZip("1000"))
    .anyLineSatisfying(line -> line
        .hasProduct("Laptop")
        .hasQuantity(2));
```

#### Usage — event payload (same assertion class, no REST involved)

```java
EventConsumerAssert.assertThat(orderEventConsumer)
    .hasReceivedMessages(1)
    .within(5, TimeUnit.SECONDS)
    .where(events -> events.anySatisfy(event ->
        OrderResponseAssert.assertThat(event)
            .hasProduct("Laptop")
            .hasShippingAddressSatisfying(addr -> addr.hasCity("Brussels"))
    ));
```

#### Usage — unit test (no network or broker at all)

```java
var order = new OrderResponse("id-1", "Laptop", 2, new Address("Main St", "Brussels", "1000"), List.of());

OrderResponseAssert.assertThat(order)
    .hasProduct("Laptop")
    .hasQuantity(2);
```

#### Trade-offs

| ✅ Pros                                                                 | ❌ Cons                                               |
|------------------------------------------------------------------------|------------------------------------------------------|
| Domain-readable DSL identical for REST, events, and unit tests         | Users must write assertion classes manually          |
| Full control over assertion logic — regex, ranges, business rules      | Initial investment per domain concept                |
| Zero new code generation; no annotation processor changes              |                                                      |
| Single shared base class; small `prefab-test` footprint                |                                                      |
| Fully backwards compatible                                             |                                                      |

---

### Alternative B — Annotation processor generates `*Assert` classes per aggregate

The annotation processor generates a `PersonResponseAssert`, `OrderResponseAssert`, etc. for every
aggregate, alongside the existing test client. Each response field gets a `has*` method; each nested
record field gets a `has*Satisfying` method; each `List` field gets an `any*Satisfying` method.
Prefab still ships `AbstractDomainAssert` as the base.

```java
// Generated: OrderResponseAssert.java
public final class OrderResponseAssert
        extends AbstractDomainAssert<OrderResponseAssert, OrderResponse> {

    public static OrderResponseAssert assertThat(OrderResponse actual) {
        return new OrderResponseAssert(actual);
    }

    public OrderResponseAssert hasProduct(String product) { ... }
    public OrderResponseAssert hasQuantity(int quantity) { ... }

    public OrderResponseAssert hasShippingAddressSatisfying(Consumer<AddressAssert> assertion) {
        return nestedSatisfying("shippingAddress", actual.shippingAddress(),
                new AddressAssert(actual.shippingAddress()), assertion);
    }

    public OrderResponseAssert anyLineSatisfying(Consumer<OrderLineAssert> assertion) {
        return anyInListSatisfying("lines", actual.lines(), OrderLineAssert::new, assertion);
    }
}
```

Usage is identical to Alternative A because the base class is the same.

#### Trade-offs

| ✅ Pros                                                           | ❌ Cons                                                                                  |
|------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| No boilerplate for simple field assertions                       | Cannot generate custom assertion logic (regex, ranges, business rules)                  |
| Assertion classes stay in sync with the response model           | Users must subclass generated classes to add custom assertions — fragile base coupling  |
| Domain DSL available immediately after adding a new field        | Significantly increases generated code volume                                            |
|                                                                  | Requires new `AssertWriter` classes in the annotation processor                         |
|                                                                  | Generated classes live in `generated-sources`; IDEs may not navigate into them cleanly  |

---

### Alternative C — Plain AssertJ `satisfies()` chaining (no new framework code)

No new base class. Developers use AssertJ's built-in `satisfies()` directly:

```java
assertThat(order)
    .satisfies(o -> assertThat(o.product()).isEqualTo("Laptop"))
    .satisfies(o -> assertThat(o.quantity()).isEqualTo(2))
    .satisfies(o -> assertThat(o.shippingAddress()).satisfies(addr -> {
        assertThat(addr.city()).isEqualTo("Brussels");
        assertThat(addr.zip()).isEqualTo("1000");
    }))
    .satisfies(o -> assertThat(o.lines()).anySatisfy(line ->
        assertThat(line.product()).isEqualTo("Laptop")));
```

#### Trade-offs

| ✅ Pros                                               | ❌ Cons                                                                              |
|------------------------------------------------------|------------------------------------------------------------------------------------|
| Zero new framework code                              | No domain vocabulary — `hasCity` vs `satisfies(o -> assertThat(o.city())...)`     |
| Works today without any changes                      | Deeply nested chains become hard to read and maintain                               |
|                                                      | Cannot be reused across tests without extracting into helper methods manually       |
|                                                      | `satisfies()` chains do not compose — each assert is a standalone block             |

---

## Comparison

| Criterion                           | A — Base class + hand-written | B — Fully generated        | C — Plain satisfies()   |
|-------------------------------------|-------------------------------|----------------------------|-------------------------|
| Domain-readable DSL                 | ✅                             | ✅                          | ❌ verbose               |
| Deep nested navigation              | ✅ `nestedSatisfying`          | ✅ generated `*Satisfying`  | ✅ but unreadable at depth|
| List element navigation             | ✅ `anyInListSatisfying`       | ✅ generated `any*Satisfying`| ✅ via `anySatisfy`      |
| Transport-agnostic (REST + events)  | ✅                             | ✅                          | ✅                       |
| Custom assertion logic              | ✅ full control                | ❌ simple field equality only| ✅ inline                |
| Reusable across tests               | ✅ assertion class             | ✅ generated class          | ❌ manual extraction     |
| Generated code size                 | Tiny (1 base class)           | Large (1+ class/aggregate)  | None                    |
| No annotation processor changes     | ✅                             | ❌ new writers required     | ✅                       |
| Backwards compatible                | ✅                             | ✅                          | ✅                       |

---

## Decision

**Alternative A — `AbstractDomainAssert` base class shipped in `prefab-test`.**

### Rationale

The most important property is that the assertion DSL is **domain-first and not tied to any transport
mechanism**. Alternative A achieves this with a single base class and no code generation changes. The
`nestedSatisfying` and `anyInListSatisfying` helpers make it straightforward to express exactly the
navigation pattern shown in the design example, including deep nesting and list traversal.

Alternative B eliminates per-field boilerplate but at significant cost: it cannot generate custom
assertion logic beyond field equality, it forces users to subclass generated classes to express anything
non-trivial, and it inflates the generated code surface. The framework would need to understand how to
generate assertion classes for every field type — a problem that grows indefinitely as new types and
patterns are added.

Alternative C requires no framework changes but produces unreadable assertion code at any meaningful
depth, and the domain vocabulary is lost entirely.

### Consequences

- `AbstractDomainAssert<SELF, ACTUAL>` is added to the `prefab-test` module with two protected helpers:
  `nestedSatisfying` and `anyInListSatisfying`.
- TASK-142 acceptance criteria #1–#3 (REST coupling, `ResultActions` wrapper, generated assertion
  objects) are dropped in favour of this transport-agnostic approach.
- TASK-142 acceptance criteria #4 (`EventConsumerWhereStep` typed overload) is retained as-is: the
  existing `where(Consumer<ListAssert<V>>)` is sufficient when combined with hand-written assert classes;
  no new overload is required.
- Example assertion classes are added to at least one example module (`examples/kafka` or
  `examples/sns-sqs`) to demonstrate the pattern for both a REST response and an event payload.
- The `prefab-test` module documentation is updated with a short usage guide showing the nested and
  list navigation patterns.
- No changes are required to the annotation processor.

