package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "uuid-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/uuid/source/UuidAvscEvent.avsc")
public interface UuidAvsc {
}

