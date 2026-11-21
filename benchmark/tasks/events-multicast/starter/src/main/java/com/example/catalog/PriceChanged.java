package com.example.catalog;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;

@Event(topic = "prices")
public record PriceChanged(@PartitioningKey Reference<Product> product, BigDecimal price) {
}
