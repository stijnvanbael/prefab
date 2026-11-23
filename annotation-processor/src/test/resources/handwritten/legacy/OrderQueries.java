package handwritten.legacy;

import be.appify.prefab.core.annotations.RepositoryMixin;
import handwritten.source.Order;
import java.util.List;

@RepositoryMixin(Order.class)
public interface OrderQueries {
    List<Order> findByCustomer(String customer);
}
