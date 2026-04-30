package event.avsc.sealed;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "sealed-multi-avsc", serialization = Event.Serialization.AVRO)
@Avsc({"event/avsc/sealed/source/SealedMultiAvscEventA.avsc", "event/avsc/sealed/source/SealedMultiAvscEventB.avsc"})
public sealed interface SealedMultiAvsc permits SealedMultiAvscEventA, SealedMultiAvscEventB {
}
