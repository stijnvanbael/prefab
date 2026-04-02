package be.appify.prefab.example.mongodb.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Represents a monetary price with an amount and a currency code. */
public record Price(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency
) {
}
