package event.avsc.multi;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "multi-avsc", serialization = Event.Serialization.AVRO)
@Avsc({"event/avsc/multi/source/MultiAvscEventA.avsc", "event/avsc/multi/source/MultiAvscEventB.avsc"})
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public interface MultiAvsc {
}

