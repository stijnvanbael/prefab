package kafka.avsc;

import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.AvscFile;
import be.appify.prefab.core.annotations.Event;

@Event(topic = "prefab.order", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc(files = @AvscFile(path = "kafka/avsc/OrderCreatedEvent.avsc", keyProperty = "orderId"))
public interface OrderCreated {
}