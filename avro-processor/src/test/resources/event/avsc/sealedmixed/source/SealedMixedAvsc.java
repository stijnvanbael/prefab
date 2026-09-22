package event.avsc.sealedmixed;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;

@Event(topic = "sealed-mixed-avsc", serialization = Event.Serialization.AVRO)
@Avsc({"event/avsc/sealedmixed/source/SealedMixedAvscEventA.avsc", "event/avsc/sealedmixed/source/SealedMixedAvscEventB.avsc"})
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public sealed interface SealedMixedAvsc extends HasReference
        permits SealedMixedAvscEventA, SealedMixedAvscEventB, SealedMixedManual {
}
