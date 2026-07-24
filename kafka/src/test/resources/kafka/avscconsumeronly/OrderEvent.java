package kafka.avscconsumeronly;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "prefab.order.consumer.only", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc("kafka/avscconsumeronly/OrderCreatedEvent.avsc")
public interface OrderEvent {
}

