package event.avro;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "simple", serialization = Event.Serialization.AVRO)
public record SimpleEvent(
        String name,
        int age,
        double score,
        boolean active
) {
}