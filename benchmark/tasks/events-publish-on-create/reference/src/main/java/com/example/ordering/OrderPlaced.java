package com.example.ordering;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;

@Event(topic = "orders")
public record OrderPlaced(@PartitioningKey Reference<Order> order, String customer, BigDecimal total) {
}
