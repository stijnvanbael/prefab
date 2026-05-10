package event.avsc.arraybranch;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
@Event(topic = "array-branch-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/arraybranch/source/ArrayBranchAvscEvent.avsc")
public interface ArrayBranchAvsc {
}