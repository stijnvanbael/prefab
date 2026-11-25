package com.example.shop.invoicing;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;

/**
 * An invoice was paid. The invoice itself listens to this event to change its status ({@code @AsyncCommit}).
 *
 * @param invoice the paid invoice
 * @param amount  the amount that was paid
 */
@Event(topic = "shop.invoice-paid")
public record InvoicePaid(@PartitioningKey Reference<Invoice> invoice, BigDecimal amount) {
}
