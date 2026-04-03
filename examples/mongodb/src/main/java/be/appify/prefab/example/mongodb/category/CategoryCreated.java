package be.appify.prefab.example.mongodb.category;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

/** Domain event published when a new category is created. */
@Event(topic = "${topics.category.name}")
public record CategoryCreated(
        @PartitioningKey Reference<Category> reference,
        String name
) {
}
