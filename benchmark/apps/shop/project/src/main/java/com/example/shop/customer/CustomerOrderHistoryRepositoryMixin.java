package com.example.shop.customer;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import java.util.List;

/** Query used by the order-event projection and the tests. */
@RepositoryMixin(CustomerOrderHistory.class)
public interface CustomerOrderHistoryRepositoryMixin {

    /**
     * Finds the order history of a customer.
     *
     * @param customer the customer
     * @return the history of the customer
     */
    List<CustomerOrderHistory> findByCustomer(Reference<Customer> customer);
}
