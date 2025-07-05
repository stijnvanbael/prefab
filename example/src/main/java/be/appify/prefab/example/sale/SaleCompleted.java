package be.appify.prefab.example.sale;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Key;
import be.appify.prefab.core.service.Reference;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static be.appify.prefab.core.annotations.Event.Platform.PUB_SUB;

@Event(topic = "${pubsub.topics.sale.name}", platform = PUB_SUB)
public record SaleCompleted(
        @Key String id,
        Instant start,
        List<SaleItem> items,
        List<Payment> payments,
        BigDecimal returned,
        State state,
        Reference<Customer> customer,
        SaleType type
) {
}
