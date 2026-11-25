package com.example.ordering;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;

@Event(topic = "orders")
public record OrderPlaced(Reference<Order> order, String customer, BigDecimal total) implements Keyed<String> {
    @Override
    public String key() {
        return order.id();
    }
}
