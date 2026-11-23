package com.example.ordering;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OrderLine(@NotNull String product, int quantity, @NotNull BigDecimal unitPrice) {
}
