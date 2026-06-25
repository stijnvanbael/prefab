package event.avsc.lowercase;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "lowercase-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/lowercase/source/lowercaseAvscEvent.avsc")
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public interface LowercaseAvsc {
}