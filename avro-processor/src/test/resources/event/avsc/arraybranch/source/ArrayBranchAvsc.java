package event.avsc.arraybranch;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "array-branch-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/arraybranch/source/ArrayBranchAvscEvent.avsc")
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public interface ArrayBranchAvsc {
}