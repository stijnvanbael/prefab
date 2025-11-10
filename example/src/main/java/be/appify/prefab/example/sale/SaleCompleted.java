package be.appify.prefab.example.sale;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Key;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static be.appify.prefab.core.annotations.Event.Platform.KAFKA;

@Event(topic = "${kafka.topics.sale.name}", platform = KAFKA, ownedBy = Sale.class)
public record SaleCompleted(
        @Key String id,
        Instant start,
        List<SaleItem> items,
        BigDecimal returned,
        SaleType type
) {
}
