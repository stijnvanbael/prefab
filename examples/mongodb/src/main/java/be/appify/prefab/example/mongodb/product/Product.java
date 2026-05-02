package be.appify.prefab.example.mongodb.product;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Outbox;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.mongodb.category.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * Represents a product aggregate in the MongoDB example.
 * The embedded {@link Price} record demonstrates MongoDB's native document nesting, and the
 * {@link Reference} to {@link Category} shows how cross-aggregate references are stored as plain
 * strings by the {@code ReferenceToStringConverter}.
 */
@Aggregate
@GetById
@GetList
@Delete
@Outbox
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @Filter String name,
        String description,
        Price price,
        Reference<Category> category
) implements PublishesEvents {

    /**
     * Creates a new product. The {@code amount} and {@code currency} parameters are flattened so
     * that callers do not need to construct a {@link Price} object — MongoDB stores them as a
     * nested sub-document automatically.
     */
    @Create
    public Product(
            @NotBlank String name,
            @NotBlank String description,
            @NotNull BigDecimal amount,
            @NotBlank String currency,
            @NotNull Reference<Category> category
    ) {
        this(Reference.create(), 0L, name, description, new Price(amount, currency), category);
        publish(new ProductCreated(id, category));
    }

    /** Returns an updated product with the new name and description. */
    @Update
    public Product update(
            @NotBlank String name,
            @NotBlank String description
    ) {
        return new Product(id, version, name, description, price, category);
    }
}
