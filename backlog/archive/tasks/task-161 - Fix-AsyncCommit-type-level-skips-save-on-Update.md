---
id: task-161
title: "Fix @AsyncCommit at type level: @Update methods skip repository.save() and return 202"
status: "To Do"
priority: "High"
labels: ["bug", "annotation-processor", "async-commit", "reported-by:maestro"]
---

## Problem Statement

When `@AsyncCommit` is placed at the **aggregate type level**, the Prefab annotation
processor incorrectly applies async-commit semantics to **all** `@Update` methods on that
aggregate — even those that should be synchronous.

This manifests as two distinct bugs:

1. The generated controller returns `202 Accepted` instead of `200 OK` for `@Update` methods.
2. The generated service does **not** call `repository.save()` for `@Update` methods,
   so changes are silently lost.

### Minimal reproduction

```java
@Aggregate
@AsyncCommit      // ← type-level
@GetById
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        String status
) {
    @Create
    @AsyncCommit  // only the @Create should be async
    public static void create(String customerId) {
        publishEvent(new OrderPlaced(Reference.create(), customerId));
    }

    @Update
    public Order complete() {          // should be synchronous → 200
        return new Order(id, version, "COMPLETED");
    }
}
```

### Generated controller (broken)

```java
// @Update complete() — should return 200 OK with updated body
public ResponseEntity<OrderResponse> complete(String id) {
    return service.complete(id)
        .map(it -> ResponseEntity.accepted().<OrderResponse>build())  // 202 — wrong
        .orElse(ResponseEntity.notFound().build());
}
```

### Generated service (broken)

```java
public Optional<Order> complete(String id) {
    return repository.findById(id).map(aggregate -> {
        aggregate = aggregate.complete();
        return aggregate;   // ← repository.save() is missing
    });
}
```

### Current workaround (Maestro)

Moved `@AsyncCommit` from the type level to the specific `@Create` method only:

```java
@Aggregate   // no @AsyncCommit here
@GetById
public record ConversationSession(...) {

    @Create
    @AsyncCommit          // only this method is async
    public static void start(String title) { ... }

    @Update
    public ConversationSession changeStatus(SessionStatus status) { ... }  // synchronous → 200
}
```

## Expected Behaviour

| Placement | Affected methods | Expected |
|-----------|-----------------|----------|
| `@AsyncCommit` on `TYPE` | `@Create` methods | Async: return 202, no save |
| `@AsyncCommit` on `TYPE` | `@Update` methods | **Not** affected — should remain synchronous: 200 + save |
| `@AsyncCommit` on specific `@Create` | That method only | Async: return 202, no save |
| `@AsyncCommit` on specific `@Update void` | That method | Async: 202, method calls `publishEvent()` internally |

`@AsyncCommit` at the type level should be shorthand for placing it on every `@Create`
method, **not** on `@Update` methods. The documented behaviour
("On `@Update` void method: method must call `publish()` internally") requires explicit
`@AsyncCommit` placement on the specific `@Update` method.

## Proposed Fix

In the code generation for `@Update` controller and service methods, only apply
async-commit semantics when `@AsyncCommit` is present on the **method itself**, not
when it is present only at the type level.

Alternatively, clarify in the documentation that `@AsyncCommit` at type level affects
all `@Create` AND all `@Update void` methods simultaneously, and require users to place
it only on methods (removing type-level target from the annotation if that is cleaner).

## Acceptance Criteria

- [ ] `@AsyncCommit` at type level does **not** change the return code or save behaviour of `@Update` methods
- [ ] `@Update` methods on an `@AsyncCommit` aggregate still call `repository.save()` and return `200 OK`
- [ ] `@Update void` methods annotated directly with `@AsyncCommit` still return `202 Accepted`
- [ ] Processor emits a warning (or error) if `@AsyncCommit` is placed at type level — recommend method-level placement
- [ ] Integration test: `PUT /aggregates/{id}` returns 200 and persists the change when the aggregate has `@AsyncCommit @Create`
- [ ] Workaround of moving `@AsyncCommit` to method level in consuming projects can be retained as the idiomatic style

