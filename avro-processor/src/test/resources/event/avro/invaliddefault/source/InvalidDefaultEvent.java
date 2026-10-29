package event.avro;

import be.appify.prefab.core.annotations.Default;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "invalid-default", serialization = Event.Serialization.AVRO)
public record InvalidDefaultEvent(@Default("many") int count) {
}
