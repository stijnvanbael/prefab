package kafka.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "prefab.order", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc("kafka/avsc/OrderCreatedEvent.avsc")
public interface OrderCreated {
    @PartitioningKey
    String orderId();
}