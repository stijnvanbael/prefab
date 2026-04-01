package event.asyncapi;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "orders-in", serialization = Event.Serialization.JSON)
public record OrderReceived(
        String orderId,
        String customerId
) {
}
