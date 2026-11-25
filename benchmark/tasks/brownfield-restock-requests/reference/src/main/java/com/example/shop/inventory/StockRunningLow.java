package com.example.shop.inventory;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import com.example.shop.catalog.Product;

/**
 * Published when the available stock of a product drops below {@link StockLevel#LOW_STOCK}.
 *
 * @param product   the product
 * @param available the items still available
 */
@Event(topic = "shop.stock-running-low")
public record StockRunningLow(@PartitioningKey Reference<Product> product, int available) {
}
