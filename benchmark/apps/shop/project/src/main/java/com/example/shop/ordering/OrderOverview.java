package com.example.shop.ordering;

import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.ViewOf;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;

/**
 * Screen-oriented view of an {@link Order}, served by {@code GET /order-overviews}. It is bound to the aggregate with
 * {@link ViewOf}, so it needs no repository, and exposes only what an overview shows plus a derived field.
 *
 * @param order       the order
 * @param status      where the order is in its life cycle
 * @param quantity    how many items were ordered
 * @param total       what the customer has to pay
 * @param cancellable whether the customer can still cancel the order
 */
@ViewOf(Order.class)
@GetList(security = @Security(role = "ADMIN"))
public record OrderOverview(
        Reference<Order> order,
        OrderStatus status,
        int quantity,
        BigDecimal total,
        boolean cancellable
) {
    /**
     * Derives the overview from an order.
     *
     * @param order the order
     */
    public OrderOverview(Order order) {
        this(order.id(), order.status(), order.quantity(), order.total(), order.status() == OrderStatus.PLACED);
    }
}
