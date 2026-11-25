package com.example.shop.ordering;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import com.example.shop.catalog.Product;
import com.example.shop.customer.Customer;
import com.example.shop.customer.CustomerDeactivated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import static be.appify.prefab.core.annotations.rest.Filter.Operator.EQUAL;

/**
 * An order of a customer for a quantity of one product.
 *
 * <p>Placing and cancelling an order publish {@link OrderEvent events}; stock, invoices and the order history of the
 * customer react to them. Closing the account of a customer cancels all their orders through a {@link Multicast}.</p>
 *
 * @param id       unique reference of the order
 * @param version  optimistic locking version
 * @param customer the customer that placed the order
 * @param product  the ordered product
 * @param quantity how many items were ordered
 * @param total    unit price times quantity
 * @param status   where the order is in its life cycle
 */
@Aggregate
@GetById(security = @Security(role = "ADMIN"))
@GetList(security = @Security(role = "ADMIN"))
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        @NotNull @Filter(operator = EQUAL) Reference<Customer> customer,
        @NotNull Reference<Product> product,
        @Positive int quantity,
        @NotNull BigDecimal total,
        @NotNull OrderStatus status
) {
    private static final EventPublisher<OrderEvent> EVENTS = EventPublisher.bind(OrderEvent.class);

    /**
     * Places an order and publishes {@link OrderEvent.Placed}.
     *
     * @param customer  the customer that places the order
     * @param product   the product to order
     * @param quantity  how many items to order
     * @param unitPrice the price of one item
     */
    @Create
    public Order(
            @NotNull Reference<Customer> customer,
            @NotNull Reference<Product> product,
            @Positive int quantity,
            @NotNull @Positive BigDecimal unitPrice
    ) {
        this(Reference.create(), 0L, customer, product, quantity,
                unitPrice.multiply(BigDecimal.valueOf(quantity)), OrderStatus.PLACED);
        EVENTS.publish(new OrderEvent.Placed(id, customer, product, quantity, total));
    }

    /**
     * Cancels the order and publishes {@link OrderEvent.Cancelled}. Cancelling a cancelled order does nothing.
     *
     * @return the cancelled order
     */
    @Update(path = "/cancellation", method = "POST")
    public Order cancel() {
        if (status == OrderStatus.CANCELLED) {
            return this;
        }
        EVENTS.publish(new OrderEvent.Cancelled(id, customer, product, quantity));
        return new Order(id, version, customer, product, quantity, total, OrderStatus.CANCELLED);
    }

    /**
     * Cancels the order when its customer closes the account. The event is delivered to every order that the query
     * method returns for the {@code customer} of the event.
     *
     * @param event the customer that was deactivated
     * @return the cancelled order
     */
    @EventHandler
    @Multicast(queryMethod = "findByCustomer", parameters = "customer", failWhenEmpty = false)
    public Order onCustomerDeactivated(CustomerDeactivated event) {
        return status == OrderStatus.CANCELLED
                ? this
                : new Order(id, version, customer, product, quantity, total, OrderStatus.CANCELLED);
    }
}
