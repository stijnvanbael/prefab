package event.avsc.scalarunion;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "scalar-union-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/scalarunion/source/ScalarUnionAvscEvent.avsc")
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public interface ScalarUnionAvsc {
}
