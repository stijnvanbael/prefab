package rest.aggregate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Order(
        @Id String id,
        @Version long version,
        String description,
        Reference<Product> product) {

    @Create
    public Order(String description, Product product) {
        this(UUID.randomUUID().toString(), 0L, description, new Reference<>(product.id()));
    }

    @Update
    public Order assignProduct(Product product) {
        return new Order(id, version, description, new Reference<>(product.id()));
    }
}

