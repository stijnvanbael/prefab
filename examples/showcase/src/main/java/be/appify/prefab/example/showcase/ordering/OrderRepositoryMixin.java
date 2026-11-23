package be.appify.prefab.example.showcase.ordering;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.customer.Customer;
import java.util.List;

/** Query that {@link Order#onCustomerDeactivated} uses to find the orders an event is delivered to. */
@RepositoryMixin(Order.class)
public interface OrderRepositoryMixin {

    /**
     * Finds the orders of a customer.
     *
     * @param customer the customer
     * @return all orders of the customer
     */
    List<Order> findByCustomer(Reference<Customer> customer);
}
