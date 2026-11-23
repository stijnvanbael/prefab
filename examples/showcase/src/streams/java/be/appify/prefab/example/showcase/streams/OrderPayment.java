package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.ordering.Order;
import java.math.BigDecimal;

/**
 * A payment confirmation projected with its order reference for stream matching.
 *
 * @param order the order being paid
 * @param amount amount received
 */
@Event(topic = "showcase.streams.order-payment")
public record OrderPayment(Reference<Order> order, BigDecimal amount) implements Keyed<Reference<Order>> {
    @Override
    public Reference<Order> key() {
        return order;
    }
}
