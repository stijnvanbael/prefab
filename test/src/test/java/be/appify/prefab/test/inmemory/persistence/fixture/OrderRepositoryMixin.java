package be.appify.prefab.test.inmemory.persistence.fixture;

import be.appify.prefab.core.service.Reference;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;

/** Mixin with a derived query on a reference and a query that only works on PostgreSQL. */
public interface OrderRepositoryMixin {

    long countByCustomer(Reference<Customer> customer);

    List<Order> findByCategoryOrderByNameAsc(String category);

    @Query("SELECT * FROM orders WHERE amount > 100")
    List<Order> findBigOrders();
}
