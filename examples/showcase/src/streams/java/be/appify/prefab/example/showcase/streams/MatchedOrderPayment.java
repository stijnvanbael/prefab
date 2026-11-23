package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.ordering.Order;
import java.math.BigDecimal;

/**
 * An order and a payment confirmation matched within the configured event-time window.
 *
 * @param order the matched order
 * @param orderTotal amount due
 * @param paymentAmount amount received
 */
@Event(topic = "showcase.streams.matched-order-payment")
public record MatchedOrderPayment(Reference<Order> order, BigDecimal orderTotal, BigDecimal paymentAmount)
        implements Keyed<Reference<Order>> {
    @Override
    public Reference<Order> key() {
        return order;
    }
}
