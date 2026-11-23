package be.appify.prefab.example.showcase.customer;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

/**
 * A customer registered with the shop.
 *
 * <p>The event names no messaging platform: Prefab derives it from the technology on the classpath, so the same
 * source publishes to Kafka, Pub/Sub or SNS/SQS. Version 2 split the single {@code name} of version 1 into first and
 * last name; {@link CustomerRegisteredMigration} reads messages that were published as version 1.</p>
 *
 * @param customer  the customer that registered, which also keeps all events of one customer in order
 * @param firstName first name of the customer
 * @param lastName  last name of the customer
 */
@Event(topic = "showcase.customer-registered", version = "2.0.0")
public record CustomerRegistered(
        @PartitioningKey Reference<Customer> customer,
        String firstName,
        String lastName
) {
}
