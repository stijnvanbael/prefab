package event.avsc.dependency;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "dependency-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/dependency/source/DependencyAvscEvent.avsc")
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public interface DependencyAvsc {
}

