# Event handling: create an aggregate

**Budget:** 750 tokens. **Verified example:** [Invoice aggregate and handlers](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/invoicing/Invoice.java) and [invoice integration tests](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/invoicing/InvoiceIntegrationTest.java).

To create an aggregate from an event, declare a `static` factory-like method on the aggregate and annotate it with `@EventHandler`. Accept the event and return the new aggregate with a fresh `Reference`.

**Rules**
- Return the aggregate to persist; do not call a repository from the handler.
- Map event data explicitly and define the initial version and state.
- Ensure the event is delivered only once or make creation idempotent using the event's business identity.

**Common mistake:** using an instance method for creation or returning `void`. A create handler is static and returns the aggregate it creates.

Details: [event handling](../feature-guides.md#73-event-handling), [`@EventHandler`](../annotation-reference.md#eventhandler).
