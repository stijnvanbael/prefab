package event.avsc.multi;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "multi-avsc", serialization = Event.Serialization.AVRO)
@Avsc({"event/avsc/multi/source/MultiAvscEventA.avsc", "event/avsc/multi/source/MultiAvscEventB.avsc"})
public interface MultiAvsc {
}

