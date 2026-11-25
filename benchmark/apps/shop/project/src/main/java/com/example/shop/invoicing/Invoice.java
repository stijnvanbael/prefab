package com.example.shop.invoicing;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import com.example.shop.customer.Customer;
import com.example.shop.ordering.Order;
import com.example.shop.ordering.OrderEvent;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import static be.appify.prefab.core.annotations.rest.Filter.Operator.EQUAL;

/**
 * The invoice of an order. It has no create endpoint: an invoice is created by an event handler when an order is
 * placed, and voided when the order is cancelled.
 *
 * <p>Paying is {@link AsyncCommit asynchronous}: the endpoint only publishes {@link InvoicePaid} and answers
 * {@code 202 Accepted}; the invoice is saved when it receives its own event.</p>
 *
 * @param id       unique reference of the invoice
 * @param version  optimistic locking version
 * @param order    the invoiced order
 * @param customer the customer that has to pay
 * @param amount   the amount due
 * @param status   where the invoice is in its life cycle
 */
@Aggregate
@GetById
@GetList
public record Invoice(
        @Id Reference<Invoice> id,
        @Version long version,
        @Filter(operator = EQUAL) Reference<Order> order,
        Reference<Customer> customer,
        BigDecimal amount,
        InvoiceStatus status
) {
    private static final EventPublisher<InvoicePaid> PAID = EventPublisher.bind(InvoicePaid.class);

    /**
     * Creates the invoice of a placed order.
     *
     * @param event the placed order
     * @return a new open invoice
     */
    @EventHandler
    public static Invoice onOrderPlaced(OrderEvent.Placed event) {
        return new Invoice(Reference.create(), 0L, event.order(), event.customer(), event.total(), InvoiceStatus.OPEN);
    }

    /**
     * Voids the invoice of a cancelled order.
     *
     * @param event the cancelled order
     * @return the voided invoice
     */
    @EventHandler
    @Multicast(queryMethod = "findByOrder", parameters = "order")
    public Invoice onOrderCancelled(OrderEvent.Cancelled event) {
        return new Invoice(id, version, order, customer, amount, InvoiceStatus.VOID);
    }

    /**
     * Pays the invoice. Nothing is saved here; the invoice changes when it handles {@link InvoicePaid}.
     */
    @Update(path = "/payment", method = "POST")
    @AsyncCommit
    public void pay() {
        PAID.publish(new InvoicePaid(id, amount));
    }

    /**
     * Marks an open invoice as paid.
     *
     * @param event the payment
     * @return the paid invoice
     */
    @EventHandler
    @ByReference(property = "invoice")
    public Invoice onInvoicePaid(InvoicePaid event) {
        return status == InvoiceStatus.OPEN
                ? new Invoice(id, version, order, customer, amount, InvoiceStatus.PAID)
                : this;
    }
}
