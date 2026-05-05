package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "nullable-array-nested-single-valued-record-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/nullablearraynestedsinglevaluedrecord/source/NullableArrayNestedSingleValuedRecordAvscEvent.avsc")
public interface NullableArrayNestedSingleValuedRecordAvsc {
}

