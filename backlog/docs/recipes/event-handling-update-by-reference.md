# Event handling: update by reference

**Budget:** 750 tokens. **Verified example:** [Invoice aggregate](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/invoicing/Invoice.java) and [invoice integration tests](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/invoicing/InvoiceIntegrationTest.java).

Use an instance `@EventHandler` with `@ByReference(property = "...")` when the event identifies one existing aggregate. The property names the event accessor containing that aggregate's reference. Return the updated aggregate.

**Rules**
- Match the reference property to the event component exactly.
- Preserve the current identity and unchanged fields.
- Make handlers safe for redelivery; do not regress a state that has already advanced.

**Common mistake:** treating a reference update like a static create handler or naming an unrelated event property. Check the event record and the matching `@ByReference` declaration together.

Details: [event handling](../feature-guides.md#73-event-handling), [`@ByReference`](../annotation-reference.md#byreference).
