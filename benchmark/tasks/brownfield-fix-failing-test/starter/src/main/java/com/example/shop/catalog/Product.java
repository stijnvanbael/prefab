package com.example.shop.catalog;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Text;
import be.appify.prefab.core.annotations.rest.Autocomplete;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.Download;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.SortOrder;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.annotations.validation.ContentType;
import be.appify.prefab.core.annotations.validation.FileSize;
import be.appify.prefab.core.audit.AuditInfo;
import be.appify.prefab.core.domain.Binary;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import static be.appify.prefab.core.annotations.rest.Filter.Operator.EQUAL;
import static be.appify.prefab.core.annotations.rest.Filter.Operator.FULL_TEXT;
import static be.appify.prefab.core.annotations.rest.Filter.Operator.GREATER_THAN_OR_EQUAL;
import static be.appify.prefab.core.annotations.rest.Filter.Operator.LESS_THAN_OR_EQUAL;
import static be.appify.prefab.core.annotations.rest.Filter.Weight.A;

/**
 * A product in the shop catalog.
 *
 * <p>{@code GET /products} lists products by name unless the request sorts otherwise, and filters them on a
 * full-text {@code query} over name and description, on brand and category (equality) and on a price range.</p>
 *
 * @param id          unique reference of the product
 * @param version     optimistic locking version, incremented on every update
 * @param name        display name; weighs most in the full-text search
 * @param description optional long description, stored as unbounded text
 * @param brand       brand name, offered as type-ahead suggestion through {@code GET /products/brand/autocomplete}
 * @param category    the category the product belongs to
 * @param price       unit price
 * @param image       optional product image, served by {@code GET /products/{id}/image}
 * @param audit       when and by whom the product was created and last modified, maintained by Prefab
 */
@Aggregate
@GetById
@GetList(sorted = @SortOrder(property = "name"))
@Delete(security = @Security(authority = "ROLE_ADMIN"))
public record Product(
        @Id Reference<Product> id,
        @Version long version,
        @NotBlank @Size(max = 255)
        @Filter(operator = FULL_TEXT, parameter = "query", weight = A) String name,
        @Nullable @Text
        @Filter(operator = FULL_TEXT, parameter = "query") String description,
        @NotBlank @Size(max = 100)
        @Filter(operator = EQUAL) @Autocomplete String brand,
        @NotNull @Filter(operator = EQUAL) Reference<Category> category,
        @NotNull @Positive
        @Filter(operator = GREATER_THAN_OR_EQUAL)
        @Filter(operator = LESS_THAN_OR_EQUAL) BigDecimal price,
        @Nullable @Download Binary image,
        AuditInfo audit
) {
    private static final EventPublisher<ProductDiscontinued> DISCONTINUED =
            EventPublisher.bind(ProductDiscontinued.class);

    /** Longest accepted description. The column is unbounded ({@code @Text}); the API sets the limit. */
    public static final int MAX_DESCRIPTION_LENGTH = 2000;

    /** Largest accepted product image: 1 MB. */
    public static final int MAX_IMAGE_SIZE = 1024 * 1024;

    /**
     * Adds a new product to the catalog, without an image.
     *
     * @param name        display name
     * @param description optional long description
     * @param brand       brand name
     * @param category    the category the product belongs to
     * @param price       unit price
     */
    @Create(security = @Security(role = "ADMIN"))
    public Product(
            @NotBlank @Size(max = 255) String name,
            @Nullable @Size(max = MAX_DESCRIPTION_LENGTH) String description,
            @NotBlank @Size(max = 100) String brand,
            @NotNull Reference<Category> category,
            @NotNull @Positive BigDecimal price
    ) {
        this(Reference.create(), 0L, name, description, brand, category, price, null, new AuditInfo());
    }

    /**
     * Changes the name and description of the product.
     *
     * @param name        new display name
     * @param description new long description
     * @return the updated product
     */
    @Update(security = @Security(role = "ADMIN"))
    public Product describe(
            @NotBlank @Size(max = 255) String name,
            @Nullable @Size(max = MAX_DESCRIPTION_LENGTH) String description
    ) {
        return new Product(id, version, name, description, brand, category, price, image, audit);
    }

    /**
     * Changes the unit price.
     *
     * @param price new unit price
     * @return the updated product
     */
    @Update(path = "/price", security = @Security(role = "ADMIN"))
    public Product reprice(@NotNull @Positive BigDecimal price) {
        return new Product(id, version, name, description, brand, category, price, image, audit);
    }

    /**
     * Replaces the product image.
     *
     * @param image a PNG or JPEG image of at most {@link #MAX_IMAGE_SIZE} bytes
     * @return the updated product
     */
    @Update(path = "/image", security = @Security(role = "ADMIN"))
    public Product changeImage(
            @NotNull @ContentType({"image/png", "image/jpeg"}) @FileSize(max = MAX_IMAGE_SIZE) Binary image
    ) {
        return new Product(id, version, name, description, brand, category, price, image, audit);
    }

    /**
     * Takes the product out of the catalog for good. Inventory writes off its remaining stock.
     *
     * @return the product, unchanged
     */
    @Update(path = "/discontinuation", method = "POST", security = @Security(role = "ADMIN"))
    public Product discontinue() {
        DISCONTINUED.publish(new ProductDiscontinued(id));
        return this;
    }
}
