package event.avsc.nullablemultibranch;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "nullable-multi-branch-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/nullablemultibranch/source/NullableMultiBranchAvscEvent.avsc")
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public interface NullableMultiBranchAvsc {
}