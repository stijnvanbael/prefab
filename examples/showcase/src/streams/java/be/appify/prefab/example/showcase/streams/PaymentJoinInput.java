package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.ordering.Order;
import java.math.BigDecimal;

/**
 * An order amount normalized for the stream-stream join.
 *
 * @param order order identifier used as the join key
 * @param amount amount used to compare placed orders and payments
 */
@Event(topic = "showcase.streams.payment-join-input")
public record PaymentJoinInput(Reference<Order> order, BigDecimal amount) implements Keyed<Reference<Order>> {
    @Override
    public Reference<Order> key() {
        return order;
    }
}
