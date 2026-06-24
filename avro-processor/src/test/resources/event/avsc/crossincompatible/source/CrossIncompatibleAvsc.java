package event.avsc.crossincompatible;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "cross-incompatible", serialization = Event.Serialization.AVRO)
@Avsc({
    "event/avsc/crossincompatible/source/CrossEventA.avsc",
    "event/avsc/crossincompatible/source/CrossEventB.avsc"
})
public interface CrossIncompatibleAvsc {
}

