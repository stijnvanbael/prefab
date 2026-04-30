package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

// Intentional collision: interface name matches the AVSC record name.
// Used to verify the annotation processor reports a clear compile error.
@Event(topic = "array-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/array/source/ArrayAvscEvent.avsc")
public interface ArrayAvscEvent {
}
