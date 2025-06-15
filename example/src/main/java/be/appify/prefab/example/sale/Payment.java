package be.appify.prefab.example.sale;

import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record Payment(
    @NotNull BigDecimal amount,
    @NotNull PaymentMethod method,
    Reference<GiftVoucher> giftVoucher
) {
}
