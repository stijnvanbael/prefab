package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "non-primitive-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/nonprimitive/source/NonPrimitiveAvscEvent.avsc")
public interface NonPrimitiveAvsc {
}
