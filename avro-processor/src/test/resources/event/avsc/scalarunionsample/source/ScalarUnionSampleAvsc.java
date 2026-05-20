package event.avsc.scalarunionsample;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "scalar-union-sample-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/scalarunionsample/source/ScalarUnionSampleAvscEvent.avsc")
public interface ScalarUnionSampleAvsc {
}

