package be.appify.prefab.example.kafka.product;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @NotNull @Size(max = 255) String name,
        ProductDetails details,
        List<ProductTag> tags
) {
    @Create
    public Product(@NotNull @Size(max = 255) String name, @NotNull ProductDetails details) {
        this(Reference.create(), 0L, name, details, new ArrayList<>());
    }

    @Update(path = "/tags", method = "POST")
    public void addTag(@NotNull ProductTag tag) {
        tags.add(tag);
    }
}
