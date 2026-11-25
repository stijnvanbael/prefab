package com.example.shop.catalog;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import com.example.shop.customer.Customer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * A customer's review of a product, a nested resource of the product: {@code /products/{productId}/reviews}.
 *
 * @param id       unique reference of the review
 * @param version  optimistic locking version
 * @param product  the reviewed product
 * @param customer the customer who wrote the review
 * @param rating   from 1 (poor) to 5 (excellent)
 * @param comment  optional text
 */
@Aggregate
@GetById
@GetList
public record Review(
        @Id Reference<Review> id,
        @Version long version,
        @Parent @NotNull Reference<Product> product,
        @NotNull Reference<Customer> customer,
        @Min(1) @Max(5) int rating,
        @Nullable @Size(max = 1000) String comment
) {
    private static final EventPublisher<ReviewPosted> POSTED = EventPublisher.bind(ReviewPosted.class);

    /**
     * Posts a review and publishes {@link ReviewPosted}, which updates the rating of the product.
     *
     * @param product  the reviewed product
     * @param customer the customer who wrote the review
     * @param rating   from 1 to 5
     * @param comment  optional text
     */
    @Create
    public Review(
            @NotNull Reference<Product> product,
            @NotNull Reference<Customer> customer,
            @Min(1) @Max(5) int rating,
            @Nullable @Size(max = 1000) String comment
    ) {
        this(Reference.create(), 0L, product, customer, rating, comment);
        POSTED.publish(new ReviewPosted(product, id, rating));
    }
}
