package assertion;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "product-created", serialization = Event.Serialization.JSON)
public record ProductCreated(String productId, String name) {
}
