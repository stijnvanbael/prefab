# Publishing events

**Budget:** 750 tokens. **Verified example:** [Customer aggregate](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/customer/Customer.java), [event contract](../../../examples/showcase/src/main/java/be/appify/prefab/example/showcase/customer/CustomerRegistered.java), and [event integration test](../../../examples/showcase/src/test/java/be/appify/prefab/example/showcase/customer/CustomerIntegrationTest.java).

Declare a record with `@Event(topic = "...")`. Bind one publisher handle as a `static final EventPublisher<E>` on the aggregate, then publish the event after the state transition in its create or update method.

**Rules**
- Include the aggregate reference in the event when consumers need to correlate it.
- Put `@PartitioningKey` on the stable key used to order related events.
- For code-first Avro events, the schema namespace defaults to the Java package; use `@AvroSchema(namespace = "...")` to override it.
- Test the event payload and the state transition that caused publication.

**Common mistake:** using deprecated `PublishesEvents` or publishing directly through `DomainEventPublisher`. Use `EventPublisher.bind(Event.class)` and its `publish` method.

Details: [event publishing](../feature-guides.md#72-event-publishing), [event reference](../annotation-reference.md#event).
