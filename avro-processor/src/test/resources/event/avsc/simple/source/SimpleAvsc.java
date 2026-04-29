package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "simple-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/simple/source/SimpleAvscEvent.avsc")
public interface SimpleAvsc {
}
