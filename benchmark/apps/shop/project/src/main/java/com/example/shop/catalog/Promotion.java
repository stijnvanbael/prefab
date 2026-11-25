package com.example.shop.catalog;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * A promotion that customers redeem with a code. The kinds of promotion are subtypes of a sealed interface: Prefab
 * persists them together with a type discriminator and exposes them through one polymorphic REST resource.
 */
@Aggregate
@GetById
@GetList
public sealed interface Promotion permits Promotion.PercentageOff, Promotion.AmountOff {

    /**
     * The code customers enter to redeem the promotion.
     *
     * @return the promotion code
     */
    String code();

    /**
     * A discount of a percentage of the price.
     *
     * @param id         unique reference of the promotion
     * @param version    optimistic locking version
     * @param code       promotion code
     * @param percentage discount percentage
     */
    record PercentageOff(
            @Id Reference<Promotion> id,
            @Version long version,
            @NotBlank @Size(max = 50) String code,
            @Positive @Max(100) int percentage
    ) implements Promotion {
        /**
         * Creates a percentage discount.
         *
         * @param code       promotion code
         * @param percentage discount percentage
         */
        @Create
        public PercentageOff(@NotBlank @Size(max = 50) String code, @Positive @Max(100) int percentage) {
            this(Reference.create(), 0L, code, percentage);
        }
    }

    /**
     * A fixed amount off the price.
     *
     * @param id      unique reference of the promotion
     * @param version optimistic locking version
     * @param code    promotion code
     * @param amount  discount amount
     */
    record AmountOff(
            @Id Reference<Promotion> id,
            @Version long version,
            @NotBlank @Size(max = 50) String code,
            @NotNull @Positive BigDecimal amount
    ) implements Promotion {
        /**
         * Creates a fixed amount discount.
         *
         * @param code   promotion code
         * @param amount discount amount
         */
        @Create
        public AmountOff(@NotBlank @Size(max = 50) String code, @NotNull @Positive BigDecimal amount) {
            this(Reference.create(), 0L, code, amount);
        }
    }
}
