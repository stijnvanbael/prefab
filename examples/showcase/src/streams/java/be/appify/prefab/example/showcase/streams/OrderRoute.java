package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.ordering.Order;

/**
 * An order event routed by its lifecycle type.
 *
 * @param order order identifier
 * @param orderType {@code placed} or {@code cancelled}
 */
@Event(topic = "showcase.streams.order-route")
public record OrderRoute(Reference<Order> order, String orderType) implements Keyed<Reference<Order>> {
    @Override
    public Reference<Order> key() {
        return order;
    }
}
