package event.avsc.nullablemultibranch;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
@Event(topic = "nullable-multi-branch-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/nullablemultibranch/source/NullableMultiBranchAvscEvent.avsc")
public interface NullableMultiBranchAvsc {
}