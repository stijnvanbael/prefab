package be.appify.prefab.example.sale;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaleItem(
    @NotNull String description,
    @NotNull BigDecimal quantity,
    @NotNull BigDecimal price
) {
}
