package com.example.shop.customer;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.service.Reference;
import com.example.shop.ordering.OrderEvent;
import com.example.shop.ordering.OrderStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * Read model of the orders of one customer. It has no create endpoint and no writes of its own: it is built from the
 * {@link CustomerRegistered} and {@link OrderEvent} events, which makes it a projection that can be rebuilt by
 * replaying them.
 *
 * <p>Both order events are handled by one generated consumer, so they are applied in the order they were
 * published.</p>
 *
 * @param id       unique reference of the history
 * @param version  optimistic locking version
 * @param customer the customer the history belongs to
 * @param orders   the orders of the customer, oldest first
 */
@Aggregate
public record CustomerOrderHistory(
        @Id Reference<CustomerOrderHistory> id,
        @Version long version,
        Reference<Customer> customer,
        List<Entry> orders
) {

    /**
     * One order in the history.
     *
     * @param order  the order
     * @param status where the order is in its life cycle
     */
    public record Entry(Reference<com.example.shop.ordering.Order> order, OrderStatus status) {
    }

    /**
     * Starts an empty history for a new customer.
     *
     * @param event the registered customer
     * @return an empty history
     */
    @EventHandler
    public static CustomerOrderHistory onCustomerRegistered(CustomerRegistered event) {
        return new CustomerOrderHistory(Reference.create(), 0L, event.customer(), List.of());
    }

    /**
     * Adds a placed order to the history.
     *
     * @param event the placed order
     * @return the history with the order added
     */
    @EventHandler
    @Multicast(queryMethod = "findByCustomer", parameters = "customer")
    public CustomerOrderHistory onOrderPlaced(OrderEvent.Placed event) {
        var updated = new ArrayList<>(orders);
        updated.add(new Entry(event.order(), OrderStatus.PLACED));
        return new CustomerOrderHistory(id, version, customer, List.copyOf(updated));
    }

    /**
     * Marks an order in the history as cancelled.
     *
     * @param event the cancelled order
     * @return the history with the order cancelled
     */
    @EventHandler
    @Multicast(queryMethod = "findByCustomer", parameters = "customer")
    public CustomerOrderHistory onOrderCancelled(OrderEvent.Cancelled event) {
        var updated = orders.stream()
                .map(entry -> entry.order().equals(event.order()) ? new Entry(entry.order(), OrderStatus.CANCELLED) : entry)
                .toList();
        return new CustomerOrderHistory(id, version, customer, updated);
    }
}
