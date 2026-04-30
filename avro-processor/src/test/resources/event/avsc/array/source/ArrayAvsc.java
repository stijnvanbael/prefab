package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "array-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/array/source/ArrayAvscEvent.avsc")
public interface ArrayAvsc {
}

