package event.asyncapi;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "simple-event", serialization = Event.Serialization.JSON)
public record SimpleAsyncApiEvent(
        String name,
        int age,
        boolean active
) {
}
