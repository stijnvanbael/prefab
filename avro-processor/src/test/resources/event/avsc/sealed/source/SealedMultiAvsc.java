package event.avsc.sealed;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "sealed-multi-avsc", serialization = Event.Serialization.AVRO)
@Avsc({"event/avsc/sealed/source/SealedMultiAvscEventA.avsc", "event/avsc/sealed/source/SealedMultiAvscEventB.avsc"})
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public sealed interface SealedMultiAvsc permits SealedMultiAvscEventA, SealedMultiAvscEventB {
}
