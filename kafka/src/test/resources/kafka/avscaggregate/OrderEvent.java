package kafka.avscaggregate;
import be.appify.prefab.core.annotations.Avsc;
import be.appify.prefab.core.annotations.Event;
@Event(topic = "prefab.order", platform = Event.Platform.KAFKA, serialization = Event.Serialization.AVRO)
@Avsc({"kafka/avscaggregate/OrderCreatedEvent.avsc", "kafka/avscaggregate/OrderShippedEvent.avsc"})
public interface OrderEvent {
}
