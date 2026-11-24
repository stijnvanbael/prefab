# Event handling: multicast

**Budget:** 750 tokens. **Verified example:** [Order aggregate](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/ordering/Order.java), [order repository mixin](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/ordering/OrderRepositoryMixin.java), and [multicast integration test](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/ordering/OrderCancellationOnDeactivationIntegrationTest.java).

For an event that affects many aggregates, annotate an instance handler with `@EventHandler` and `@Multicast`. Set `queryMethod` to a method on the aggregate's repository mixin and map each `parameters` entry to an event property. Return the updated aggregate from each invocation.

**Rules**
- Define a derived query or `@Query` method in `@RepositoryMixin(Aggregate.class)`.
- Query only the aggregates identified by the event; keep event and query parameter names aligned.
- By default, an empty result fails and is retried (then dead-lettered) unless a matching static handler creates the aggregate. Set `failWhenEmpty = false` when no matches is a normal outcome.
- Test zero, one, and multiple matches where the behavior differs.

**Common mistake:** assuming multicast itself discovers matching records. The repository query defines the target set; use `failWhenEmpty = false` only when an empty set is expected, not to hide missing or mismatched query wiring.

Details: [event handling](../feature-guides.md#73-event-handling), [`@Multicast`](../annotation-reference.md#multicast).
