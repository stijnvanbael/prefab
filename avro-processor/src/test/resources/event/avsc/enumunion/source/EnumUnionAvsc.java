package event.avsc.enumunion;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
@Event(topic = "enum-union-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/enumunion/source/EnumUnionAvscEvent.avsc")
public interface EnumUnionAvsc {
}