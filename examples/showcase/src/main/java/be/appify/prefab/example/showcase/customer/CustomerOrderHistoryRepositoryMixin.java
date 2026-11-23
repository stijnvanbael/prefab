package be.appify.prefab.example.showcase.customer;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import java.util.List;

/** Query that {@link CustomerOrderHistory} uses to find the histories an order event is delivered to. */
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
