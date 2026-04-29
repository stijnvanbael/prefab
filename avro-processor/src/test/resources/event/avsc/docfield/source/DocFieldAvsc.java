package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "doc-field-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/docfield/source/DocFieldAvscEvent.avsc")
public interface DocFieldAvsc {
}

