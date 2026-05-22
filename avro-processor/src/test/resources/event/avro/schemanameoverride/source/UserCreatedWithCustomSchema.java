package event.avro;

import be.appify.prefab.core.annotations.AvroSchema;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "users", serialization = Event.Serialization.AVRO)
@AvroSchema(name = "UserCreatedV1")
public record UserCreatedWithCustomSchema(String id, String name) implements OverriddenUserEvent {
}

