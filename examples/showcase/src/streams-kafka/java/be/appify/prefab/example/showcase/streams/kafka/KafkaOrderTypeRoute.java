package be.appify.prefab.example.showcase.streams.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.ordering.Order;

/**
 * An order route re-keyed by its type in a native Kafka Streams fragment.
 *
 * @param orderType order lifecycle type
 * @param order order identifier
 */
@Event(topic = "showcase.streams.order-type-route")
public record KafkaOrderTypeRoute(String orderType, Reference<Order> order) implements Keyed<String> {
    @Override
    public String key() {
        return orderType;
    }
}
