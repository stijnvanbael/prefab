package com.example.shop.invoicing;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import com.example.shop.ordering.Order;
import java.util.List;

/** Query that {@link Invoice} uses to find the invoices an order event is delivered to. */
@RepositoryMixin(Invoice.class)
public interface InvoiceRepositoryMixin {

    /**
     * Finds the invoices of an order.
     *
     * @param order the order
     * @return the invoices of the order
     */
    List<Invoice> findByOrder(Reference<Order> order);
}
