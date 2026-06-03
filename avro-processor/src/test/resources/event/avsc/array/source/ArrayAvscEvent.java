package event.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

// Intentional collision: interface name matches the AVSC record name.
// Used to verify the annotation processor reports a clear compile error.
@Event(topic = "array-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/array/source/ArrayAvscEvent.avsc")
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public interface ArrayAvscEvent {
}
