package com.example.shop.purchasing;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import com.example.shop.catalog.Product;

/**
 * Published when the items of a restock request were delivered.
 *
 * @param product  the restocked product
 * @param quantity the delivered items
 */
@Event(topic = "shop.restock-fulfilled")
public record RestockFulfilled(@PartitioningKey Reference<Product> product, int quantity) {
}
