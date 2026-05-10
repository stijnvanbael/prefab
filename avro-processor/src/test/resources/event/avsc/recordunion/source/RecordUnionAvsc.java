package event.avsc.recordunion;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
@Event(topic = "record-union-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/recordunion/source/RecordUnionAvscEvent.avsc")
public interface RecordUnionAvsc {
}