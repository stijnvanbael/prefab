package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "nullable-single-valued-record-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/nullablesinglevaluedrecord/source/NullableSingleValuedRecordAvscEvent.avsc")
public interface NullableSingleValuedRecordAvsc {
}

