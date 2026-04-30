package event.avro;

import be.appify.prefab.core.annotations.Doc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "doc-field", serialization = Event.Serialization.AVRO)
public record DocFieldEvent(
        @Doc("The full name of the person") String name,
        int age
) {
}

