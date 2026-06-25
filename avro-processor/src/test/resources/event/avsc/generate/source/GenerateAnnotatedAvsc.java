package event.avsc.generate;

import be.appify.prefab.avro.processor.AvscPlugin;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "generate-annotated-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/generate/source/GenerateAnnotatedAvscEvent.avsc")
@Generate(plugin = AvscPlugin.class, enabled = false)
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public interface GenerateAnnotatedAvsc {
}


