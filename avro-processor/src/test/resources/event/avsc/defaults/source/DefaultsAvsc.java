package event.avsc.defaults;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "defaults-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/defaults/source/DefaultsAvscEvent.avsc")
public interface DefaultsAvsc {
}

