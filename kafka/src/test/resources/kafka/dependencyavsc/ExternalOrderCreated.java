package kafka.dependencyavsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "prefab.external.order", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc("kafka/dependencyavsc/ExternalOrderCreatedEvent.avsc")
public interface ExternalOrderCreated {
}

