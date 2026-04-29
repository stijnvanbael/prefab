package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "nullable-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/nullable/source/NullableAvscEvent.avsc")
public interface NullableAvsc {
}
