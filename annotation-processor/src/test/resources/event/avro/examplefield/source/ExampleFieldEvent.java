package event.avro;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Example;

@Event(topic = "example-field", serialization = Event.Serialization.AVRO)
public record ExampleFieldEvent(
        @Example("john-doe") String name,
        int age
) {
}

