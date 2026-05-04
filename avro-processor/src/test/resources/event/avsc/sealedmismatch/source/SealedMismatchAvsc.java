package event.avsc.sealedmismatch;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "sealed-mismatch-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/sealedmismatch/source/MeteringconfigUpdated.avsc")
public sealed interface SealedMismatchAvsc permits MeteringconfigUpdated {
}

