package com.example.shop.catalog;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.annotations.rest.SortOrder;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * A variant of a product, such as a colour or size. Variants are a nested resource of their product:
 * {@code /products/{productId}/product-variants}.
 *
 * @param id      unique reference of the variant
 * @param version optimistic locking version
 * @param product the product this is a variant of
 * @param sku     stock keeping unit
 * @param label   what distinguishes this variant, e.g. "Red, XL"
 */
@Aggregate
@GetById
@GetList(sorted = @SortOrder(property = "sku"))
public record ProductVariant(
        @Id Reference<ProductVariant> id,
        @Version long version,
        @Parent @NotNull Reference<Product> product,
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 100) String label
) {
    /**
     * Adds a variant to a product.
     *
     * @param product the product this is a variant of
     * @param sku     stock keeping unit
     * @param label   what distinguishes this variant
     */
    @Create
    public ProductVariant(
            @NotNull Reference<Product> product,
            @NotBlank @Size(max = 50) String sku,
            @NotBlank @Size(max = 100) String label
    ) {
        this(Reference.create(), 0L, product, sku, label);
    }
}
