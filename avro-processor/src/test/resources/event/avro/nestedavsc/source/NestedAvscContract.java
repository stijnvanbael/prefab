package event.avro;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "nested-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avro/nestedavsc/source/NestedAvscEvent.avsc")
public interface NestedAvscContract {
}

