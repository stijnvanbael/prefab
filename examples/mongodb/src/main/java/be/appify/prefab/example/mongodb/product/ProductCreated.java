package be.appify.prefab.example.mongodb.product;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.mongodb.category.Category;

/** Domain event published when a new product is created. */
@Event(topic = "${topics.product.name}")
public record ProductCreated(
        @PartitioningKey Reference<Product> reference,
        Reference<Category> category
) {
}
