package mongodb.indexed;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Indexed;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @Filter @NotNull String name,
        @Indexed(unique = true) @NotNull String sku,
        @NotNull String description
) {
    public Product(@NotNull String name, @NotNull String sku, @NotNull String description) {
        this(Reference.create(), 0L, name, sku, description);
    }
}
