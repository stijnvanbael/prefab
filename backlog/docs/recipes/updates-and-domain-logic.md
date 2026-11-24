# Updates and domain logic

**Budget:** 750 tokens. **Verified example:** [Order aggregate](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/ordering/Order.java) and [update integration tests](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/ordering/OrderIntegrationTest.java).

Keep invariants in aggregate methods. Annotate an operation with `@Update`; validate its arguments, reject invalid transitions, and return a new aggregate carrying the changed state. Use `path` and `method` when the action deserves a distinct endpoint.

**Rules**
- Preserve `id`, `version`, and unchanged fields when constructing the next state.
- Make repeated commands safe where retries are possible.
- Add tests for both the accepted transition and the rejected or repeated case.

**Common mistake:** mutating state or returning `void`. Return the new state from a normal `@Update`; the legacy void form is deprecated. `@AsyncCommit` is a distinct event-driven pattern—see [the update reference](../annotation-reference.md#update) before using it.

Details: [REST CRUD operations](../feature-guides.md#71-rest-crud-operations), [`@Update`](../annotation-reference.md#update).
