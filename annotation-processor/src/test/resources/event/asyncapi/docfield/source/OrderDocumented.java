package event.asyncapi;

import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "orders", serialization = Event.Serialization.JSON)
public record OrderDocumented(
        @Doc("Unique identifier of the order") String orderId,
        String customerId
) {
}

