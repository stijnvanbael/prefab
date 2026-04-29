package kafka.avscasynccommit;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "orders", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc("kafka/avscasynccommit/OrderPlacedEvent.avsc")
public interface OrderPlaced {
}

