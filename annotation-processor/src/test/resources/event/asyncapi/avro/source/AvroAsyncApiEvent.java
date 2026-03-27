package event.asyncapi;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "avro-event", serialization = Event.Serialization.AVRO)
public record AvroAsyncApiEvent(
        String name,
        int count
) {
}
