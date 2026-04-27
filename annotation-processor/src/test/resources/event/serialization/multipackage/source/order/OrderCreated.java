package event.serialization.multipackage.order;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "order-created", serialization = Event.Serialization.JSON)
public record OrderCreated(String orderId) {
}

