package event.avsc.lowercase;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
@Event(topic = "lowercase-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/lowercase/source/lowercaseAvscEvent.avsc")
public interface LowercaseAvsc {
}