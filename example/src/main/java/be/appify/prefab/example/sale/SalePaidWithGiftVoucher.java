package be.appify.prefab.example.sale;

import be.appify.prefab.core.service.Reference;

import java.math.BigDecimal;

public record SalePaidWithGiftVoucher(Reference<GiftVoucher> giftVoucher, BigDecimal amount) {
}
