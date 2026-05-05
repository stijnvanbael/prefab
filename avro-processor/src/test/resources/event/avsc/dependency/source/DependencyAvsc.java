package event.avsc.dependency;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "dependency-avsc", serialization = Event.Serialization.AVRO)
@Avsc("event/avsc/dependency/source/DependencyAvscEvent.avsc")
public interface DependencyAvsc {
}

