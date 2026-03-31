package be.appify.prefab.example.mongodb.product;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/** Represents a product aggregate in the MongoDB example. */
@Aggregate
@GetById
@GetList
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @NotNull String name,
        @NotNull String description
) {
    /** Creates a new product with the given name and description. */
    @Create
    public Product(String name, String description) {
        this(Reference.create(), 0L, name, description);
    }

    /** Returns an updated product with the new name and description. */
    @Update(path = "")
    public Product update(String name, String description) {
        return new Product(id, version, name, description);
    }
}
