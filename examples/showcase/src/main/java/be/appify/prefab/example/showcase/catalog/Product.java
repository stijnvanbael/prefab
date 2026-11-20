package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * A product in the shop catalog.
 *
 * @param id      unique reference of the product
 * @param version optimistic locking version
 * @param name    display name of the product
 */
@Aggregate
@GetById
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @NotBlank @Size(max = 255) String name
) {
    /**
     * Adds a new product to the catalog.
     *
     * @param name display name of the product
     */
    @Create
    public Product(@NotBlank @Size(max = 255) String name) {
        this(Reference.create(), 0L, name);
    }
}
