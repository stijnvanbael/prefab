package event.avsc.scalarunion;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "scalar-union-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/scalarunion/source/ScalarUnionAvscEvent.avsc")
public interface ScalarUnionAvsc {
}
