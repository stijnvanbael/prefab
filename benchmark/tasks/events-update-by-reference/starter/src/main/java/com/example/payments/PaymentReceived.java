package com.example.payments;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import com.example.ordering.Order;
import java.math.BigDecimal;

@Event(topic = "payments")
public record PaymentReceived(@PartitioningKey Reference<Order> order, BigDecimal amount) {
}
