package handwritten.legacy;

import handwritten.source.Order;
import org.springframework.data.repository.CrudRepository;

public interface LegacyOrderRepository extends CrudRepository<Order, String> {
}
