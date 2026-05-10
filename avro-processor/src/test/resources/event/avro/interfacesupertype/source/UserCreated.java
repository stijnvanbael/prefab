package event.avro;

import be.appify.prefab.core.annotations.Event;

@Event(topic = "users", serialization = Event.Serialization.AVRO)
public record UserCreated(String id, String name) implements UserEvent {
}

