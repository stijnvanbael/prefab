package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "nullable-nested-enum-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/nullablenestedenum/source/NullableNestedEnumAvscEvent.avsc")
public interface NullableNestedEnumAvsc {
}

