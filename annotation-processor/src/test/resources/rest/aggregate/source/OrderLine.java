package rest.aggregate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.service.Reference;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record OrderLine(
        @Id String id,
        @Version long version,
        @Parent Reference<Order> order,
        Reference<Product> product,
        int quantity) {

    @Create
    public OrderLine(Order order, Product product, int quantity) {
        this(UUID.randomUUID().toString(), 0L, new Reference<>(order.id()), new Reference<>(product.id()), quantity);
    }
}

