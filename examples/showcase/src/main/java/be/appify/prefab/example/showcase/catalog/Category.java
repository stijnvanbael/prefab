package be.appify.prefab.example.showcase.catalog;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.SortOrder;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * A category that groups products in the shop catalog.
 *
 * @param id      unique reference of the category
 * @param version optimistic locking version
 * @param name    display name, offered as type-ahead suggestion through {@code GET /categories/name/autocomplete}
 */
@Aggregate
@GetById
@GetList(sorted = @SortOrder(property = "name"))
public record Category(
        @Id Reference<Category> id,
        @Version long version,
        @NotBlank @Size(max = 100) @Autocomplete String name
) {
    /**
     * Adds a new category to the catalog.
     *
     * @param name display name of the category
     */
    @Create
    public Category(@NotBlank @Size(max = 100) String name) {
        this(Reference.create(), 0L, name);
    }
}
