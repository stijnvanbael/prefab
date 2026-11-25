package com.example.shop.catalog;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

/**
 * Published when a product is taken out of the catalog for good.
 *
 * @param product the discontinued product
 */
@Event(topic = "shop.product-discontinued")
public record ProductDiscontinued(@PartitioningKey Reference<Product> product) {
}
