package com.example.ordering;

import be.appify.prefab.core.annotations.RepositoryMixin;
import java.util.List;

@RepositoryMixin(Order.class)
public interface OrderRepositoryMixin {
    List<Order> findByCustomerEmail(String customerEmail);

    long countByStatus(OrderStatus status);
}
