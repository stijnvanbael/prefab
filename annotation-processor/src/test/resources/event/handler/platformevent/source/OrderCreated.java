package event.handler.platformevent;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "orders", serialization = Event.Serialization.JSON)
public record OrderCreated(
        String orderId
) {
}
