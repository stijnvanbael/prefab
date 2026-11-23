package be.appify.prefab.example.showcase.ordering;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.catalog.Product;
import be.appify.prefab.example.showcase.customer.Customer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;

/**
 * Everything that happens to an order, as one sealed hierarchy on one topic.
 *
 * <p>The events of one order share a partitioning key, so a consumer sees them in the order they were published.
 * The event names no messaging platform; Prefab derives it from the classpath.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "showcase.order")
public sealed interface OrderEvent extends Keyed<Reference<Order>> permits OrderEvent.Placed, OrderEvent.Cancelled {

    /**
     * The order the event is about.
     *
     * @return the order, which is also the partitioning key
     */
    @PartitioningKey
    Reference<Order> order();

    /**
     * Uses the order reference as the stream record key, matching the event partitioning key.
     *
     * @return the order reference
     */
    @Override
    default Reference<Order> key() {
        return order();
    }

    /**
     * A customer placed an order.
     *
     * @param order    the new order
     * @param customer the customer that placed it
     * @param product  the product that was ordered
     * @param quantity how many items were ordered
     * @param total    what the customer has to pay
     */
    record Placed(
            Reference<Order> order,
            Reference<Customer> customer,
            Reference<Product> product,
            int quantity,
            BigDecimal total
    ) implements OrderEvent {
    }

    /**
     * An order was cancelled before it was delivered.
     *
     * @param order    the cancelled order
     * @param customer the customer that placed it
     * @param product  the product that was ordered
     * @param quantity how many items were ordered
     */
    record Cancelled(
            Reference<Order> order,
            Reference<Customer> customer,
            Reference<Product> product,
            int quantity
    ) implements OrderEvent {
    }
}
